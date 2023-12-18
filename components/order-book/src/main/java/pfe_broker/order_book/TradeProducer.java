package pfe_broker.order_book;

import io.micronaut.configuration.kafka.annotation.KafkaClient;
import io.micronaut.configuration.kafka.annotation.KafkaKey;
import io.micronaut.configuration.kafka.annotation.Topic;
import pfe_broker.avro.Trade;

@KafkaClient
public interface TradeProducer {
  @Topic("${kafka.topics.trades}")
  void sendTrade(@KafkaKey String key, Trade trade);
}
