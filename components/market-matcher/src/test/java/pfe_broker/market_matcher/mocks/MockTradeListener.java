package pfe_broker.market_matcher.mocks;

import io.micronaut.configuration.kafka.annotation.KafkaKey;
import io.micronaut.configuration.kafka.annotation.KafkaListener;
import io.micronaut.configuration.kafka.annotation.Topic;
import jakarta.inject.Singleton;
import java.util.ArrayList;
import java.util.List;
import pfe_broker.avro.Trade;

@Singleton
public class MockTradeListener {

  public List<Trade> trades = new ArrayList<>();

  @KafkaListener("mock-trades-consumer")
  @Topic("${kafka.topics.trades}")
  void receiveTrade(@KafkaKey String key, Trade trade) {
    trades.add(trade);
  }
}
