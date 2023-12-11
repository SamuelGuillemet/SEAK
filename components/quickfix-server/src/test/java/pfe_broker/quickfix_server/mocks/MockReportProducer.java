package pfe_broker.quickfix_server.mocks;

import io.micronaut.configuration.kafka.annotation.KafkaClient;
import io.micronaut.configuration.kafka.annotation.KafkaKey;
import io.micronaut.configuration.kafka.annotation.Topic;
import pfe_broker.avro.RejectedOrder;
import pfe_broker.avro.Trade;

@KafkaClient
public interface MockReportProducer {
  @Topic("${kafka.topics.accepted-orders}")
  void sendTrade(@KafkaKey String key, Trade trade);
  @Topic("${kafka.topics.rejected-orders}")
  void sendRejectedOrder(@KafkaKey String key, RejectedOrder rejectedOrder);
}
