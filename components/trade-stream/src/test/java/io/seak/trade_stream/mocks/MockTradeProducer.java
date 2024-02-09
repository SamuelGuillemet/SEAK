package io.seak.trade_stream.mocks;

import io.micronaut.configuration.kafka.annotation.KafkaClient;
import io.micronaut.configuration.kafka.annotation.KafkaKey;
import io.micronaut.configuration.kafka.annotation.Topic;
import io.seak.avro.Trade;

@KafkaClient
public interface MockTradeProducer {
  @Topic("${kafka.topics.trades}")
  void sendTrade(@KafkaKey String key, Trade trade);
}
