package pfe_broker.market_matcher;

import io.micronaut.configuration.kafka.annotation.KafkaClient;
import io.micronaut.configuration.kafka.annotation.KafkaKey;
import io.micronaut.configuration.kafka.annotation.Topic;
import pfe_broker.avro.RejectedOrder;

@KafkaClient(id = "market-matcher-rejected-order-producer")
public interface RejectedOrderProducer {
  @Topic("${kafka.topics.rejected-orders}")
  void sendRejectedOrder(@KafkaKey String key, RejectedOrder rejectedOrder);
}
