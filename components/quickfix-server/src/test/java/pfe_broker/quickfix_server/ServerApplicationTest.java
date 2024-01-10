package pfe_broker.quickfix_server;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import io.micronaut.context.annotation.Property;
import io.micronaut.core.annotation.NonNull;
import io.micronaut.test.annotation.TransactionMode;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import io.micronaut.test.support.TestPropertyProvider;
import jakarta.inject.Inject;
import java.time.Duration;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import pfe_broker.common.utils.KafkaTestContainer;
import pfe_broker.models.domains.User;
import pfe_broker.models.repositories.UserRepository;
import pfe_broker.quickfix_server.mocks.MockOrderListener;
import quickfix.FieldNotFound;
import quickfix.IncorrectTagValue;
import quickfix.SessionID;
import quickfix.UnsupportedMessageType;
import quickfix.field.MDEntryType;
import quickfix.field.MDReqID;
import quickfix.field.MDUpdateType;
import quickfix.field.MarketDepth;
import quickfix.field.SenderCompID;
import quickfix.field.SubscriptionRequestType;
import quickfix.field.Symbol;
import quickfix.field.TargetCompID;
import quickfix.fix44.MarketDataRequest;
import quickfix.fix44.MarketDataSnapshotFullRefresh;
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
public class ServerApplicationTest implements TestPropertyProvider {

  @Container
  static final KafkaTestContainer kafka = new KafkaTestContainer();

  @Inject
  private ServerApplication serverApplication;

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
  void cleanup(MockOrderListener mockOrderListener) {
    mockOrderListener.receivedOrders.clear();
    mockOrderListener.receivedOrderBookRequests.clear();
  }

  @Test
  public void testOnMessageNewOrderSingleMarket(
    MockOrderListener mockOrderListener
  ) throws FieldNotFound, UnsupportedMessageType, IncorrectTagValue {
    NewOrderSingle newOrderSingle = new NewOrderSingle(
      new quickfix.field.ClOrdID("1"),
      new quickfix.field.Side(quickfix.field.Side.BUY),
      new quickfix.field.TransactTime(),
      new quickfix.field.OrdType(quickfix.field.OrdType.MARKET)
    );
    newOrderSingle.set(new quickfix.field.Symbol("AAPL"));
    newOrderSingle.set(new quickfix.field.OrderQty(10));
    newOrderSingle.getHeader().setString(SenderCompID.FIELD, "testuser");

    serverApplication.onMessage(
      newOrderSingle,
      new SessionID("FIX.4.4", "testuser", "SERVER")
    );

    await()
      .pollInterval(Duration.ofSeconds(1))
      .atMost(Duration.ofSeconds(10))
      .untilAsserted(() -> {
        assertThat(mockOrderListener.receivedOrders).hasSize(1);
      });
  }

  @Test
  public void testOnMessageNewOrderSingleLimit(
    MockOrderListener mockOrderListener
  ) throws FieldNotFound, UnsupportedMessageType, IncorrectTagValue {
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

    serverApplication.onMessage(
      newOrderSingle,
      new SessionID("FIX.4.4", "testuser", "SERVER")
    );

    await()
      .pollInterval(Duration.ofSeconds(1))
      .atMost(Duration.ofSeconds(10))
      .untilAsserted(() -> {
        assertThat(mockOrderListener.receivedOrders).hasSize(1);
      });
  }

  @Test
  public void testOnMessageOrderCancelRequest(
    MockOrderListener mockOrderListener
  ) throws FieldNotFound, UnsupportedMessageType, IncorrectTagValue {
    OrderCancelRequest orderCancelRequest = new OrderCancelRequest(
      new quickfix.field.OrigClOrdID("1"),
      new quickfix.field.ClOrdID("2"),
      new quickfix.field.Side(quickfix.field.Side.BUY),
      new quickfix.field.TransactTime()
    );

    orderCancelRequest.set(new quickfix.field.OrderID("1"));
    orderCancelRequest.set(new quickfix.field.Symbol("AAPL"));
    orderCancelRequest.getHeader().setString(SenderCompID.FIELD, "testuser");

    serverApplication.onMessage(
      orderCancelRequest,
      new SessionID("FIX.4.4", "testuser", "SERVER")
    );

    await()
      .pollInterval(Duration.ofSeconds(1))
      .atMost(Duration.ofSeconds(10))
      .untilAsserted(() -> {
        assertThat(mockOrderListener.receivedOrderBookRequests).hasSize(1);
      });
  }

  @Test
  public void testOnMessageOrderCancelReplaceRequest(
    MockOrderListener mockOrderListener
  ) throws FieldNotFound, UnsupportedMessageType, IncorrectTagValue {
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

    serverApplication.onMessage(
      orderCancelReplaceRequest,
      new SessionID("FIX.4.4", "testuser", "SERVER")
    );

    await()
      .pollInterval(Duration.ofSeconds(1))
      .atMost(Duration.ofSeconds(10))
      .untilAsserted(() -> {
        assertThat(mockOrderListener.receivedOrderBookRequests).hasSize(1);
      });
  }
  // @Test
  // void createMarketDataSnapshotTest() throws FieldNotFound {
  //   MarketDataRequest marketDataRequest = new MarketDataRequest();

  //   marketDataRequest.set(new MDReqID("1"));
  //   marketDataRequest.set(
  //     new SubscriptionRequestType(SubscriptionRequestType.SNAPSHOT_UPDATES)
  //   );
  //   marketDataRequest.set(new MarketDepth(0));
  //   marketDataRequest.set(new MDUpdateType(MDUpdateType.FULL_REFRESH));
  //   marketDataRequest.getHeader().setField(new SenderCompID("user1"));
  //   marketDataRequest.getHeader().setField(new TargetCompID("SERVER"));

  //   MarketDataRequest.NoRelatedSym relatedSymbolGroup1 =
  //     new MarketDataRequest.NoRelatedSym();
  //   relatedSymbolGroup1.set(new Symbol("GOOGL"));
  //   marketDataRequest.addGroup(relatedSymbolGroup1);

  //   MarketDataRequest.NoMDEntryTypes entryTypeGroup1 =
  //     new MarketDataRequest.NoMDEntryTypes();
  //   entryTypeGroup1.set(new MDEntryType(MDEntryType.BID));
  //   marketDataRequest.addGroup(entryTypeGroup1);

  //   MarketDataRequest.NoRelatedSym relatedSymbolGroup2 =
  //     new MarketDataRequest.NoRelatedSym();
  //   relatedSymbolGroup2.set(new Symbol("AAPL"));
  //   marketDataRequest.addGroup(relatedSymbolGroup2);

  //   MarketDataRequest.NoMDEntryTypes entryTypeGroup2 =
  //     new MarketDataRequest.NoMDEntryTypes();
  //   entryTypeGroup2.set(new MDEntryType(MDEntryType.BID));
  //   marketDataRequest.addGroup(entryTypeGroup2);

  //   MarketDataSnapshotFullRefresh snapshot = null;
  //   snapshot = serverApplication.createMarketDataSnapshot(marketDataRequest);

  //   assertNotNull(snapshot);
  //   assertEquals("1", snapshot.getMDReqID().getValue());
  //   assertEquals(2, snapshot.getNoMDEntries().getValue());
  // }
}
