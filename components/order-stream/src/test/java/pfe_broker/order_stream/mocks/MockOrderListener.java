package pfe_broker.order_stream.mocks;

import io.micronaut.configuration.kafka.annotation.KafkaKey;
import io.micronaut.configuration.kafka.annotation.KafkaListener;
import io.micronaut.configuration.kafka.annotation.Topic;
import jakarta.inject.Singleton;
import java.util.ArrayList;
import java.util.List;
import pfe_broker.avro.Order;
import pfe_broker.avro.RejectedOrder;

@Singleton
public class MockOrderListener {

  public List<Order> acceptedOrders = new ArrayList<>();
  public List<RejectedOrder> rejectedOrders = new ArrayList<>();

  @KafkaListener("mock-orders-consumer")
  @Topic("${kafka.topics.accepted-orders}")
  void receiveAcceptedOrder(@KafkaKey String key, Order order) {
    acceptedOrders.add(order);
  }

  @KafkaListener("mock-rejected-orders-consumer")
  @Topic("${kafka.topics.rejected-orders}")
  void receiveRejectedOrder(@KafkaKey String key, RejectedOrder order) {
    rejectedOrders.add(order);
  }
}
