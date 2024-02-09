package io.seak.quickfix_server;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;

import io.micronaut.context.annotation.Property;
import io.micronaut.core.annotation.NonNull;
import io.micronaut.test.annotation.TransactionMode;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import io.micronaut.test.support.TestPropertyProvider;
import io.seak.avro.MarketData;
import io.seak.avro.MarketDataEntry;
import io.seak.avro.MarketDataRejected;
import io.seak.avro.MarketDataRejectedReason;
import io.seak.avro.MarketDataResponse;
import io.seak.avro.Order;
import io.seak.avro.OrderBookRequest;
import io.seak.avro.OrderBookRequestType;
import io.seak.avro.OrderRejectReason;
import io.seak.avro.RejectedOrder;
import io.seak.avro.Side;
import io.seak.avro.Trade;
import io.seak.avro.Type;
import io.seak.common.utils.KafkaTestContainer;
import io.seak.quickfix_server.mocks.MockMessageSender;
import io.seak.quickfix_server.mocks.MockReportProducer;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import quickfix.FieldNotFound;
import quickfix.Group;
import quickfix.Message;
import quickfix.field.CxlRejResponseTo;
import quickfix.field.ExecType;
import quickfix.field.MDReqID;
import quickfix.field.OrdStatus;
import quickfix.field.OrderID;

@MicronautTest(
  rollback = false,
  transactional = false,
  transactionMode = TransactionMode.SINGLE_TRANSACTION
)
@Property(
  name = "datasources.default.driver-class-name",
  value = "org.testcontainers.jdbc.ContainerDatabaseDriver"
)
@Property(
  name = "datasources.default.url",
  value = "jdbc:tc:postgresql:16.1:///db"
)
@Testcontainers(disabledWithoutDocker = true)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ReportListenerTest implements TestPropertyProvider {

  @Container
  static final KafkaTestContainer kafka = new KafkaTestContainer();

  @Override
  public @NonNull Map<String, String> getProperties() {
    if (!kafka.isRunning()) {
      kafka.start();
    }
    kafka.registerTopics(
      "orders",
      "accepted-trades",
      "rejected-orders",
      "order-book-responses",
      "order-book-rejected"
    );
    return Map.of(
      "kafka.bootstrap.servers",
      kafka.getBootstrapServers(),
      "kafka.schema.registry.url",
      kafka.getSchemaRegistryUrl()
    );
  }

  @BeforeEach
  public void clearMessages(MockMessageSender mockMessageSender) {
    mockMessageSender.messages.clear();
  }

  @Test
  void testAcceptedTrade(
    MockReportProducer mockReportProducer,
    ServerApplication serverApplication,
    MockMessageSender mockMessageSender
  ) throws InterruptedException, FieldNotFound {
    Order order = new Order(
      "testuser",
      "AAPL",
      10,
      Side.BUY,
      Type.MARKET,
      null,
      "0"
    );
    Trade trade = new Trade(order, "APPL", 100.0, 10);

    mockReportProducer.sendTrade("testuser:1", trade);

    await()
      .atMost(10, TimeUnit.SECONDS)
      .untilAsserted(() -> {
        assertEquals(1, mockMessageSender.messages.size());
      });
    Message message = mockMessageSender.messages.take();
    assertEquals('1', message.getChar(OrderID.FIELD));
  }

  @Test
  void testRejectedOrder(
    MockReportProducer mockReportProducer,
    ServerApplication serverApplication,
    MockMessageSender mockMessageSender
  ) throws InterruptedException, FieldNotFound {
    Order order = new Order(
      "testuser",
      "AAPL",
      10,
      Side.BUY,
      Type.MARKET,
      null,
      "0"
    );
    RejectedOrder rejectedOrder = new RejectedOrder(
      order,
      OrderRejectReason.OTHER
    );

    mockReportProducer.sendRejectedOrder("testuser:1", rejectedOrder);

    await()
      .atMost(10, TimeUnit.SECONDS)
      .untilAsserted(() -> {
        assertEquals(1, mockMessageSender.messages.size());
      });

    Message message = mockMessageSender.messages.take();
    assertEquals('1', message.getChar(OrderID.FIELD));
  }

  @Test
  void testOrderBookResponseNew(
    MockReportProducer mockReportProducer,
    ServerApplication serverApplication,
    MockMessageSender mockMessageSender
  ) throws InterruptedException, FieldNotFound {
    Order order = new Order(
      "testuser",
      "AAPL",
      10,
      Side.BUY,
      Type.LIMIT,
      100.0,
      "0"
    );
    OrderBookRequest orderBookRequest = new OrderBookRequest(
      OrderBookRequestType.NEW,
      order,
      null
    );

    mockReportProducer.sendOrderBookResponse("testuser:1", orderBookRequest);

    await()
      .atMost(10, TimeUnit.SECONDS)
      .untilAsserted(() -> {
        assertEquals(1, mockMessageSender.messages.size());
      });

    Message message = mockMessageSender.messages.take();
    assertEquals('1', message.getChar(OrderID.FIELD));
    assertEquals(ExecType.NEW, message.getChar(ExecType.FIELD));
    assertEquals(OrdStatus.NEW, message.getChar(OrdStatus.FIELD));
  }

  @Test
  void testOrderBookResponseCancel(
    MockReportProducer mockReportProducer,
    ServerApplication serverApplication,
    MockMessageSender mockMessageSender
  ) throws FieldNotFound, InterruptedException {
    Order order = new Order(
      "testuser",
      "AAPL",
      0,
      Side.BUY,
      Type.LIMIT,
      0.0,
      "1"
    );
    OrderBookRequest orderBookRequest = new OrderBookRequest(
      OrderBookRequestType.CANCEL,
      order,
      "0"
    );

    mockReportProducer.sendOrderBookResponse("testuser:1", orderBookRequest);

    await()
      .atMost(10, TimeUnit.SECONDS)
      .untilAsserted(() -> {
        assertEquals(1, mockMessageSender.messages.size());
      });

    Message message = mockMessageSender.messages.take();
    assertEquals('1', message.getChar(OrderID.FIELD));
    assertEquals(ExecType.CANCELED, message.getChar(ExecType.FIELD));
    assertEquals(OrdStatus.CANCELED, message.getChar(OrdStatus.FIELD));
  }

  @Test
  void testOrderBookReplace(
    MockReportProducer mockReportProducer,
    ServerApplication serverApplication,
    MockMessageSender mockMessageSender
  ) throws InterruptedException, FieldNotFound {
    Order order = new Order(
      "testuser",
      "AAPL",
      10,
      Side.BUY,
      Type.LIMIT,
      90.0,
      "1"
    );
    OrderBookRequest orderBookRequest = new OrderBookRequest(
      OrderBookRequestType.REPLACE,
      order,
      "0"
    );

    mockReportProducer.sendOrderBookResponse("testuser:1", orderBookRequest);

    await()
      .atMost(10, TimeUnit.SECONDS)
      .untilAsserted(() -> {
        assertEquals(1, mockMessageSender.messages.size());
      });

    Message message = mockMessageSender.messages.take();
    assertEquals('1', message.getChar(OrderID.FIELD));
    assertEquals(ExecType.REPLACED, message.getChar(ExecType.FIELD));
    assertEquals(OrdStatus.NEW, message.getChar(OrdStatus.FIELD));
  }

  @Test
  void testOrderBookRejectedCancel(
    MockReportProducer mockReportProducer,
    ServerApplication serverApplication,
    MockMessageSender mockMessageSender
  ) throws FieldNotFound, InterruptedException {
    Order order = new Order(
      "testuser",
      "AAPL",
      0,
      Side.BUY,
      Type.LIMIT,
      0.0,
      "1"
    );
    OrderBookRequest orderBookRequest = new OrderBookRequest(
      OrderBookRequestType.CANCEL,
      order,
      "0"
    );

    mockReportProducer.sendOrderBookRejected("testuser:1", orderBookRequest);

    await()
      .atMost(10, TimeUnit.SECONDS)
      .untilAsserted(() -> {
        assertEquals(1, mockMessageSender.messages.size());
      });

    Message message = mockMessageSender.messages.take();
    assertEquals('1', message.getChar(OrderID.FIELD));
    assertEquals(
      CxlRejResponseTo.ORDER_CANCEL_REQUEST,
      message.getChar(CxlRejResponseTo.FIELD)
    );
    assertEquals(OrdStatus.REJECTED, message.getChar(OrdStatus.FIELD));
  }

  @Test
  void testOrderBookRejectedReplace(
    MockReportProducer mockReportProducer,
    ServerApplication serverApplication,
    MockMessageSender mockMessageSender
  ) throws InterruptedException, FieldNotFound {
    Order order = new Order(
      "testuser",
      "AAPL",
      10,
      Side.BUY,
      Type.LIMIT,
      90.0,
      "1"
    );
    OrderBookRequest orderBookRequest = new OrderBookRequest(
      OrderBookRequestType.REPLACE,
      order,
      "0"
    );

    mockReportProducer.sendOrderBookRejected("testuser:1", orderBookRequest);

    await()
      .atMost(10, TimeUnit.SECONDS)
      .untilAsserted(() -> {
        assertEquals(1, mockMessageSender.messages.size());
      });

    Message message = mockMessageSender.messages.take();
    assertEquals('1', message.getChar(OrderID.FIELD));
    assertEquals(
      CxlRejResponseTo.ORDER_CANCEL_REPLACE_REQUEST,
      message.getChar(CxlRejResponseTo.FIELD)
    );
    assertEquals(OrdStatus.REJECTED, message.getChar(OrdStatus.FIELD));
  }

  @Test
  void testMarketDataResponse(
    MockReportProducer mockReportProducer,
    ServerApplication serverApplication,
    MockMessageSender mockMessageSender
  ) throws InterruptedException, FieldNotFound {
    MarketDataResponse marketDataResponse = new MarketDataResponse(
      "testuser",
      "AAPL",
      List.of(
        new MarketData(100.0, 100.0, 100.0, 100.0, 10),
        new MarketData(90.0, 90.0, 90.0, 90.0, 10)
      ),
      "1",
      List.of(
        MarketDataEntry.CLOSE,
        MarketDataEntry.HIGH,
        MarketDataEntry.LOW,
        MarketDataEntry.OPEN
      )
    );

    mockReportProducer.sendMarketDataResponse("testuser:1", marketDataResponse);

    await()
      .atMost(10, TimeUnit.SECONDS)
      .untilAsserted(() -> {
        assertEquals(1, mockMessageSender.messages.size());
      });

    Message message = mockMessageSender.messages.take();

    assertEquals('1', message.getChar(MDReqID.FIELD));
    assertEquals(8, message.getInt(quickfix.field.NoMDEntries.FIELD));

    for (Group group : message.getGroups(quickfix.field.NoMDEntries.FIELD)) {
      if (group.getDouble(quickfix.field.MDEntryPx.FIELD) == 100.0) {
        assertEquals(
          2, // Oldest entry
          group.getInt(quickfix.field.MDEntryPositionNo.FIELD)
        );
      } else if (group.getDouble(quickfix.field.MDEntryPx.FIELD) == 90.0) {
        assertEquals(
          1, // Newest entry
          group.getInt(quickfix.field.MDEntryPositionNo.FIELD)
        );
      } else {
        throw new AssertionError("Unexpected price");
      }
    }
  }

  @Test
  void testMarketDataRejection(
    MockReportProducer mockReportProducer,
    ServerApplication serverApplication,
    MockMessageSender mockMessageSender
  ) throws InterruptedException, FieldNotFound {
    MarketDataRejected marketDataRejected = new MarketDataRejected(
      "testuser",
      "1",
      MarketDataRejectedReason.UNKNOWN_SYMBOL
    );

    mockReportProducer.sendMarketDataRejected("testuser:1", marketDataRejected);

    await()
      .atMost(10, TimeUnit.SECONDS)
      .untilAsserted(() -> {
        assertEquals(1, mockMessageSender.messages.size());
      });

    Message message = mockMessageSender.messages.take();

    assertEquals('1', message.getChar(MDReqID.FIELD));
    assertEquals(
      quickfix.field.MDReqRejReason.UNKNOWN_SYMBOL,
      message.getChar(quickfix.field.MDReqRejReason.FIELD)
    );
  }
}
