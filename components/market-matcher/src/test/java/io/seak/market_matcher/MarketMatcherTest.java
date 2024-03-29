package io.seak.market_matcher;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import io.micronaut.core.annotation.NonNull;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import io.micronaut.test.support.TestPropertyProvider;
import io.seak.avro.MarketData;
import io.seak.avro.Order;
import io.seak.avro.Side;
import io.seak.avro.Type;
import io.seak.common.SymbolReader;
import io.seak.common.utils.KafkaTestContainer;
import io.seak.common.utils.mocks.MockMarketDataProducer;
import io.seak.market_matcher.mocks.MockOrderProducer;
import io.seak.market_matcher.mocks.MockTradeListener;
import jakarta.inject.Inject;
import java.time.Duration;
import java.util.Map;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@MicronautTest(transactional = false)
@Testcontainers(disabledWithoutDocker = true)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MarketMatcherTest implements TestPropertyProvider {

  @Container
  static final KafkaTestContainer kafka = new KafkaTestContainer();

  @Inject
  SymbolReader symbolReader;

  private final float closeValue = 100.0f;

  @Override
  public @NonNull Map<String, String> getProperties() {
    if (!kafka.isRunning()) {
      kafka.start();
    }
    kafka.registerTopics("market-data.AAPL", "accepted-orders", "trades");
    return Map.of(
      "kafka.bootstrap.servers",
      kafka.getBootstrapServers(),
      "kafka.schema.registry.url",
      kafka.getSchemaRegistryUrl()
    );
  }

  @BeforeAll
  void setup(
    MockMarketDataProducer mockMarketDataProducer,
    MockOrderProducer mockOrderProducer
  ) {
    mockMarketDataProducer.sendMarketData(
      MarketData
        .newBuilder()
        .setClose(closeValue)
        .setHigh(110.0f)
        .setLow(90.0f)
        .setOpen(95.0f)
        .setVolume(10)
        .build()
    );
  }

  @BeforeEach
  void reset(MockTradeListener mockTradeListener) throws InterruptedException {
    symbolReader.retrieveSymbols();
    mockTradeListener.trades.clear();
    mockTradeListener.rejectedOrders.clear();
  }

  @Test
  void testOrderConsumer(
    MockTradeListener mockTradeListener,
    MockOrderProducer mockOrderProducer
  ) {
    // Assert that AAPL is in the list of symbols of the order consumer
    assertThat(symbolReader.getSymbolsCached()).contains("AAPL");
    // Given
    Order order = new Order(
      "user",
      "AAPL",
      10,
      Side.BUY,
      Type.MARKET,
      null,
      "1"
    );

    // When
    mockOrderProducer.sendOrder("user", order);

    // Then
    await()
      .pollInterval(Duration.ofSeconds(1))
      .atMost(Duration.ofSeconds(10))
      .untilAsserted(() -> {
        assertThat(mockTradeListener.trades).hasSize(1);
        assertThat(mockTradeListener.trades.get(0).getOrder()).isEqualTo(order);
        assertThat(mockTradeListener.trades.get(0).getPrice())
          .isEqualTo(closeValue);
      });
  }

  @Test
  void testOrderConsumerWithUnknownSymbol(
    MockTradeListener mockTradeListener,
    MockOrderProducer mockOrderProducer
  ) {
    // Given
    Order order = new Order(
      "user",
      "UNKNOWN",
      10,
      Side.BUY,
      Type.MARKET,
      null,
      "1"
    );

    // When
    mockOrderProducer.sendOrder("user", order);

    // Then
    await()
      .pollInterval(Duration.ofSeconds(1))
      .atMost(Duration.ofSeconds(5))
      .untilAsserted(() -> {
        assertThat(mockTradeListener.trades).isEmpty();
        assertThat(mockTradeListener.rejectedOrders).hasSize(1);
      });
  }
}
