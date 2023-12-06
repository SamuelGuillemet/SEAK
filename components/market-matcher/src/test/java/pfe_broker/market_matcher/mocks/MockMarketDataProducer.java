package pfe_broker.market_matcher.mocks;

import io.micronaut.configuration.kafka.annotation.KafkaClient;
import io.micronaut.configuration.kafka.annotation.Topic;
import pfe_broker.avro.MarketData;

@KafkaClient
public interface MockMarketDataProducer {
  @Topic("market-data.AAPL")
  void sendMarketData(MarketData marketData);
}
