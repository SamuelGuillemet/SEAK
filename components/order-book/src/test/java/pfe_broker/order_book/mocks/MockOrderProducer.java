package pfe_broker.order_book.mocks;

import io.micronaut.configuration.kafka.annotation.KafkaClient;
import io.micronaut.configuration.kafka.annotation.KafkaKey;
import io.micronaut.configuration.kafka.annotation.Topic;
import pfe_broker.avro.Order;

@KafkaClient
public interface MockOrderProducer {
  @Topic("${kafka.topics.accepted-orders-order-book}")
  void sendOrder(@KafkaKey String key, Order order);
}
