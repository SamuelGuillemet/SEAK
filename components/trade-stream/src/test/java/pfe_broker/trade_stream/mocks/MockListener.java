package pfe_broker.trade_stream.mocks;

import io.micronaut.configuration.kafka.annotation.KafkaKey;
import io.micronaut.configuration.kafka.annotation.KafkaListener;
import io.micronaut.configuration.kafka.annotation.Topic;
import jakarta.inject.Singleton;
import java.util.ArrayList;
import java.util.List;
import pfe_broker.avro.RejectedOrder;
import pfe_broker.avro.Trade;

@Singleton
public class MockListener {

  public List<Trade> acceptedTrades = new ArrayList<>();
  public List<RejectedOrder> rejectedOrders = new ArrayList<>();

  @KafkaListener("mock-trades-consumer")
  @Topic("${kafka.topics.accepted-trades}")
  void receiveAcceptedTrade(@KafkaKey String key, Trade trade) {
    acceptedTrades.add(trade);
  }

  @KafkaListener("mock-rejected-orders-consumer")
  @Topic("${kafka.topics.rejected-orders}")
  void receiveRejectedOrder(@KafkaKey String key, RejectedOrder order) {
    rejectedOrders.add(order);
  }
}
