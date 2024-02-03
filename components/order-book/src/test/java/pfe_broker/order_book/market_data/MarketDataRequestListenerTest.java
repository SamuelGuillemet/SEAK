package pfe_broker.order_book.market_data;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import io.micronaut.core.annotation.NonNull;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import io.micronaut.test.support.TestPropertyProvider;
import jakarta.inject.Inject;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import pfe_broker.avro.MarketData;
import pfe_broker.avro.MarketDataRequest;
import pfe_broker.avro.MarketDataSubscriptionRequest;
import pfe_broker.common.utils.KafkaTestContainer;
import pfe_broker.common.utils.mocks.MockMarketDataProducer;
import pfe_broker.order_book.mocks.MockProducer;
import pfe_broker.order_book.mocks.MockTradeListener;

@MicronautTest(transactional = false)
@Testcontainers(disabledWithoutDocker = true)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MarketDataRequestListenerTest implements TestPropertyProvider {

  @Container
  static final KafkaTestContainer kafka = new KafkaTestContainer();

  @Inject
  MarketDataSubscriptionCatalog marketDataSubscriptionCatalog;

  private final String symbol = "AAPL";

  @Inject
  MockTradeListener mockTradeListener;

  @Inject
  MockProducer mockOrderProducer;

  @Override
  public @NonNull Map<String, String> getProperties() {
    if (!kafka.isRunning()) {
      kafka.start();
    }
    kafka.registerTopics(
      "market-data-request",
      "market-data-response",
      "market-data-rejected",
      "market-data.AAPL"
    );

    return Map.of(
      "kafka.bootstrap.servers",
      kafka.getBootstrapServers(),
      "kafka.schema.registry.url",
      kafka.getSchemaRegistryUrl()
    );
  }

  @BeforeEach
  void reset() {
    mockTradeListener.marketDataResponses.clear();
    mockTradeListener.marketDataRejecteds.clear();
    marketDataSubscriptionCatalog.clear();
  }

  @BeforeAll
  void setup(MockMarketDataProducer mockMarketDataProducer) {
    Instant now = Instant.now();
    for (int i = 0; i < 100; i++) {
      mockMarketDataProducer.sendMarketData(
        now.toString(),
        MarketData
          .newBuilder()
          .setClose(100.0f)
          .setHigh(110.0f)
          .setLow(90.0f)
          .setOpen(95.0f)
          .setVolume(10 + i)
          .build()
      );
      now = now.plus(Duration.ofSeconds(1));
    }
  }

  @Test
  void testMarketDataRequestSnapshot() throws InterruptedException {
    MarketDataRequest marketDataRequest = new MarketDataRequest(
      "testuser",
      List.of(symbol),
      10,
      List.of(),
      MarketDataSubscriptionRequest.SNAPSHOT,
      "1"
    );

    mockOrderProducer.sendMarketDataRequest("testuser:1", marketDataRequest);

    await()
      .atMost(Duration.ofSeconds(5))
      .pollInterval(Duration.ofSeconds(1))
      .untilAsserted(() -> {
        assertThat(marketDataSubscriptionCatalog.getMarketDataRequests(symbol))
          .isEmpty();
        assertThat(mockTradeListener.marketDataResponses).hasSize(1);
        assertThat(
          mockTradeListener.marketDataResponses.get(0).getSymbol().toString()
        )
          .hasToString(symbol);
      });
  }

  @Test
  void testMarketDataRequestSubscribe() throws InterruptedException {
    MarketDataRequest marketDataRequest = new MarketDataRequest(
      "testuser",
      List.of(symbol),
      10,
      List.of(),
      MarketDataSubscriptionRequest.SUBSCRIBE,
      "1"
    );

    mockOrderProducer.sendMarketDataRequest("testuser:1", marketDataRequest);

    await()
      .atMost(Duration.ofSeconds(5))
      .pollInterval(Duration.ofSeconds(1))
      .untilAsserted(() -> {
        assertThat(marketDataSubscriptionCatalog.getMarketDataRequests(symbol))
          .hasSize(1);
      });
  }

  @Test
  void testMarketDataRequestUnsubscribe() throws InterruptedException {
    MarketDataRequest marketDataRequestSub = new MarketDataRequest(
      "testuser",
      List.of(symbol),
      10,
      List.of(),
      MarketDataSubscriptionRequest.SUBSCRIBE,
      "1"
    );

    mockOrderProducer.sendMarketDataRequest("testuser:1", marketDataRequestSub);

    await()
      .atMost(Duration.ofSeconds(5))
      .pollInterval(Duration.ofSeconds(1))
      .untilAsserted(() -> {
        assertThat(marketDataSubscriptionCatalog.getMarketDataRequests(symbol))
          .hasSize(1);
        assertThat(mockTradeListener.marketDataResponses).isEmpty();
      });

    MarketDataRequest marketDataRequestUnsub = new MarketDataRequest(
      "testuser",
      List.of(symbol),
      10,
      List.of(),
      MarketDataSubscriptionRequest.UNSUBSCRIBE,
      "1"
    );

    mockOrderProducer.sendMarketDataRequest(
      "testuser:1",
      marketDataRequestUnsub
    );

    await()
      .atMost(Duration.ofSeconds(5))
      .pollInterval(Duration.ofSeconds(1))
      .untilAsserted(() -> {
        assertThat(marketDataSubscriptionCatalog.getMarketDataRequests(symbol))
          .isEmpty();
        assertThat(mockTradeListener.marketDataResponses).isEmpty();
      });
  }

  @Test
  void testMarketDataRequestSubsribeDuplicate() throws InterruptedException {
    MarketDataRequest marketDataRequest = new MarketDataRequest(
      "testuser",
      List.of(symbol),
      10,
      List.of(),
      MarketDataSubscriptionRequest.SUBSCRIBE,
      "1"
    );

    mockOrderProducer.sendMarketDataRequest("testuser:1", marketDataRequest);

    await()
      .atMost(Duration.ofSeconds(5))
      .pollInterval(Duration.ofSeconds(1))
      .untilAsserted(() -> {
        assertThat(marketDataSubscriptionCatalog.getMarketDataRequests(symbol))
          .hasSize(1);
        assertThat(mockTradeListener.marketDataResponses).isEmpty();
      });

    mockOrderProducer.sendMarketDataRequest("testuser:1", marketDataRequest);

    await()
      .atMost(Duration.ofSeconds(5))
      .pollInterval(Duration.ofSeconds(1))
      .untilAsserted(() ->
        assertThat(mockTradeListener.marketDataRejecteds).hasSize(1)
      );
  }

  @Test
  void testMarketDataRequestUnkownSymbol() throws InterruptedException {
    MarketDataRequest marketDataRequest = new MarketDataRequest(
      "testuser",
      List.of("unknown"),
      10,
      List.of(),
      MarketDataSubscriptionRequest.SUBSCRIBE,
      "1"
    );

    mockOrderProducer.sendMarketDataRequest("testuser:1", marketDataRequest);

    await()
      .atMost(Duration.ofSeconds(5))
      .pollInterval(Duration.ofSeconds(1))
      .untilAsserted(() -> {
        assertThat(marketDataSubscriptionCatalog.getMarketDataRequests(symbol))
          .isEmpty();
        assertThat(mockTradeListener.marketDataResponses).isEmpty();
        assertThat(mockTradeListener.marketDataRejecteds).hasSize(1);
      });
  }
}
