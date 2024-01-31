package pfe_broker.order_book.mocks;

import io.micronaut.configuration.kafka.annotation.KafkaKey;
import io.micronaut.configuration.kafka.annotation.KafkaListener;
import io.micronaut.configuration.kafka.annotation.Topic;
import jakarta.inject.Singleton;
import java.util.ArrayList;
import java.util.List;
import pfe_broker.avro.MarketDataRejected;
import pfe_broker.avro.MarketDataResponse;
import pfe_broker.avro.OrderBookRequest;
import pfe_broker.avro.Trade;

@Singleton
public class MockTradeListener {

  public List<Trade> trades = new ArrayList<>();
  public List<OrderBookRequest> orderBookRequests = new ArrayList<>();
  public List<MarketDataResponse> marketDataResponses = new ArrayList<>();
  public List<MarketDataRejected> marketDataRejecteds = new ArrayList<>();

  @KafkaListener("mock-trades-consumer")
  @Topic("${kafka.topics.trades}")
  void receiveTrade(@KafkaKey String key, Trade trade) {
    trades.add(trade);
  }

  @KafkaListener("mock-order-book-requests-consumer")
  @Topic("${kafka.topics.order-book-rejected}")
  void receiveOrderBookRequest(
    @KafkaKey String key,
    OrderBookRequest orderBookRequest
  ) {
    orderBookRequests.add(orderBookRequest);
  }

  @KafkaListener("mock-market-data-responses-consumer")
  @Topic("${kafka.topics.market-data-response}")
  void receiveMarketDataResponse(
    @KafkaKey String key,
    MarketDataResponse marketDataResponse
  ) {
    marketDataResponses.add(marketDataResponse);
  }

  @KafkaListener("mock-market-data-rejected-consumer")
  @Topic("${kafka.topics.market-data-rejected}")
  void receiveMarketDataRejected(
    @KafkaKey String key,
    MarketDataRejected marketDataRejected
  ) {
    marketDataRejecteds.add(marketDataRejected);
  }
}
