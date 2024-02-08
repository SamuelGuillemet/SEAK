package io.seak.quickfix_server;

import io.micronaut.configuration.kafka.annotation.KafkaKey;
import io.micronaut.configuration.kafka.annotation.KafkaListener;
import io.micronaut.configuration.kafka.annotation.Topic;
import io.seak.avro.MarketDataRejected;
import io.seak.avro.MarketDataResponse;
import io.seak.avro.OrderBookRequest;
import io.seak.avro.RejectedOrder;
import io.seak.avro.Trade;
import jakarta.inject.Singleton;
import java.util.List;
import org.apache.kafka.clients.consumer.ConsumerRecord;

@Singleton
public class ReportListener {

  private final ServerApplication serverApplication;

  public ReportListener(ServerApplication serverApplication) {
    this.serverApplication = serverApplication;
  }

  @KafkaListener(
    groupId = "quickfix-accepted-trades-consumer",
    pollTimeout = "0ms",
    batch = true
  )
  @Topic("${kafka.topics.accepted-trades}")
  void receiveAcceptedTrade(List<ConsumerRecord<String, Trade>> records) {
    records.forEach(item ->
      serverApplication.sendTradeReport(item.key(), item.value())
    );
  }

  @KafkaListener("quickfix-rejected-orders-consumer")
  @Topic("${kafka.topics.rejected-orders}")
  void receiveRejectedOrder(@KafkaKey String key, RejectedOrder rejectedOrder) {
    serverApplication.sendRejectedOrderReport(key, rejectedOrder);
  }

  @KafkaListener("quickfix-order-book-response")
  @Topic("${kafka.topics.order-book-response}")
  void receiveOrderBookResponse(
    @KafkaKey String key,
    OrderBookRequest orderBookRequest
  ) {
    serverApplication.sendOrderBookReport(key, orderBookRequest);
  }

  @KafkaListener("quickfix-order-book-rejected")
  @Topic("${kafka.topics.order-book-rejected}")
  void receiveOrderBookRejected(
    @KafkaKey String key,
    OrderBookRequest orderBookRequest
  ) {
    serverApplication.sendOrderBookRejected(key, orderBookRequest);
  }

  @KafkaListener(groupId = "quickfix-market-data-response", batch = true)
  @Topic("${kafka.topics.market-data-response}")
  void receiveMarketDataResponse(List<MarketDataResponse> marketDataResponses) {
    marketDataResponses.forEach(serverApplication::sendMarketDataSnapshot);
  }

  @KafkaListener("quickfix-market-data-rejected")
  @Topic("${kafka.topics.market-data-rejected}")
  void receiveMarketDataRejected(
    @KafkaKey String key,
    MarketDataRejected marketDataRejected
  ) {
    serverApplication.sendMarketDataRequestReject(marketDataRejected);
  }
}
