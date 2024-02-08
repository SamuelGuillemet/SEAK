package io.seak.order_stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import io.lettuce.core.api.StatefulRedisConnection;
import io.micronaut.core.annotation.NonNull;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import io.micronaut.test.support.TestPropertyProvider;
import io.seak.avro.Order;
import io.seak.avro.Side;
import io.seak.avro.Type;
import io.seak.common.SymbolReader;
import io.seak.common.utils.KafkaTestContainer;
import io.seak.common.utils.RedisTestContainer;
import io.seak.order_stream.mocks.MockOrderListener;
import io.seak.order_stream.mocks.MockOrderProducer;
import jakarta.inject.Inject;
import java.time.Duration;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@MicronautTest(transactional = false)
@Testcontainers(disabledWithoutDocker = true)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class OrderStreamTest implements TestPropertyProvider {

  @Container
  static final KafkaTestContainer kafka = new KafkaTestContainer();

  @Container
  static final RedisTestContainer redis = new RedisTestContainer();

  @Inject
  SymbolReader symbolReader;

  @Override
  public @NonNull Map<String, String> getProperties() {
    if (!kafka.isRunning()) {
      kafka.start();
    }
    kafka.registerTopics("market-data.AAPL");
    if (!redis.isRunning()) {
      redis.start();
    }
    return Map.of(
      "kafka.bootstrap.servers",
      kafka.getBootstrapServers(),
      "kafka.schema.registry.url",
      kafka.getSchemaRegistryUrl(),
      "redis.uri",
      redis.getRedisUrl()
    );
  }

  @BeforeEach
  void setup(
    MockOrderListener mockOrderListener,
    StatefulRedisConnection<String, String> redisConnection
  ) {
    symbolReader.retrieveSymbols();
    mockOrderListener.acceptedOrders.clear();
    mockOrderListener.rejectedOrders.clear();
    mockOrderListener.orderBookRequests.clear();
    redisConnection.sync().flushall();
    // Register user
    redisConnection.sync().set("user:balance", "100000");
  }

  @Test
  void testOrderStreamBuyMarketOrder(
    MockOrderProducer mockOrderProducer,
    MockOrderListener mockOrderListener
  ) {
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
        assertThat(mockOrderListener.acceptedOrders).hasSize(1);
      });
  }

  @Test
  void testOrderStreamSellMarketOrder(
    MockOrderProducer mockOrderProducer,
    MockOrderListener mockOrderListener,
    StatefulRedisConnection<String, String> redisConnection
  ) {
    // Given
    redisConnection.sync().set("user:AAPL", "10");
    Order order = new Order(
      "user",
      "AAPL",
      7,
      Side.SELL,
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
        assertThat(mockOrderListener.acceptedOrders).hasSize(1);
        assertThat(redisConnection.sync().get("user:AAPL")).isEqualTo("3");
      });
  }

  @Test
  void testOrderStreamSellMarketOrderInsufficientStocks(
    MockOrderProducer mockOrderProducer,
    MockOrderListener mockOrderListener,
    StatefulRedisConnection<String, String> redisConnection
  ) {
    // Given
    redisConnection.sync().set("user:AAPL", "9");
    Order order = new Order(
      "user",
      "AAPL",
      10,
      Side.SELL,
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
        assertThat(mockOrderListener.acceptedOrders).isEmpty();
        assertThat(mockOrderListener.rejectedOrders).hasSize(1);
        assertThat(redisConnection.sync().get("user:AAPL")).isEqualTo("9");
      });
  }

  @Test
  void testOrderStreamBuyLimitOrder(
    MockOrderProducer mockOrderProducer,
    MockOrderListener mockOrderListener,
    StatefulRedisConnection<String, String> redisConnection
  ) {
    // Given
    Order order = new Order(
      "user",
      "AAPL",
      10,
      Side.BUY,
      Type.LIMIT,
      100.0,
      "1"
    );

    // When
    mockOrderProducer.sendOrder("user", order);

    // Then
    await()
      .pollInterval(Duration.ofSeconds(1))
      .atMost(Duration.ofSeconds(10))
      .untilAsserted(() -> {
        assertThat(mockOrderListener.acceptedOrders).isEmpty();
        assertThat(mockOrderListener.rejectedOrders).isEmpty();
        assertThat(redisConnection.sync().get("user:balance"))
          .isEqualTo("99000");
        assertThat(mockOrderListener.orderBookRequests).hasSize(1);
      });
  }

  @Test
  void testOrderStreamSellLimitOrder(
    MockOrderProducer mockOrderProducer,
    MockOrderListener mockOrderListener,
    StatefulRedisConnection<String, String> redisConnection
  ) {
    // Given
    redisConnection.sync().set("user:AAPL", "10");
    Order order = new Order(
      "user",
      "AAPL",
      7,
      Side.SELL,
      Type.LIMIT,
      100.0,
      "1"
    );

    // When
    mockOrderProducer.sendOrder("user", order);

    // Then
    await()
      .pollInterval(Duration.ofSeconds(1))
      .atMost(Duration.ofSeconds(10))
      .untilAsserted(() -> {
        assertThat(mockOrderListener.acceptedOrders).isEmpty();
        assertThat(mockOrderListener.rejectedOrders).isEmpty();
        assertThat(redisConnection.sync().get("user:AAPL")).isEqualTo("3");
        assertThat(mockOrderListener.orderBookRequests).hasSize(1);
      });
  }
}
