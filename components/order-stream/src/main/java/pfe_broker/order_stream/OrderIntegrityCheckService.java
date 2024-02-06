package pfe_broker.order_stream;

import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.Timer;
import io.micronaut.context.annotation.Property;
import jakarta.annotation.PreDestroy;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pfe_broker.avro.Order;
import pfe_broker.avro.OrderRejectReason;
import pfe_broker.avro.Side;
import pfe_broker.avro.Type;
import pfe_broker.common.SymbolReader;
import pfe_broker.common.UtilsRunning;

@Singleton
public class OrderIntegrityCheckService {

  private static final Logger LOG = LoggerFactory.getLogger(
    OrderIntegrityCheckService.class
  );

  private static final String BALANCE_KEY_PATTERN = "%s:balance";
  private static final String STOCK_KEY_PATTERN = "%s:%s";

  private final SymbolReader symbolReader;

  private final StatefulRedisConnection<String, String> redisConnection;

  private final MeterRegistry meterRegistry;

  public OrderIntegrityCheckService(
    @Property(name = "redis.uri") String redisUri,
    SymbolReader symbolReader,
    RedisClient redisClient,
    MeterRegistry meterRegistry
  ) {
    this.symbolReader = symbolReader;
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

  private boolean verifyUserExistInRedis(String username) {
    return redisConnection.sync().exists(buildBalanceKey(username)) == 1;
  }

  /**
   * Check the integrity of a sell order (market or limit):
   * @param order the order to check
   * @return null if the order is valid, the reason why it is not valid otherwise
   */
  private OrderRejectReason sellVerification(
    Order order,
    RedisCommands<String, String> syncCommands
  ) {
    String stockKey = buildStockKey(
      order.getUsername().toString(),
      order.getSymbol().toString()
    );
    Integer quantity = order.getQuantity();

    int countdown = 10;
    while (countdown-- > 0) {
      syncCommands.watch(stockKey);
      String stockQuantityString = syncCommands.get(stockKey);
      if (stockQuantityString == null || stockQuantityString.isEmpty()) {
        LOG.debug(
          "Order {} rejected because of insufficient stocks (entry does not exist)",
          order
        );
        return OrderRejectReason.INCORRECT_QUANTITY;
      }
      Integer stockQuantity = Integer.parseInt(stockQuantityString);
      if (stockQuantity < quantity) {
        LOG.debug("Order {} rejected because of insufficient stocks", order);
        return OrderRejectReason.INCORRECT_QUANTITY;
      }
      syncCommands.multi();
      syncCommands.decrby(stockKey, quantity);
      if (syncCommands.exec().size() == 1) {
        return null;
      }
      LOG.debug("Retrying order {}", order);
    }
    LOG.debug("Order {} rejected because of insufficient stocks", order);
    return OrderRejectReason.INCORRECT_QUANTITY;
  }

  /**
   * Check the integrity of a buy limit order:
   * @param order the order to check
   * @return null if the order is valid, the reason why it is not valid otherwise
   */
  private OrderRejectReason buyLimitVerification(
    Order order,
    RedisCommands<String, String> syncCommands
  ) {
    String balanceKey = buildBalanceKey(order.getUsername().toString());
    Double orderTotalPrice = order.getQuantity() * order.getPrice();

    int countdown = 10;
    while (countdown-- > 0) {
      syncCommands.watch(balanceKey);
      Double balance = Double.parseDouble(syncCommands.get(balanceKey));
      if (balance < orderTotalPrice) {
        LOG.debug("Order {} rejected because of insufficient balance", order);
        return OrderRejectReason.INCORRECT_QUANTITY;
      }
      syncCommands.multi();
      syncCommands.incrbyfloat(balanceKey, -orderTotalPrice);
      if (syncCommands.exec().size() == 1) {
        return null;
      }
      LOG.debug("Retrying order {}", order);
    }
    LOG.debug("Order {} rejected because of insufficient balance", order);
    return OrderRejectReason.INCORRECT_QUANTITY;
  }

  /**
   * Check the integrity of a market order:
   * @param order the order to check
   * @return null if the order is valid, the reason why it is not valid otherwise
   */
  private OrderRejectReason marketOrderCheckIntegrity(Order order) {
    RedisCommands<String, String> syncCommands = redisConnection.sync();

    Side side = order.getSide();

    // No need to check for BUY market orders
    if (side == Side.BUY) {
      return null;
    }

    return sellVerification(order, syncCommands);
  }

  /**
   * Check the integrity of a limit order:
   * @param order the order to check
   * @return null if the order is valid, the reason why it is not valid otherwise
   */
  private OrderRejectReason limitOrderCheckIntegrity(Order order) {
    RedisCommands<String, String> syncCommands = redisConnection.sync();

    Side side = order.getSide();

    if (side == Side.BUY) {
      return buyLimitVerification(order, syncCommands);
    }

    return sellVerification(order, syncCommands);
  }

  /**
   * Check the integrity of an order:
   *
   * What needs to be done with redis:
   *
   * Market order:
   * - BUY: nothing
   * - SELL: check if the user has enough stocks / decrement the stock
   *
   * Limit order:
   * - BUY: check if the user has enough balance / decrement the balance
   * - SELL: check if the user has enough stocks / decrement the stock
   *
   */
  private OrderRejectReason checkIntegrityWrapped(Order order) {
    LOG.debug("Checking integrity of order {}", order);

    String username = order.getUsername().toString();
    String symbol = order.getSymbol().toString();
    Integer quantity = order.getQuantity();
    Type type = order.getType();

    if (username == null || username.isEmpty()) {
      LOG.debug("Order {} rejected because of empty username", order);
      return OrderRejectReason.UNKNOWN_ACCOUNT;
    }
    if (
      symbol == null ||
      symbol.isEmpty() ||
      (!symbolReader.getSymbolsCached().contains(symbol) &&
        !symbolReader.retrieveSymbols().contains(symbol))
    ) {
      LOG.debug("Order {} rejected because of unknown symbol", order);
      return OrderRejectReason.UNKNOWN_SYMBOL;
    }
    if (quantity == null || quantity <= 0) {
      LOG.debug("Order {} rejected because of invalid quantity", order);
      return OrderRejectReason.INCORRECT_QUANTITY;
    }

    if (!verifyUserExistInRedis(username)) {
      LOG.debug("Order {} rejected because of unknown user", order);
      return OrderRejectReason.UNKNOWN_ACCOUNT;
    }

    OrderRejectReason orderCheckIntegrityResult = OrderRejectReason.OTHER;
    if (type == Type.MARKET) {
      orderCheckIntegrityResult = marketOrderCheckIntegrity(order);
    } else if (type == Type.LIMIT) {
      orderCheckIntegrityResult = limitOrderCheckIntegrity(order);
    }

    if (orderCheckIntegrityResult == null) {
      LOG.debug("Order {} accepted", order);
    }

    return orderCheckIntegrityResult;
  }

  public OrderRejectReason checkIntegrity(Order order) {
    Tags tags = Tags.of(
      Tag.of("type", order.getType().toString()),
      Tag.of("side", order.getSide().toString()),
      Tag.of("symbol", order.getSymbol().toString())
    );
    Timer timer = meterRegistry.timer("order_stream_check_integrity", tags);
    Timer.Sample sample = Timer.start();

    OrderRejectReason orderCheckIntegrityResult = checkIntegrityWrapped(order);

    if (orderCheckIntegrityResult != null) {
      Tags tagsWithReason = Tags
        .of(tags)
        .and(Tag.of("orderRejectReason", orderCheckIntegrityResult.toString()));
      meterRegistry
        .counter("order_stream_rejected_order", tagsWithReason)
        .increment();
    } else {
      meterRegistry.counter("order_stream_accepted_order", tags).increment();
    }
    sample.stop(timer);

    return orderCheckIntegrityResult;
  }
}
