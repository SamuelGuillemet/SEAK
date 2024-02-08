package io.seak.quickfix_server;

import io.micronaut.configuration.kafka.annotation.KafkaClient;
import io.micronaut.configuration.kafka.annotation.KafkaKey;
import io.micronaut.configuration.kafka.annotation.Topic;
import io.seak.avro.MarketDataRequest;
import io.seak.avro.Order;
import io.seak.avro.OrderBookRequest;

@KafkaClient(id = "quickfix-order-producer")
public interface KafkaProducer {
  @Topic("${kafka.topics.orders}")
  void sendOrder(@KafkaKey String key, Order order);

  @Topic("${kafka.topics.order-book-request}")
  void sendOrderBookRequest(@KafkaKey String key, OrderBookRequest order);

  @Topic("${kafka.topics.market-data-request}")
  void sendMarketDataRequest(
    @KafkaKey String key,
    MarketDataRequest marketDataRequest
  );
}
