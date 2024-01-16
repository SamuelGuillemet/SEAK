package pfe_broker.order_book;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import pfe_broker.avro.Order;

public class OrderList {

  private Double price;
  private Double volume;
  private Map<String, Order> orders;

  public OrderList(Double price) {
    this.price = price;
    this.volume = 0.0;
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

  public Order getOrder(String id) {
    return orders.get(id);
  }

  public double getPrice() {
    return price;
  }

  public Double getVolume() {
    return volume;
  }

  public Map<String, Order> getOrders() {
    return Map.copyOf(orders);
  }

  public String toString() {
    return "Price: " + price + " |Volume: " + volume + " |Orders: " + orders;
  }
}
