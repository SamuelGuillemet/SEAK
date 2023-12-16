package pfe_broker.trade_stream;

import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import io.micronaut.context.annotation.Property;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pfe_broker.avro.OrderRejectReason;
import pfe_broker.avro.Side;
import pfe_broker.avro.Trade;
import pfe_broker.avro.Type;
import pfe_broker.common.UtilsRunning;

public class TradeIntegrityCheckService {

  private static final Logger LOG = LoggerFactory.getLogger(
    TradeIntegrityCheckService.class
  );

  @Inject
  private RedisClient redisClient;

  @Property(name = "redis.uri")
  private String redisUri;

  private StatefulRedisConnection<String, String> redisConnection;

  @PostConstruct
  void init() {
    if (this.isRedisRunning()) {
      this.redisConnection = redisClient.connect();
    } else {
      LOG.error("Redis is not running");
    }
  }

  private OrderRejectReason marketOrderCheckIntegrity(Trade trade) {
    RedisCommands<String, String> syncCommands = redisConnection.sync();

    String username = trade.getOrder().getUsername().toString();
    String symbol = trade.getSymbol().toString();
    Integer quantity = trade.getQuantity();
    Double price = trade.getPrice();
    Side side = trade.getOrder().getSide();
    Double amount = price * quantity;

    String stockKey = username + ":" + symbol;
    String balanceKey = username + ":balance";

    if (side == Side.SELL) {
      syncCommands.incrbyfloat(balanceKey, amount);
      return null;
    }

    syncCommands.watch(balanceKey);
    int countdown = 10;
    while (countdown-- > 0) {
      Double balance = Double.parseDouble(syncCommands.get(balanceKey));
      if (balance < amount) {
        syncCommands.unwatch();
        LOG.debug(
          "Order {} rejected because of insufficient funds",
          trade.getOrder()
        );
        return OrderRejectReason.INCORRECT_QUANTITY;
      }
      syncCommands.multi();
      syncCommands.incrbyfloat(balanceKey, -amount);
      syncCommands.incrby(stockKey, quantity);

      try {
        syncCommands.exec();
        return null;
      } catch (Exception e) {
        LOG.debug("Retrying trade {}", trade);
      }
    }
    LOG.debug(
      "Order {} rejected because of insufficient funds",
      trade.getOrder()
    );
    syncCommands.unwatch();
    return OrderRejectReason.INCORRECT_QUANTITY;
  }

  public OrderRejectReason checkIntegrity(Trade trade) {
    LOG.debug("Checking integrity of trade {}", trade);
    Type type = trade.getOrder().getType();

    OrderRejectReason tradeCheckIntegrityResult = OrderRejectReason.OTHER;
    if (type == Type.MARKET) {
      tradeCheckIntegrityResult = marketOrderCheckIntegrity(trade);
    } else if (type == Type.LIMIT) {
      LOG.warn("Limit order not implemented yet");
    }

    if (tradeCheckIntegrityResult == null) {
      LOG.debug("Trade {} accepted", trade);
    }

    return tradeCheckIntegrityResult;
  }

  private boolean isRedisRunning() {
    return UtilsRunning.isRedisRunning(redisUri);
  }
}
