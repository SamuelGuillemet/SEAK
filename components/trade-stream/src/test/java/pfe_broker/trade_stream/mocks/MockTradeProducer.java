package pfe_broker.trade_stream.mocks;

import io.micronaut.configuration.kafka.annotation.KafkaClient;
import io.micronaut.configuration.kafka.annotation.KafkaKey;
import io.micronaut.configuration.kafka.annotation.Topic;
import pfe_broker.avro.Trade;

@KafkaClient
public interface MockTradeProducer {
  @Topic("${kafka.topics.trades}")
  void sendTrade(@KafkaKey String key, Trade trade);
}
