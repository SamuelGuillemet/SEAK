package pfe_broker.order_stream;

import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import io.micronaut.context.annotation.Property;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.ArrayList;
import java.util.List;
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

  @Inject
  private SymbolReader symbolReader;

  @Inject
  private RedisClient redisClient;

  @Property(name = "redis.uri")
  private String redisUri;

  private StatefulRedisConnection<String, String> redisConnection;

  private List<String> symbols = new ArrayList<>();

  @PostConstruct
  void init() {
    if (this.symbolReader.isKafkaRunning()) {
      this.retreiveSymbols();
    } else {
      LOG.error("Kafka is not running");
    }
    if (this.isRedisRunning()) {
      this.redisConnection = redisClient.connect();
    } else {
      LOG.error("Redis is not running");
    }
  }

  private boolean verifyUserExistInRedis(String username) {
    Boolean userExists =
      redisConnection.sync().exists(username + ":balance") == 1;
    if (userExists) {
      return true;
    } else {
      // Create the user in redis
      redisConnection.sync().set(username + ":balance", "10000");
    }
    return redisConnection.sync().exists(username + ":balance") == 1;
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
    String username = order.getUsername().toString();
    String symbol = order.getSymbol().toString();
    Integer quantity = order.getQuantity();

    String stockKey = username + ":" + symbol;

    syncCommands.watch(stockKey);
    int countdown = 10;
    while (countdown-- > 0) {
      String stockQuantityString = syncCommands.get(stockKey);
      if (stockQuantityString == null || stockQuantityString.isEmpty()) {
        LOG.debug(
          "Order {} rejected because of insufficient stocks (entry does not exist)",
          order
        );
        syncCommands.unwatch();
        return OrderRejectReason.INCORRECT_QUANTITY;
      }
      Integer stockQuantity = Integer.parseInt(stockQuantityString);
      if (stockQuantity < quantity) {
        LOG.debug("Order {} rejected because of insufficient stocks", order);
        syncCommands.unwatch();
        return OrderRejectReason.INCORRECT_QUANTITY;
      }
      syncCommands.multi();
      syncCommands.decrby(stockKey, quantity);
      try {
        syncCommands.exec();
        syncCommands.unwatch();
        return null;
      } catch (Exception e) {
        LOG.debug("Retrying order {}", order);
      }
    }
    LOG.debug("Order {} rejected because of insufficient stocks", order);
    syncCommands.unwatch();
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
    String username = order.getUsername().toString();
    Integer quantity = order.getQuantity();
    Double price = order.getPrice();

    Double orderTotalPrice = quantity * price;

    String balanceKey = username + ":balance";

    syncCommands.watch(balanceKey);
    int countdown = 10;
    while (countdown-- > 0) {
      Double balance = Double.parseDouble(syncCommands.get(balanceKey));
      if (balance < orderTotalPrice) {
        LOG.debug("Order {} rejected because of insufficient balance", order);
        syncCommands.unwatch();
        return OrderRejectReason.INCORRECT_QUANTITY;
      }
      syncCommands.multi();
      syncCommands.incrbyfloat(balanceKey, -orderTotalPrice);
      try {
        syncCommands.exec();
        syncCommands.unwatch();
        return null;
      } catch (Exception e) {
        LOG.debug("Retrying order {}", order);
      }
    }
    return null;
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
   * What needs to be checked with redis:
   *
   * Market order:
   * - BUY: nothing
   * - SELL: check if the user has enough stocks
   *
   * Limit order:
   * - BUY: check if the user has enough balance
   * - SELL: check if the user has enough stocks
   *
   */
  public OrderRejectReason checkIntegrity(Order order) {
    LOG.debug("Checking integrity of order {}", order);

    String username = order.getUsername().toString();
    String symbol = order.getSymbol().toString();
    Integer quantity = order.getQuantity();
    Type type = order.getType();

    if (username == null || username.isEmpty()) {
      LOG.debug("Order {} rejected because of empty username", order);
      return OrderRejectReason.UNKNOWN_ACCOUNT;
    }
    if (symbol == null || symbol.isEmpty() || !symbols.contains(symbol)) {
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

  /**
   * Expose this public method to be able to call it from the test
   */
  public void retreiveSymbols() {
    try {
      this.symbols = symbolReader.getSymbols();
    } catch (Exception e) {
      LOG.error("Error while retreiving symbols: {}", e.getMessage());
    }
  }

  public boolean isRedisRunning() {
    return UtilsRunning.isRedisRunning(redisUri);
  }
}
