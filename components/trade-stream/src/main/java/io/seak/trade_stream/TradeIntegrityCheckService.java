package io.seak.trade_stream;

import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.Timer;
import io.micronaut.context.annotation.Property;
import io.seak.avro.Order;
import io.seak.avro.OrderRejectReason;
import io.seak.avro.Side;
import io.seak.avro.Trade;
import io.seak.avro.Type;
import io.seak.common.UtilsRunning;
import jakarta.annotation.PreDestroy;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class TradeIntegrityCheckService {

  private static final Logger LOG = LoggerFactory.getLogger(
    TradeIntegrityCheckService.class
  );

  private static final String BALANCE_KEY_PATTERN = "%s:balance";
  private static final String STOCK_KEY_PATTERN = "%s:%s";

  private final StatefulRedisConnection<String, String> redisConnection;

  private final MeterRegistry meterRegistry;

  public TradeIntegrityCheckService(
    @Property(name = "redis.uri") String redisUri,
    RedisClient redisClient,
    MeterRegistry meterRegistry
  ) {
    this.meterRegistry = meterRegistry;

    if (UtilsRunning.isRedisRunning(redisUri)) {
      this.redisConnection = redisClient.connect();
    } else {
      this.redisConnection = null;
      LOG.error("Redis is not running");
    }
  }

  @PreDestroy
  void close() {
    if (redisConnection != null) {
      redisConnection.close();
    }
  }

  private String buildBalanceKey(String username) {
    return String.format(BALANCE_KEY_PATTERN, username);
  }

  private String buildStockKey(String username, String symbol) {
    return String.format(STOCK_KEY_PATTERN, username, symbol);
  }

  /**
   * Check the integrity of a sell trade (limit or market)
   * @param trade The trade to check
   * @return null if the trade is accepted, the reason of rejection otherwise
   */
  private OrderRejectReason sellVerification(
    Trade trade,
    RedisCommands<String, String> syncCommands
  ) {
    String balanceKey = buildBalanceKey(
      trade.getOrder().getUsername().toString()
    );
    Double amount = trade.getPrice() * trade.getQuantity();

    syncCommands.incrbyfloat(balanceKey, amount);
    return null;
  }

  /**
   * Check the integrity of a buy limit trade
   * @param trade The trade to check
   * @return null if the trade is accepted, the reason of rejection otherwise
   */
  private OrderRejectReason buyLimitVerification(
    Trade trade,
    RedisCommands<String, String> syncCommands
  ) {
    Integer quantity = trade.getQuantity();

    String stockKey = buildStockKey(
      trade.getOrder().getUsername().toString(),
      trade.getSymbol().toString()
    );

    syncCommands.incrby(stockKey, quantity);
    return null;
  }

  /**
   * Check the integrity of a buy market trade
   * @param trade The trade to check
   * @return null if the trade is accepted, the reason of rejection otherwise
   */
  private OrderRejectReason buyMarketVerification(
    Trade trade,
    RedisCommands<String, String> syncCommands
  ) {
    Order order = trade.getOrder();
    String stockKey = buildStockKey(
      order.getUsername().toString(),
      trade.getSymbol().toString()
    );
    String balanceKey = buildBalanceKey(order.getUsername().toString());
    Integer quantity = trade.getQuantity();
    Double amount = trade.getPrice() * trade.getQuantity();

    int countdown = 10;
    while (countdown-- > 0) {
      syncCommands.watch(balanceKey);
      Double balance = Double.parseDouble(syncCommands.get(balanceKey));
      if (balance < amount) {
        LOG.debug("Order {} rejected because of insufficient funds", order);
        return OrderRejectReason.INCORRECT_QUANTITY;
      }
      syncCommands.multi();
      syncCommands.incrbyfloat(balanceKey, -amount);
      syncCommands.incrby(stockKey, quantity);
      if (syncCommands.exec().size() == 2) {
        return null;
      }
      LOG.debug("Retrying trade {}", trade);
    }
    LOG.debug("Order {} rejected because of insufficient funds", order);
    return OrderRejectReason.INCORRECT_QUANTITY;
  }

  /**
   * Check the integrity of a market order
   * @param trade The trade to check
   * @return null if the trade is accepted, the reason of rejection otherwise
   */
  private OrderRejectReason marketOrderCheckIntegrity(Trade trade) {
    RedisCommands<String, String> syncCommands = redisConnection.sync();
    Side side = trade.getOrder().getSide();

    if (side == Side.SELL) {
      return sellVerification(trade, syncCommands);
    }

    return buyMarketVerification(trade, syncCommands);
  }

  /**
   * Check the integrity of a limit order
   * @param trade The trade to check
   * @return null if the trade is accepted, the reason of rejection otherwise
   */
  private OrderRejectReason limitOrderCheckIntegrity(Trade trade) {
    RedisCommands<String, String> syncCommands = redisConnection.sync();
    Side side = trade.getOrder().getSide();

    if (side == Side.SELL) {
      return sellVerification(trade, syncCommands);
    }

    return buyLimitVerification(trade, syncCommands);
  }

  /**
   * Check the integrity of an order
   *
   * What needs to be done with redis:
   *
   * Market order:
   * - BUY: check if the user has enough funds / increment the stock / decrement the balance
   * - SELL: increment the balance
   *
   * Limit order:
   * - BUY: increment the stock
   * - SELL: increment the balance
   */
  private OrderRejectReason checkIntegrityWrapped(Trade trade) {
    LOG.debug("Checking integrity of trade {}", trade);
    Type type = trade.getOrder().getType();

    OrderRejectReason tradeCheckIntegrityResult = OrderRejectReason.OTHER;
    if (type == Type.MARKET) {
      tradeCheckIntegrityResult = marketOrderCheckIntegrity(trade);
    } else if (type == Type.LIMIT) {
      tradeCheckIntegrityResult = limitOrderCheckIntegrity(trade);
    }

    if (tradeCheckIntegrityResult == null) {
      LOG.debug("Trade {} accepted", trade);
    }

    return tradeCheckIntegrityResult;
  }

  public OrderRejectReason checkIntegrity(Trade trade) {
    Tags tags = Tags.of(
      Tag.of("type", trade.getOrder().getType().toString()),
      Tag.of("side", trade.getOrder().getSide().toString()),
      Tag.of("symbol", trade.getSymbol().toString())
    );
    Timer timer = meterRegistry.timer("trade_stream_check_integrity", tags);
    Timer.Sample sample = Timer.start();

    OrderRejectReason orderCheckIntegrityResult = checkIntegrityWrapped(trade);

    if (orderCheckIntegrityResult != null) {
      Tags tagsWithReason = Tags
        .of(tags)
        .and(Tag.of("orderRejectReason", orderCheckIntegrityResult.toString()));
      meterRegistry
        .counter("trade_stream_rejected_trade", tagsWithReason)
        .increment();
    } else {
      meterRegistry.counter("trade_stream_accepted_trade", tags).increment();
    }
    sample.stop(timer);

    return orderCheckIntegrityResult;
  }
}
