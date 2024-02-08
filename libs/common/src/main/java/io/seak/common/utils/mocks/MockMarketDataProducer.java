package io.seak.common.utils.mocks;

import io.micronaut.configuration.kafka.annotation.KafkaClient;
import io.micronaut.configuration.kafka.annotation.KafkaKey;
import io.micronaut.configuration.kafka.annotation.Topic;
import io.seak.avro.MarketData;

@KafkaClient
public interface MockMarketDataProducer {
  @Topic("market-data.AAPL")
  void sendMarketData(MarketData marketData);

  @Topic("market-data.AAPL")
  void sendMarketData(@KafkaKey String key, MarketData marketData);
}
