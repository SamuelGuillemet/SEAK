package pfe_broker.quickfix_server.mocks;

import io.micronaut.configuration.kafka.annotation.KafkaKey;
import io.micronaut.configuration.kafka.annotation.KafkaListener;
import io.micronaut.configuration.kafka.annotation.Topic;
import jakarta.inject.Singleton;
import java.util.ArrayList;
import java.util.List;
import pfe_broker.avro.Order;
import pfe_broker.avro.OrderBookRequest;

@Singleton
public class MockOrderListener {

  public List<Order> receivedOrders = new ArrayList<>();
  public List<OrderBookRequest> receivedOrderBookRequests = new ArrayList<>();

  @KafkaListener("mock-orders-consumer")
  @Topic("${kafka.topics.orders}")
  void receiveOrder(@KafkaKey String key, Order order) {
    receivedOrders.add(order);
  }

  @KafkaListener("mock-order-book-request-consumer")
  @Topic("${kafka.topics.order-book-request}")
  void receiveOrderBookRequest(@KafkaKey String key, OrderBookRequest order) {
    receivedOrderBookRequests.add(order);
  }
}
