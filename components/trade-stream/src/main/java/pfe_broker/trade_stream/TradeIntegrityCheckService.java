package pfe_broker.trade_stream;

import static pfe_broker.log.Log.LOG;

import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import io.micronaut.context.annotation.Property;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import pfe_broker.avro.OrderRejectReason;
import pfe_broker.avro.Side;
import pfe_broker.avro.Trade;
import pfe_broker.common.UtilsRunning;

public class TradeIntegrityCheckService {

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
      LOG.fatal("Redis is not running");
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
          "Order " +
          trade.getOrder() +
          " rejected because of insufficient funds"
        );
        return OrderRejectReason.ORDER_EXCEEDS_LIMIT;
      }
      syncCommands.multi();
      syncCommands.incrbyfloat(balanceKey, -amount);
      syncCommands.incrby(stockKey, quantity);

      try {
        syncCommands.exec();
        return null;
      } catch (Exception e) {
        LOG.debug("Retrying trade " + trade);
      }
    }
    LOG.debug(
      "Order " + trade.getOrder() + " rejected because of insufficient funds"
    );
    syncCommands.unwatch();
    return OrderRejectReason.ORDER_EXCEEDS_LIMIT;
  }

  public OrderRejectReason checkIntegrity(Trade trade) {
    LOG.info("Checking integrity of trade " + trade);

    OrderRejectReason marketOrderCheckIntegrityResult =
      marketOrderCheckIntegrity(trade);

    if (marketOrderCheckIntegrityResult == null) {
      LOG.info("Trade " + trade + " accepted at " + java.time.Instant.now());
    }

    return marketOrderCheckIntegrityResult;
  }

  private boolean isRedisRunning() {
    return UtilsRunning.isRedisRunning(redisUri);
  }
}
