package pfe_broker.order_book;

import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import io.micronaut.context.annotation.Property;
import jakarta.annotation.PreDestroy;
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

  private static final String BALANCE_KEY_PATTERN = "%s:balance";
  private static final String STOCK_KEY_PATTERN = "%s:%s";

  private final StatefulRedisConnection<String, String> redisConnection;

  public IntegrityCheckService(
    RedisClient redisClient,
    @Property(name = "redis.uri") String redisUri
  ) {
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

  public boolean replaceCancelOrder(Order oldOrder, Order newOrder) {
    RedisCommands<String, String> syncCommands = redisConnection.sync();

    if (newOrder.getPrice() < 0 || newOrder.getQuantity() < 0) {
      return false;
    }

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
    String balanceKey = buildBalanceKey(oldOrder.getUsername().toString());
    Double modification =
      oldOrder.getPrice() * oldOrder.getQuantity() -
      newOrder.getPrice() * newOrder.getQuantity();

    int countdown = 10;
    while (countdown-- > 0) {
      syncCommands.watch(balanceKey);
      Double balance = Double.parseDouble(syncCommands.get(balanceKey));
      if (balance + modification < 0) {
        return false;
      }
      syncCommands.multi();
      syncCommands.incrbyfloat(balanceKey, modification);
      if (syncCommands.exec().size() != 0) {
        return true;
      }
      LOG.debug("Retrying...");
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
    String stockKey = buildStockKey(
      oldOrder.getUsername().toString(),
      oldOrder.getSymbol().toString()
    );
    Integer modification = oldOrder.getQuantity() - newOrder.getQuantity();

    int countdown = 10;
    while (countdown-- > 0) {
      syncCommands.watch(stockKey);
      Integer stockQuantity = Integer.parseInt(syncCommands.get(stockKey));
      if (stockQuantity + modification < 0) {
        return false;
      }
      syncCommands.multi();
      syncCommands.incrby(stockKey, modification);
      if (syncCommands.exec().size() != 0) {
        return true;
      }
      LOG.debug("Retrying...");
    }
    LOG.error("Failed to modify the stock");
    return false;
  }
}
