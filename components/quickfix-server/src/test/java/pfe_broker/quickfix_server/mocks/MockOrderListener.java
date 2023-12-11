package pfe_broker.quickfix_server.mocks;

import io.micronaut.configuration.kafka.annotation.KafkaKey;
import io.micronaut.configuration.kafka.annotation.KafkaListener;
import io.micronaut.configuration.kafka.annotation.Topic;
import jakarta.inject.Singleton;
import java.util.ArrayList;
import java.util.List;
import pfe_broker.avro.Order;

@Singleton
public class MockOrderListener {

  public List<Order> receivedOrders = new ArrayList<>();

  @KafkaListener("mock-orders-consumer")
  @Topic("${kafka.topics.orders}")
  void receiveOrder(@KafkaKey String key, Order order) {
    receivedOrders.add(order);
  }
}
