package io.seak.market_matcher.mocks;

import io.micronaut.configuration.kafka.annotation.KafkaKey;
import io.micronaut.configuration.kafka.annotation.KafkaListener;
import io.micronaut.configuration.kafka.annotation.Topic;
import io.seak.avro.RejectedOrder;
import io.seak.avro.Trade;
import jakarta.inject.Singleton;
import java.util.ArrayList;
import java.util.List;

@Singleton
public class MockTradeListener {

  public List<Trade> trades = new ArrayList<>();
  public List<RejectedOrder> rejectedOrders = new ArrayList<>();

  @KafkaListener("mock-trades-consumer")
  @Topic("${kafka.topics.trades}")
  void receiveTrade(@KafkaKey String key, Trade trade) {
    trades.add(trade);
  }

  @KafkaListener("mock-rejected-orders-consumer")
  @Topic("${kafka.topics.rejected-orders}")
  void receiveRejectedOrder(@KafkaKey String key, RejectedOrder rejectedOrder) {
    rejectedOrders.add(rejectedOrder);
  }
}
