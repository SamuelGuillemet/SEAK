package pfe_broker.order_book;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import pfe_broker.avro.Order;

public class OrderList {

  private Double price;
  private long volume;
  private Map<String, Order> orders;

  public OrderList(Double price) {
    this.price = price;
    orders = Collections.synchronizedMap(new HashMap<>());
  }

  public void addOrder(String id, Order order) {
    orders.put(id, order);
    volume += order.getQuantity();
  }

  public Order removeOrder(String id) {
    Order order = orders.get(id);
    volume -= order.getQuantity();
    return orders.remove(id);
  }

  public void replaceOrder(String id, Order order) {
    Order oldOrder = orders.get(id);
    volume -= oldOrder.getQuantity();
    volume += order.getQuantity();
    orders.put(id, order);
  }

  public double getPrice() {
    return price;
  }

  public long getVolume() {
    return volume;
  }

  public Map<String, Order> getOrders() {
    return Map.copyOf(orders);
  }

  public String toString() {
    return "Price: " + price + " |Volume: " + volume + " |Orders: " + orders;
  }
}
