package pfe_broker.order_book;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pfe_broker.avro.Order;
import pfe_broker.avro.Side;

public class OrderTree {

  private static final Logger LOG = LoggerFactory.getLogger(OrderTree.class);

  // All order of the side, map (id, price)
  private final Map<String, Double> orders;

  // Map of price and orderList
  private final SortedMap<Double, OrderList> priceMap;

  // Side of the tree
  private final Side side;

  public OrderTree(final Side side) {
    this.orders = Collections.synchronizedMap(new HashMap<>());
    this.priceMap = Collections.synchronizedSortedMap(new TreeMap<>());
    this.side = side;
  }

  public Order addOrder(String id, Order order) {
    LOG.trace("Add order [{}]{} to order tree {}", id, order, this.side);
    Double price = order.getPrice();
    if (priceMap.containsKey(price)) {
      OrderList orderList = priceMap.get(price);
      orderList.addOrder(id, order);
    } else {
      OrderList orderList = new OrderList(price);
      orderList.addOrder(id, order);
      priceMap.put(price, orderList);
    }
    orders.put(id, price);
    return order;
  }

  public Order removeOrder(String id) {
    LOG.trace("Remove order [{}] from order tree {}", id, this.side);
    Double price = orders.get(id);
    if (price == null) {
      return null;
    }
    OrderList orderList = priceMap.get(price);
    Order order = orderList.removeOrder(id);
    if (orderList.getVolume() == 0) {
      priceMap.remove(price);
    }
    orders.remove(id);
    return order;
  }

  public Order replaceOrder(String id, Order order) {
    LOG.trace("Replace order [{}]{} in order tree {}", id, order, this.side);
    Double oldPrice = orders.get(id);
    if (oldPrice == null) {
      return null;
    }

    Double newPrice = order.getPrice();

    if (oldPrice.equals(newPrice)) {
      OrderList orderList = priceMap.get(oldPrice);
      orderList.replaceOrder(id, order);
    } else {
      OrderList oldOrderList = priceMap.get(oldPrice);
      OrderList newOrderList = priceMap.get(newPrice);

      oldOrderList.removeOrder(id);
      newOrderList.addOrder(id, order);

      if (oldOrderList.getVolume() == 0) {
        priceMap.remove(oldPrice);
      }
    }
    orders.put(id, newPrice);
    return order;
  }

  /**
   * Match orders with market data price
   * @param price
   * @return Map of order id and order
   */
  public Map<String, Order> matchOrders(Double price) {
    LOG.trace("Match orders in order tree {} with price {}", this.side, price);
    Map<String, Order> ordersMap = null;
    if (side == Side.BUY) {
      ordersMap = matchBuyOrders(price);
    } else {
      ordersMap = matchSellOrders(price);
    }
    ordersMap.forEach((id, order) -> removeOrder(id));
    return ordersMap;
  }

  /**
   * Match all order which are above a certain price
   * @param price
   * @return
   */
  private Map<String, Order> matchBuyOrders(Double price) {
    return priceMap
      .tailMap(price)
      .values()
      .stream()
      .map(OrderList::getOrders)
      .flatMap(map -> map.entrySet().stream())
      .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
  }

  /**
   * Match all order which are bellow a certain price
   * @param price
   * @return
   */
  private Map<String, Order> matchSellOrders(Double price) {
    return priceMap
      .headMap(price)
      .values()
      .stream()
      .map(OrderList::getOrders)
      .flatMap(map -> map.entrySet().stream())
      .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
  }

  public Map<String, Order> getOrders() {
    Map<String, Order> ordersMap = new HashMap<>();
    priceMap.forEach((price, orderList) -> {
      Map<String, Order> orderMap = orderList.getOrders();
      ordersMap.putAll(orderMap);
    });
    return ordersMap;
  }

  public boolean contains(String id) {
    return orders.containsKey(id);
  }

  public Side getSide() {
    return side;
  }

  public String toString() {
    return priceMap.toString();
  }
}
