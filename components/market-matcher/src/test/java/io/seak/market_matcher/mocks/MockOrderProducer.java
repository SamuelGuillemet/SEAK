package io.seak.market_matcher.mocks;

import io.micronaut.configuration.kafka.annotation.KafkaClient;
import io.micronaut.configuration.kafka.annotation.KafkaKey;
import io.micronaut.configuration.kafka.annotation.Topic;
import io.seak.avro.Order;

@KafkaClient
public interface MockOrderProducer {
  @Topic("${kafka.topics.accepted-orders}")
  void sendOrder(@KafkaKey String key, Order order);
}
