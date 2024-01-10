package pfe_broker.quickfix_server;

import io.micronaut.configuration.kafka.annotation.KafkaClient;
import io.micronaut.configuration.kafka.annotation.KafkaKey;
import io.micronaut.configuration.kafka.annotation.Topic;
import pfe_broker.avro.Order;
import pfe_broker.avro.OrderBookRequest;

@KafkaClient(id = "quickfix-order-producer")
public interface OrderProducer {
  @Topic("${kafka.topics.orders}")
  void sendOrder(@KafkaKey String key, Order order);

  @Topic("${kafka.topics.order-book-request}")
  void sendOrderBookRequest(@KafkaKey String key, OrderBookRequest order);
}
