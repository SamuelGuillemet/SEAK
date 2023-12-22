package pfe_broker.order_book;

import io.micronaut.configuration.kafka.annotation.KafkaListener;
import io.micronaut.configuration.kafka.annotation.OffsetReset;
import io.micronaut.configuration.kafka.annotation.Topic;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.List;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import pfe_broker.avro.Order;

@Singleton
public class OrderListener {

  private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(
    OrderListener.class
  );

  @Inject
  private OrderBookCatalog orderBooks;

  @KafkaListener(
    groupId = "order-book-orders",
    batch = true,
    offsetReset = OffsetReset.EARLIEST,
    pollTimeout = "0ms"
  )
  @Topic(patterns = "${kafka.topics.accepted-orders-order-book}")
  public void receiveMarketData(List<ConsumerRecord<String, Order>> records) {
    records.forEach(record -> {
      Order order = record.value();
      String key = record.key();
      String symbol = order.getSymbol().toString();
      LOG.debug("Received order: {} for symbol: {}", order, symbol);
      LimitOrderBook orderBook = orderBooks.getOrderBook(symbol);
      if (orderBook == null) {
        orderBooks.addOrderBook(symbol);
        orderBook = orderBooks.getOrderBook(symbol);
      }

      orderBook.addOrder(key, order);
    });
  }
}
