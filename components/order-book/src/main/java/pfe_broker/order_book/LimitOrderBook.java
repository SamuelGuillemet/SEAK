package pfe_broker.order_book;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Timer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pfe_broker.avro.MarketData;
import pfe_broker.avro.Order;
import pfe_broker.avro.Side;
import pfe_broker.avro.Trade;

public class LimitOrderBook {

  private static final Logger LOG = LoggerFactory.getLogger(
    LimitOrderBook.class
  );

  private final String symbol;

  private final OrderTree buyOrderTree;

  private final OrderTree sellOrderTree;

  private final Timer matchOrdersTimer;

  public LimitOrderBook(String symbol, MeterRegistry meterRegistry) {
    this.symbol = symbol;
    this.buyOrderTree = new OrderTree(Side.BUY);
    this.sellOrderTree = new OrderTree(Side.SELL);

    Tag symbolTag = Tag.of("symbol", symbol);

    meterRegistry.gauge(
      "order_book_volume_order_book",
      List.of(symbolTag, Tag.of("side", Side.BUY.toString())),
      buyOrderTree,
      OrderTree::getTotalVolume
    );

    meterRegistry.gauge(
      "order_book_volume_order_book",
      List.of(symbolTag, Tag.of("side", Side.SELL.toString())),
      sellOrderTree,
      OrderTree::getTotalVolume
    );

    this.matchOrdersTimer =
      meterRegistry.timer("order_book_match_orders", List.of(symbolTag));
  }

  public void addOrder(String id, Order order) {
    LOG.debug("Add order [{}]{} to order book {}", id, order, symbol);
    if (order.getSide() == Side.BUY) {
      buyOrderTree.addOrder(id, order);
    } else {
      sellOrderTree.addOrder(id, order);
    }
  }

  public Order removeOrder(String id) {
    LOG.debug("Remove order [{}] from order book {}", id, symbol);
    Order order = null;
    if (buyOrderTree.contains(id)) {
      order = buyOrderTree.removeOrder(id);
    } else if (sellOrderTree.contains(id)) {
      order = sellOrderTree.removeOrder(id);
    }
    return order;
  }

  public Order replaceOrder(String id, Order order) {
    LOG.debug("Replace order [{}]{} in order book {}", id, order, symbol);
    Order oldOrder = null;
    if (buyOrderTree.contains(id)) {
      oldOrder = buyOrderTree.replaceOrder(id, order);
    } else if (sellOrderTree.contains(id)) {
      oldOrder = sellOrderTree.replaceOrder(id, order);
    }
    return oldOrder;
  }

  public Order getOrder(String id) {
    Order order = null;
    if (buyOrderTree.contains(id)) {
      order = buyOrderTree.getOrder(id);
    } else if (sellOrderTree.contains(id)) {
      order = sellOrderTree.getOrder(id);
    }
    return order;
  }

  public Map<String, Trade> matchOrdersToTrade(MarketData marketData) {
    Timer.Sample sample = Timer.start();
    LOG.trace(
      "Match orders to trade in order book {} with market data {}",
      symbol,
      marketData
    );
    Double low = marketData.getLow();
    Double high = marketData.getHigh();

    Map<String, Order> matchedOrders = new HashMap<>();
    matchedOrders.putAll(buyOrderTree.matchOrders(low));
    matchedOrders.putAll(sellOrderTree.matchOrders(high));

    Map<String, Trade> trades = new HashMap<>();
    matchedOrders.forEach((id, order) -> {
      Trade trade = new Trade(
        order,
        symbol,
        order.getPrice(),
        order.getQuantity()
      );
      trades.put(id, trade);
    });

    sample.stop(matchOrdersTimer);
    return trades;
  }

  public Map<String, Order> getBuyOrders() {
    return buyOrderTree.getOrders();
  }

  public Map<String, Order> getSellOrders() {
    return sellOrderTree.getOrders();
  }

  public String getSymbol() {
    return symbol;
  }

  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("LimitOrderBook: ").append(symbol).append("\n");
    sb.append("Buy Orders: ").append("\n");
    sb.append(buyOrderTree.toString()).append("\n");
    sb.append("Sell Orders: ").append("\n");
    sb.append(sellOrderTree.toString()).append("\n");
    return sb.toString();
  }
}
