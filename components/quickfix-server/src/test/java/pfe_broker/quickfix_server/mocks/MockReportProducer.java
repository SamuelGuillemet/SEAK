package pfe_broker.quickfix_server.mocks;

import io.micronaut.configuration.kafka.annotation.KafkaClient;
import io.micronaut.configuration.kafka.annotation.KafkaKey;
import io.micronaut.configuration.kafka.annotation.Topic;
import pfe_broker.avro.MarketDataRejected;
import pfe_broker.avro.MarketDataResponse;
import pfe_broker.avro.OrderBookRequest;
import pfe_broker.avro.RejectedOrder;
import pfe_broker.avro.Trade;

@KafkaClient
public interface MockReportProducer {
  @Topic("${kafka.topics.accepted-trades}")
  void sendTrade(@KafkaKey String key, Trade trade);

  @Topic("${kafka.topics.rejected-orders}")
  void sendRejectedOrder(@KafkaKey String key, RejectedOrder rejectedOrder);

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

  @Topic("${kafka.topics.market-data-response}")
  void sendMarketDataResponse(
    @KafkaKey String key,
    MarketDataResponse marketDataResponse
  );

  @Topic("${kafka.topics.market-data-rejected}")
  void sendMarketDataRejected(
    @KafkaKey String key,
    MarketDataRejected marketDataRejected
  );
}
