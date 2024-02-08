package pfe_broker.order_book.order_book;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micronaut.configuration.kafka.annotation.KafkaListener;
import io.micronaut.configuration.kafka.annotation.OffsetReset;
import io.micronaut.configuration.kafka.annotation.Topic;
import jakarta.inject.Singleton;
import java.util.List;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import pfe_broker.avro.Order;
import pfe_broker.avro.OrderBookRequest;
import pfe_broker.avro.OrderBookRequestType;
import pfe_broker.order_book.MessageProducer;

@Singleton
public class OrderListener {

  private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(
    OrderListener.class
  );

  private final OrderBookCatalog orderBookCatalog;
  private final IntegrityCheckService integrityCheckService;
  private final MessageProducer messageProducer;
  private final MeterRegistry meterRegistry;

  public OrderListener(
    OrderBookCatalog orderBookCatalog,
    IntegrityCheckService integrityCheckService,
    MessageProducer messageProducer,
    MeterRegistry meterRegistry
  ) {
    this.orderBookCatalog = orderBookCatalog;
    this.integrityCheckService = integrityCheckService;
    this.messageProducer = messageProducer;
    this.meterRegistry = meterRegistry;
  }

  @KafkaListener(
    groupId = "order-book-orders",
    batch = true,
    offsetReset = OffsetReset.EARLIEST
  )
  @Topic(patterns = "${kafka.topics.order-book-request}")
  public void receiveOrder(
    List<ConsumerRecord<String, OrderBookRequest>> records
  ) {
    records.forEach(item -> {
      Timer orderBookHandleOrderTimer = meterRegistry.timer(
        "order_book_handle_order",
        "symbol",
        item.value().getOrder().getSymbol().toString(),
        "requestType",
        item.value().getType().toString()
      );
      Timer.Sample sample = Timer.start();
      handleOrderBookRequest(item.key(), item.value());
      sample.stop(orderBookHandleOrderTimer);
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
      messageProducer.sendOrderBookResponse(key, orderBookRequest);
      return;
    }

    Order oldOrder = orderBook.getOrder(key);

    if (oldOrder == null) {
      LOG.error(
        "Order could not be replaced/cancelled by {} because it does not exist",
        order
      );
      messageProducer.sendOrderBookRejected(key, orderBookRequest);
      return;
    }
    if (
      !oldOrder.getClOrderID().equals((orderBookRequest.getOrigClOrderID()))
    ) {
      LOG.error("Matching of the clOrderID is wrong");
      messageProducer.sendOrderBookRejected(key, orderBookRequest);
      return;
    }

    if (orderBookRequest.getType() == OrderBookRequestType.CANCEL) {
      integrityCheckService.cancelOrder(oldOrder);

      LOG.debug("Order {} cancelled by {}", oldOrder, order);
      orderBook.removeOrder(key);
      messageProducer.sendOrderBookResponse(key, orderBookRequest);
      return;
    }

    if (orderBookRequest.getType() == OrderBookRequestType.REPLACE) {
      if (oldOrder.getSide() != order.getSide()) {
        LOG.error("Modifification of the side is not allowed");
        messageProducer.sendOrderBookRejected(key, orderBookRequest);
        return;
      }
      if (oldOrder.getType() != order.getType()) {
        LOG.error("Modifification of the type is not allowed");
        messageProducer.sendOrderBookRejected(key, orderBookRequest);
        return;
      }

      boolean integrityCheck = integrityCheckService.replaceOrder(
        oldOrder,
        order
      );

      if (!integrityCheck) {
        LOG.error("Order {} could not be replaced by {}", oldOrder, order);
        messageProducer.sendOrderBookRejected(key, orderBookRequest);
        return;
      }

      LOG.debug("Order {} replaced by {}", oldOrder, order);
      orderBook.replaceOrder(key, order);
      messageProducer.sendOrderBookResponse(key, orderBookRequest);
    }
  }
}
