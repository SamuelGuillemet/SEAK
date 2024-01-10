package pfe_broker.order_book.mocks;

import io.micronaut.configuration.kafka.annotation.KafkaClient;
import io.micronaut.configuration.kafka.annotation.KafkaKey;
import io.micronaut.configuration.kafka.annotation.Topic;
import pfe_broker.avro.OrderBookRequest;

@KafkaClient
public interface MockOrderProducer {
  @Topic("${kafka.topics.order-book-request}")
  void sendOrderBookRequest(@KafkaKey String key, OrderBookRequest order);
}
