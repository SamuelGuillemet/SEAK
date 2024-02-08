package io.seak.quickfix_server;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import io.micronaut.context.annotation.Property;
import io.micronaut.core.annotation.NonNull;
import io.micronaut.test.annotation.TransactionMode;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import io.micronaut.test.support.TestPropertyProvider;
import io.seak.common.utils.KafkaTestContainer;
import io.seak.quickfix_server.mocks.MockKafkaListener;
import jakarta.inject.Inject;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import quickfix.FieldNotFound;
import quickfix.IncorrectDataFormat;
import quickfix.IncorrectTagValue;
import quickfix.SessionID;
import quickfix.UnsupportedMessageType;
import quickfix.field.SenderCompID;
import quickfix.fix44.MarketDataRequest;
import quickfix.fix44.NewOrderSingle;
import quickfix.fix44.OrderCancelReplaceRequest;
import quickfix.fix44.OrderCancelRequest;

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
class ServerApplicationTest implements TestPropertyProvider {

  @Container
  static final KafkaTestContainer kafka = new KafkaTestContainer();

  @Inject
  private ServerApplication serverApplication;

  @Override
  public @NonNull Map<String, String> getProperties() {
    if (!kafka.isRunning()) {
      kafka.start();
    }
    kafka.registerTopics(
      "orders",
      "accepted-trades",
      "rejected-orders",
      "order-book-request",
      "market-data-request"
    );
    return Map.of(
      "kafka.bootstrap.servers",
      kafka.getBootstrapServers(),
      "kafka.schema.registry.url",
      kafka.getSchemaRegistryUrl()
    );
  }

  @AfterEach
  void cleanup(MockKafkaListener mockKafkaListener) {
    mockKafkaListener.receivedOrders.clear();
    mockKafkaListener.receivedOrderBookRequests.clear();
  }

  @Test
  void testOnMessageNewOrderSingleMarket(MockKafkaListener mockKafkaListener)
    throws FieldNotFound, UnsupportedMessageType, IncorrectTagValue, IncorrectDataFormat {
    NewOrderSingle newOrderSingle = new NewOrderSingle(
      new quickfix.field.ClOrdID("1"),
      new quickfix.field.Side(quickfix.field.Side.BUY),
      new quickfix.field.TransactTime(),
      new quickfix.field.OrdType(quickfix.field.OrdType.MARKET)
    );
    newOrderSingle.set(new quickfix.field.Symbol("AAPL"));
    newOrderSingle.set(new quickfix.field.OrderQty(10));
    newOrderSingle.getHeader().setString(SenderCompID.FIELD, "testuser");

    serverApplication.fromApp(
      newOrderSingle,
      new SessionID("FIX.4.4", "testuser", "SERVER")
    );

    await()
      .pollInterval(Duration.ofSeconds(1))
      .atMost(Duration.ofSeconds(10))
      .untilAsserted(() -> {
        assertThat(mockKafkaListener.receivedOrders).hasSize(1);
      });
  }

  @Test
  void testOnMessageNewOrderSingleLimit(MockKafkaListener mockKafkaListener)
    throws FieldNotFound, UnsupportedMessageType, IncorrectTagValue, IncorrectDataFormat {
    NewOrderSingle newOrderSingle = new NewOrderSingle(
      new quickfix.field.ClOrdID("1"),
      new quickfix.field.Side(quickfix.field.Side.BUY),
      new quickfix.field.TransactTime(),
      new quickfix.field.OrdType(quickfix.field.OrdType.LIMIT)
    );
    newOrderSingle.set(new quickfix.field.Symbol("AAPL"));
    newOrderSingle.set(new quickfix.field.OrderQty(10));
    newOrderSingle.set(new quickfix.field.Price(100.0));
    newOrderSingle.getHeader().setString(SenderCompID.FIELD, "testuser");

    serverApplication.fromApp(
      newOrderSingle,
      new SessionID("FIX.4.4", "testuser", "SERVER")
    );

    await()
      .pollInterval(Duration.ofSeconds(1))
      .atMost(Duration.ofSeconds(10))
      .untilAsserted(() -> {
        assertThat(mockKafkaListener.receivedOrders).hasSize(1);
      });
  }

  @Test
  void testOnMessageOrderCancelRequest(MockKafkaListener mockKafkaListener)
    throws FieldNotFound, UnsupportedMessageType, IncorrectTagValue, IncorrectDataFormat {
    OrderCancelRequest orderCancelRequest = new OrderCancelRequest(
      new quickfix.field.OrigClOrdID("1"),
      new quickfix.field.ClOrdID("2"),
      new quickfix.field.Side(quickfix.field.Side.BUY),
      new quickfix.field.TransactTime()
    );

    orderCancelRequest.set(new quickfix.field.OrderID("1"));
    orderCancelRequest.set(new quickfix.field.Symbol("AAPL"));
    orderCancelRequest.getHeader().setString(SenderCompID.FIELD, "testuser");

    serverApplication.fromApp(
      orderCancelRequest,
      new SessionID("FIX.4.4", "testuser", "SERVER")
    );

    await()
      .pollInterval(Duration.ofSeconds(1))
      .atMost(Duration.ofSeconds(10))
      .untilAsserted(() -> {
        assertThat(mockKafkaListener.receivedOrderBookRequests).hasSize(1);
      });
  }

  @Test
  void testOnMessageOrderCancelReplaceRequest(
    MockKafkaListener mockKafkaListener
  )
    throws FieldNotFound, UnsupportedMessageType, IncorrectTagValue, IncorrectDataFormat {
    OrderCancelReplaceRequest orderCancelReplaceRequest =
      new OrderCancelReplaceRequest(
        new quickfix.field.OrigClOrdID("1"),
        new quickfix.field.ClOrdID("2"),
        new quickfix.field.Side(quickfix.field.Side.BUY),
        new quickfix.field.TransactTime(),
        new quickfix.field.OrdType(quickfix.field.OrdType.LIMIT)
      );
    orderCancelReplaceRequest.set(new quickfix.field.OrderID("1"));
    orderCancelReplaceRequest.set(new quickfix.field.Symbol("AAPL"));
    orderCancelReplaceRequest.set(new quickfix.field.OrderQty(10));
    orderCancelReplaceRequest.set(new quickfix.field.Price(100.0));
    orderCancelReplaceRequest
      .getHeader()
      .setString(SenderCompID.FIELD, "testuser");

    serverApplication.fromApp(
      orderCancelReplaceRequest,
      new SessionID("FIX.4.4", "testuser", "SERVER")
    );

    await()
      .pollInterval(Duration.ofSeconds(1))
      .atMost(Duration.ofSeconds(10))
      .untilAsserted(() -> {
        assertThat(mockKafkaListener.receivedOrderBookRequests).hasSize(1);
      });
  }

  @Test
  void createMarketDataSnapshotTest(MockKafkaListener mockKafkaListener)
    throws FieldNotFound, IncorrectTagValue, IncorrectDataFormat, UnsupportedMessageType {
    MarketDataRequest marketDataRequest = new MarketDataRequest(
      new quickfix.field.MDReqID("1"),
      new quickfix.field.SubscriptionRequestType(
        quickfix.field.SubscriptionRequestType.SNAPSHOT
      ),
      new quickfix.field.MarketDepth(1)
    );

    List<String> symbols = List.of("AAPL", "GOOGL");
    for (String symbol : symbols) {
      MarketDataRequest.NoRelatedSym relatedSymbolGroup =
        new MarketDataRequest.NoRelatedSym();
      relatedSymbolGroup.set(new quickfix.field.Symbol(symbol));
      marketDataRequest.addGroup(relatedSymbolGroup);
    }

    List<Character> mdEntryTypes = List.of(
      quickfix.field.MDEntryType.OPENING_PRICE,
      quickfix.field.MDEntryType.CLOSING_PRICE,
      quickfix.field.MDEntryType.TRADING_SESSION_HIGH_PRICE,
      quickfix.field.MDEntryType.TRADING_SESSION_LOW_PRICE
    );

    for (Character mdEntryType : mdEntryTypes) {
      MarketDataRequest.NoMDEntryTypes entryTypeGroup =
        new MarketDataRequest.NoMDEntryTypes();
      entryTypeGroup.set(new quickfix.field.MDEntryType(mdEntryType));
      marketDataRequest.addGroup(entryTypeGroup);
    }

    marketDataRequest.getHeader().setString(SenderCompID.FIELD, "testuser");

    serverApplication.fromApp(
      marketDataRequest,
      new SessionID("FIX.4.4", "testuser", "SERVER")
    );

    await()
      .pollInterval(Duration.ofSeconds(1))
      .atMost(Duration.ofSeconds(10))
      .untilAsserted(() -> {
        assertThat(mockKafkaListener.receivedMarketDataRequests).hasSize(1);
        assertThat(
          mockKafkaListener.receivedMarketDataRequests
            .get(0)
            .getMarketDataEntries()
        )
          .hasSize(4);
        assertThat(
          mockKafkaListener.receivedMarketDataRequests.get(0).getSymbols()
        )
          .hasSize(2);
      });
  }
}
