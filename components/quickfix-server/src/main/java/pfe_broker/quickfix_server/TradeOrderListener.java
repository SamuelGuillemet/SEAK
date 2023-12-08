package pfe_broker.quickfix_server;

import io.micronaut.configuration.kafka.annotation.KafkaKey;
import io.micronaut.configuration.kafka.annotation.KafkaListener;
import io.micronaut.configuration.kafka.annotation.Topic;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import pfe_broker.avro.RejectedOrder;
import pfe_broker.avro.Trade;

@Singleton
public class TradeOrderListener {

  @Inject
  private ApplicationMessageCracker applicationMessageCracker;

  @KafkaListener("accepted-trades-consumer")
  @Topic("${kafka.topics.accepted-trades}")
  void receiveAcceptedTrade(@KafkaKey String key, Trade trade) {
    applicationMessageCracker.sendExecutionReport(key, trade);
  }

  @KafkaListener("rejected-orders-consumer")
  @Topic("${kafka.topics.rejected-orders}")
  void receiveRejectedOrder(@KafkaKey String key, RejectedOrder rejectedOrder) {
    applicationMessageCracker.sendOrderCancelReject(key, rejectedOrder);
  }
}
