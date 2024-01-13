package pfe_broker.order_book;

import io.micronaut.configuration.kafka.annotation.KafkaListener;
import io.micronaut.configuration.kafka.annotation.OffsetReset;
import io.micronaut.configuration.kafka.annotation.Topic;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.List;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import pfe_broker.avro.Order;
import pfe_broker.avro.OrderBookRequest;
import pfe_broker.avro.OrderBookRequestType;

@Singleton
public class OrderListener {

  private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(
    OrderListener.class
  );

  @Inject
  private OrderBookCatalog orderBookCatalog;

  @Inject
  private IntegrityCheckService integrityCheckService;

  @Inject
  private TradeProducer tradeProducer;

  @KafkaListener(
    groupId = "order-book-orders",
    batch = true,
    offsetReset = OffsetReset.EARLIEST,
    pollTimeout = "0ms"
  )
  @Topic(patterns = "${kafka.topics.order-book-request}")
  public void receiveOrder(
    List<ConsumerRecord<String, OrderBookRequest>> records
  ) {
    records.forEach(record -> {
      handleOrderBookRequest(record.key(), record.value());
    });
  }

  private void handleOrderBookRequest(
    String key,
    OrderBookRequest orderBookRequest
  ) {
    Order order = orderBookRequest.getOrder();
    String symbol = order.getSymbol().toString();
    LOG.debug(
      "Received order book request: {} for symbol: {}",
      orderBookRequest,
      symbol
    );

    LimitOrderBook orderBook = orderBookCatalog.getOrderBook(symbol);
    if (orderBook == null) {
      orderBookCatalog.addOrderBook(symbol);
      orderBook = orderBookCatalog.getOrderBook(symbol);
    }

    if (orderBookRequest.getType() == OrderBookRequestType.NEW) {
      orderBook.addOrder(key, order);
      tradeProducer.sendOrderBookResponse(key, orderBookRequest);
      return;
    }

    Order oldOrder = orderBook.getOrder(key);

    if (oldOrder == null) {
      LOG.error(
        "Order {} could not be replaced/cancelled by {} because it does not exist",
        oldOrder,
        order
      );
      tradeProducer.sendOrderBookRejected(key, orderBookRequest);
      return;
    }
    if (
      !oldOrder.getClOrderID().equals((orderBookRequest.getOrigClOrderID()))
    ) {
      LOG.error("Matching of the clOrderID is wrong");
      tradeProducer.sendOrderBookRejected(key, orderBookRequest);
      return;
    }

    if (orderBookRequest.getType() == OrderBookRequestType.CANCEL) {
      Boolean integrityCheck = integrityCheckService.replaceCancelOrder(
        oldOrder,
        order
      );

      if (!integrityCheck) {
        LOG.error("Order {} could not be cancelled by {}", oldOrder, order);
        tradeProducer.sendOrderBookRejected(key, orderBookRequest);
        return;
      }

      LOG.debug("Order {} cancelled by {}", oldOrder, order);
      orderBook.removeOrder(key);
      tradeProducer.sendOrderBookResponse(key, orderBookRequest);
      return;
    }

    if (orderBookRequest.getType() == OrderBookRequestType.REPLACE) {
      if (oldOrder.getSide() != order.getSide()) {
        LOG.error("Modifification of the side is not allowed");
        tradeProducer.sendOrderBookRejected(key, orderBookRequest);
        return;
      }
      if (oldOrder.getType() != order.getType()) {
        LOG.error("Modifification of the type is not allowed");
        tradeProducer.sendOrderBookRejected(key, orderBookRequest);
        return;
      }

      Boolean integrityCheck = integrityCheckService.replaceCancelOrder(
        oldOrder,
        order
      );

      if (!integrityCheck) {
        LOG.error("Order {} could not be replaced by {}", oldOrder, order);
        tradeProducer.sendOrderBookRejected(key, orderBookRequest);
        return;
      }

      LOG.debug("Order {} replaced by {}", oldOrder, order);
      orderBook.replaceOrder(key, order);
      tradeProducer.sendOrderBookResponse(key, orderBookRequest);
      return;
    }
  }
}
