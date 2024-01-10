package pfe_broker.quickfix_server;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;

import io.micronaut.context.annotation.Property;
import io.micronaut.core.annotation.NonNull;
import io.micronaut.test.annotation.TransactionMode;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import io.micronaut.test.support.TestPropertyProvider;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import pfe_broker.avro.Order;
import pfe_broker.avro.OrderBookRequest;
import pfe_broker.avro.OrderBookRequestType;
import pfe_broker.avro.OrderRejectReason;
import pfe_broker.avro.RejectedOrder;
import pfe_broker.avro.Side;
import pfe_broker.avro.Trade;
import pfe_broker.avro.Type;
import pfe_broker.common.utils.KafkaTestContainer;
import pfe_broker.quickfix_server.mocks.MockMessageSender;
import pfe_broker.quickfix_server.mocks.MockReportProducer;
import quickfix.FieldNotFound;
import quickfix.Message;
import quickfix.field.CxlRejResponseTo;
import quickfix.field.ExecType;
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
public class ReportListenerTest implements TestPropertyProvider {

  @Container
  static final KafkaTestContainer kafka = new KafkaTestContainer();

  @Override
  public @NonNull Map<String, String> getProperties() {
    if (!kafka.isRunning()) {
      kafka.start();
    }
    kafka.registerTopics("orders", "accepted-trades", "rejected-orders");
    return Map.of(
      "kafka.bootstrap.servers",
      kafka.getBootstrapServers(),
      "kafka.schema.registry.url",
      kafka.getSchemaRegistryUrl()
    );
  }

  @AfterEach
  public void clearMessages(MockMessageSender mockMessageSender) {
    mockMessageSender.messages.clear();
  }

  @Test
  public void testAcceptedTrade(
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
      .atMost(5, TimeUnit.SECONDS)
      .untilAsserted(() -> {
        assertEquals(1, mockMessageSender.messages.size());
      });
    Message message = mockMessageSender.messages.take();
    assertEquals('1', message.getChar(OrderID.FIELD));
  }

  @Test
  public void testRejectedOrder(
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
      .atMost(5, TimeUnit.SECONDS)
      .untilAsserted(() -> {
        assertEquals(1, mockMessageSender.messages.size());
      });

    Message message = mockMessageSender.messages.take();
    assertEquals('1', message.getChar(OrderID.FIELD));
  }

  @Test
  public void testOrderBookResponseNew(
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
      .atMost(5, TimeUnit.SECONDS)
      .untilAsserted(() -> {
        assertEquals(1, mockMessageSender.messages.size());
      });

    Message message = mockMessageSender.messages.take();
    assertEquals('1', message.getChar(OrderID.FIELD));
    assertEquals(ExecType.NEW, message.getChar(ExecType.FIELD));
    assertEquals(OrdStatus.NEW, message.getChar(OrdStatus.FIELD));
  }

  @Test
  public void testOrderBookResponseCancel(
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
      .atMost(5, TimeUnit.SECONDS)
      .untilAsserted(() -> {
        assertEquals(1, mockMessageSender.messages.size());
      });

    Message message = mockMessageSender.messages.take();
    assertEquals('1', message.getChar(OrderID.FIELD));
    assertEquals(ExecType.CANCELED, message.getChar(ExecType.FIELD));
    assertEquals(OrdStatus.CANCELED, message.getChar(OrdStatus.FIELD));
  }

  @Test
  public void testOrderBookReplace(
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
      .atMost(5, TimeUnit.SECONDS)
      .untilAsserted(() -> {
        assertEquals(1, mockMessageSender.messages.size());
      });

    Message message = mockMessageSender.messages.take();
    assertEquals('1', message.getChar(OrderID.FIELD));
    assertEquals(ExecType.NEW, message.getChar(ExecType.FIELD));
    assertEquals(OrdStatus.REPLACED, message.getChar(OrdStatus.FIELD));
  }

  @Test
  public void testOrderBookRejectedCancel(
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
      .atMost(5, TimeUnit.SECONDS)
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
  public void testOrderBookRejectedReplace(
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
      .atMost(5, TimeUnit.SECONDS)
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
}
