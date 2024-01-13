package pfe_broker.order_book;

import io.micronaut.configuration.kafka.annotation.KafkaClient;
import io.micronaut.configuration.kafka.annotation.KafkaKey;
import io.micronaut.configuration.kafka.annotation.Topic;
import pfe_broker.avro.OrderBookRequest;
import pfe_broker.avro.Trade;

@KafkaClient
public interface TradeProducer {
  @Topic("${kafka.topics.trades}")
  void sendTrade(@KafkaKey String key, Trade trade);

  @Topic("${kafka.topics.order-book-response}")
  void sendOrderBookResponse(
    @KafkaKey String key,
    OrderBookRequest orderBookRequest
  );

  @Topic("${kafka.topics.order-book-rejected}")
  void sendOrderBookRejected(
    @KafkaKey String key,
    OrderBookRequest orderBookRequest
  );
}
