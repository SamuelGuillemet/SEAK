package pfe_broker.quickfix_server;

import io.micronaut.configuration.kafka.annotation.KafkaKey;
import io.micronaut.configuration.kafka.annotation.KafkaListener;
import io.micronaut.configuration.kafka.annotation.Topic;
import jakarta.inject.Singleton;
import java.util.List;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import pfe_broker.avro.OrderBookRequest;
import pfe_broker.avro.RejectedOrder;
import pfe_broker.avro.Trade;

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
}
