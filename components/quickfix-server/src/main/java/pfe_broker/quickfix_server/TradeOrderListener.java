package pfe_broker.quickfix_server;

import io.micronaut.configuration.kafka.annotation.KafkaKey;
import io.micronaut.configuration.kafka.annotation.KafkaListener;
import io.micronaut.configuration.kafka.annotation.Topic;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.List;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import pfe_broker.avro.RejectedOrder;
import pfe_broker.avro.Trade;

@Singleton
public class TradeOrderListener {

  @Inject
  private ServerApplication serverApplication;

  @KafkaListener(
    groupId = "quickfix-accepted-trades-consumer",
    pollTimeout = "0ms",
    batch = true
  )
  @Topic("${kafka.topics.accepted-trades}")
  void receiveAcceptedTrade(List<ConsumerRecord<String, Trade>> records) {
    records.forEach(record -> {
      serverApplication.sendExecutionReport(record.key(), record.value());
    });
  }

  @KafkaListener("quickfix-rejected-orders-consumer")
  @Topic("${kafka.topics.rejected-orders}")
  void receiveRejectedOrder(@KafkaKey String key, RejectedOrder rejectedOrder) {
    serverApplication.sendOrderCancelReject(key, rejectedOrder);
  }
}
