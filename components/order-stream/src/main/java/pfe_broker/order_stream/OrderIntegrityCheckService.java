package pfe_broker.order_stream;

import static pfe_broker.log.Log.LOG;

import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.ArrayList;
import java.util.List;
import pfe_broker.avro.Order;
import pfe_broker.avro.OrderRejectReason;
import pfe_broker.avro.Side;
import pfe_broker.common.SymbolReader;

@Singleton
public class OrderIntegrityCheckService {

  @Inject
  private SymbolReader symbolReader;

  @Inject
  private StatefulRedisConnection<String, String> redisConnection;

  private List<String> symbols;

  public OrderIntegrityCheckService() {
    this.symbols = new ArrayList<>();
  }

  @PostConstruct
  void init() {
    if (this.symbolReader.isKafkaRunning()) {
      this.retreiveSymbols();
    } else {
      LOG.error("Kafka is not running");
    }
  }

  private boolean verifyUserExistInRedis(String username) {
    return redisConnection.sync().exists(username + ":balance") == 1;
  }

  private OrderRejectReason marketOrderCheckIntegrity(Order order) {
    RedisCommands<String, String> syncCommands = redisConnection.sync();

    String username = order.getUsername().toString();
    String symbol = order.getSymbol().toString();
    Integer quantity = order.getQuantity();
    Side side = order.getSide();

    String stockKey = username + ":" + symbol;

    if (side == Side.BUY) {
      return null;
    }

    syncCommands.watch(stockKey);
    while (true) {
      String stockQuantityString = syncCommands.get(stockKey);
      if (stockQuantityString == null) {
        LOG.debug(
          "Order " +
          order +
          " rejected because of insufficient stocks (entry does not exist)"
        );
        syncCommands.unwatch();
        return OrderRejectReason.ORDER_EXCEEDS_LIMIT;
      }
      Integer stockQuantity = Integer.parseInt(stockQuantityString);
      if (stockQuantity < quantity) {
        LOG.debug(
          "Order " + order + " rejected because of insufficient stocks"
        );
        syncCommands.unwatch();
        return OrderRejectReason.ORDER_EXCEEDS_LIMIT;
      }
      syncCommands.multi();
      syncCommands.decrby(stockKey, quantity);
      try {
        syncCommands.exec();
        syncCommands.unwatch();
        return null;
      } catch (Exception e) {
        LOG.debug("Retrying order " + order);
      }
    }
  }

  public OrderRejectReason checkIntegrity(Order order) {
    LOG.info("Checking integrity of order " + order);

    String username = order.getUsername().toString();
    String symbol = order.getSymbol().toString();
    Integer quantity = order.getQuantity();

    if (username == null || username.isEmpty()) {
      LOG.debug("Order " + order + " rejected because of empty username");
      return OrderRejectReason.BROKER_OPTION;
    }
    if (symbol == null || symbol.isEmpty() || !symbols.contains(symbol)) {
      LOG.debug("Order " + order + " rejected because of unknown symbol");
      return OrderRejectReason.UNKNOWN_SYMBOL;
    }
    if (quantity == null || quantity <= 0) {
      LOG.debug("Order " + order + " rejected because of invalid quantity");
      return OrderRejectReason.ORDER_EXCEEDS_LIMIT;
    }

    if (!verifyUserExistInRedis(username)) {
      LOG.debug("Order " + order + " rejected because of unknown user");
      return OrderRejectReason.BROKER_OPTION;
    }

    OrderRejectReason marketOrderCheckIntegrityResult =
      marketOrderCheckIntegrity(order);

    if (marketOrderCheckIntegrityResult == null) {
      LOG.info(
        "Market order " + order + " accepted at " + java.time.Instant.now()
      );
    }

    return marketOrderCheckIntegrityResult;
  }

  /**
   * Expose this public method to be able to call it from the test
   */
  public void retreiveSymbols() {
    try {
      this.symbols = symbolReader.getSymbols();
    } catch (Exception e) {
      LOG.error("Error while retreiving symbols: " + e.getMessage());
    }
  }

  public boolean isRedisRunning() {
    return redisConnection.sync().ping().equals("PONG");
  }
}
