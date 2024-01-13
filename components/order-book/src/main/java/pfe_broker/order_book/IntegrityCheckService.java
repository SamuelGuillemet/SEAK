package pfe_broker.order_book;

import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import io.micronaut.context.annotation.Property;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pfe_broker.avro.Order;
import pfe_broker.avro.Side;
import pfe_broker.common.UtilsRunning;

@Singleton
public class IntegrityCheckService {

  private static final Logger LOG = LoggerFactory.getLogger(
    IntegrityCheckService.class
  );

  @Inject
  private RedisClient redisClient;

  @Property(name = "redis.uri")
  private String redisUri;

  private StatefulRedisConnection<String, String> redisConnection;

  @PostConstruct
  void init() {
    if (UtilsRunning.isRedisRunning(redisUri)) {
      this.redisConnection = redisClient.connect();
    } else {
      LOG.error("Redis is not running");
    }
  }

  public boolean replaceCancelOrder(Order oldOrder, Order newOrder) {
    RedisCommands<String, String> syncCommands = redisConnection.sync();

    if (oldOrder.getSide() == Side.BUY) {
      return replaceBuyLimitOrder(syncCommands, oldOrder, newOrder);
    } else {
      return replaceSellLimitOrder(syncCommands, oldOrder, newOrder);
    }
  }

  /**
   * Replace a buy limit order
   */
  private boolean replaceBuyLimitOrder(
    RedisCommands<String, String> syncCommands,
    Order oldOrder,
    Order newOrder
  ) {
    String username = oldOrder.getUsername().toString();

    String balanceKey = username + ":balance";

    // Side is buy we need to modify the balance
    Double oldTotalPrice = oldOrder.getPrice() * oldOrder.getQuantity();
    Double newTotalPrice = newOrder.getPrice() * newOrder.getQuantity();
    Double modification = oldTotalPrice - newTotalPrice;

    syncCommands.watch(balanceKey);
    int countdown = 10;
    while (countdown-- > 0) {
      Double balance = Double.parseDouble(syncCommands.get(balanceKey));
      if (balance + modification < 0) {
        syncCommands.unwatch();
        return false;
      }
      syncCommands.multi();
      syncCommands.incrbyfloat(balanceKey, modification);
      try {
        syncCommands.exec();
        syncCommands.unwatch();
        return true;
      } catch (Exception e) {
        LOG.debug("Retrying...");
      }
    }
    LOG.error("Failed to modify the balance");
    return false;
  }

  /**
   * Replace a sell limit order
   */
  private boolean replaceSellLimitOrder(
    RedisCommands<String, String> syncCommands,
    Order oldOrder,
    Order newOrder
  ) {
    String username = oldOrder.getUsername().toString();
    String symbol = oldOrder.getSymbol().toString();

    String stockKey = username + ":" + symbol;

    // Side is sell we need to modify the stock
    Integer oldQuantity = oldOrder.getQuantity();
    Integer newQuantity = newOrder.getQuantity();
    Integer modification = oldQuantity - newQuantity;

    syncCommands.watch(stockKey);
    int countdown = 10;
    while (countdown-- > 0) {
      Integer stockQuantity = Integer.parseInt(syncCommands.get(stockKey));
      if (stockQuantity + modification < 0) {
        syncCommands.unwatch();
        return false;
      }
      syncCommands.multi();
      syncCommands.incrby(stockKey, modification);
      try {
        syncCommands.exec();
        syncCommands.unwatch();
        return true;
      } catch (Exception e) {
        LOG.debug("Retrying...");
      }
    }
    LOG.error("Failed to modify the stock");
    return false;
  }
}
