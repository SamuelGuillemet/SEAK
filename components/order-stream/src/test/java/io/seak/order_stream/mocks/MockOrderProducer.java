package io.seak.order_stream.mocks;

import io.micronaut.configuration.kafka.annotation.KafkaClient;
import io.micronaut.configuration.kafka.annotation.KafkaKey;
import io.micronaut.configuration.kafka.annotation.Topic;
import io.seak.avro.Order;

@KafkaClient
public interface MockOrderProducer {
  @Topic("${kafka.topics.orders}")
  void sendOrder(@KafkaKey String key, Order order);
}
