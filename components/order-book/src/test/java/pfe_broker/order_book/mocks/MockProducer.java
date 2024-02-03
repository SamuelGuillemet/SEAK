package pfe_broker.order_book.mocks;

import io.micronaut.configuration.kafka.annotation.KafkaClient;
import io.micronaut.configuration.kafka.annotation.KafkaKey;
import io.micronaut.configuration.kafka.annotation.Topic;
import pfe_broker.avro.MarketDataRequest;
import pfe_broker.avro.OrderBookRequest;

@KafkaClient
public interface MockProducer {
  @Topic("${kafka.topics.order-book-request}")
  void sendOrderBookRequest(@KafkaKey String key, OrderBookRequest order);

  @Topic("${kafka.topics.market-data-request}")
  void sendMarketDataRequest(@KafkaKey String key, MarketDataRequest request);
}
