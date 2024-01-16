package pfe_broker.trade_stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import io.lettuce.core.api.StatefulRedisConnection;
import io.micronaut.core.annotation.NonNull;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import io.micronaut.test.support.TestPropertyProvider;
import java.time.Duration;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import pfe_broker.avro.Order;
import pfe_broker.avro.Side;
import pfe_broker.avro.Trade;
import pfe_broker.avro.Type;
import pfe_broker.common.utils.KafkaTestContainer;
import pfe_broker.common.utils.RedisTestContainer;
import pfe_broker.trade_stream.mocks.MockListener;
import pfe_broker.trade_stream.mocks.MockTradeProducer;

@MicronautTest(transactional = false)
@Testcontainers(disabledWithoutDocker = true)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TradeStreamTest implements TestPropertyProvider {

  @Container
  static final KafkaTestContainer kafka = new KafkaTestContainer();

  @Container
  static final RedisTestContainer redis = new RedisTestContainer();

  @Override
  public @NonNull Map<String, String> getProperties() {
    if (!kafka.isRunning()) {
      kafka.start();
    }
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
    MockListener mockListener,
    StatefulRedisConnection<String, String> redisConnection
  ) {
    mockListener.acceptedTrades.clear();
    mockListener.rejectedOrders.clear();
    redisConnection.sync().flushall();
  }

  @Test
  void testTradeStreamBuyMarketOrder(
    MockListener mockListener,
    MockTradeProducer mockTradeProducer,
    StatefulRedisConnection<String, String> redisConnection
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
    Trade trade = new Trade(order, "APPL", 100.0, 10);
    redisConnection.sync().set("user:balance", "10000");

    // When
    mockTradeProducer.sendTrade("user", trade);

    // Then
    await()
      .atMost(Duration.ofSeconds(5))
      .untilAsserted(() -> {
        assertThat(mockListener.acceptedTrades).hasSize(1);
        assertThat(mockListener.rejectedOrders).isEmpty();
        assertThat(redisConnection.sync().get("user:balance"))
          .isEqualTo("9000");
        assertThat(redisConnection.sync().get("user:APPL")).isEqualTo("10");
      });
  }

  @Test
  void testTradeStreamSellMarketOrder(
    MockListener mockListener,
    MockTradeProducer mockTradeProducer,
    StatefulRedisConnection<String, String> redisConnection
  ) {
    // Given
    Order order = new Order(
      "user",
      "AAPL",
      10,
      Side.SELL,
      Type.MARKET,
      null,
      "1"
    );
    Trade trade = new Trade(order, "APPL", 100.0, 10);
    redisConnection.sync().set("user:balance", "10000");

    // When
    mockTradeProducer.sendTrade("user", trade);

    // Then
    await()
      .atMost(Duration.ofSeconds(5))
      .untilAsserted(() -> {
        assertThat(mockListener.acceptedTrades).hasSize(1);
        assertThat(mockListener.rejectedOrders).isEmpty();
        assertThat(redisConnection.sync().get("user:balance"))
          .isEqualTo("11000");
      });
  }

  @Test
  void testTradeStreamBuyMarketOrderInsufficientFunds(
    MockListener mockListener,
    MockTradeProducer mockTradeProducer,
    StatefulRedisConnection<String, String> redisConnection
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
    Trade trade = new Trade(order, "APPL", 100.0, 10);
    redisConnection.sync().set("user:balance", "100");

    // When
    mockTradeProducer.sendTrade("user", trade);

    // Then
    await()
      .atMost(Duration.ofSeconds(5))
      .untilAsserted(() -> {
        assertThat(mockListener.acceptedTrades).isEmpty();
        assertThat(mockListener.rejectedOrders).hasSize(1);
        assertThat(redisConnection.sync().get("user:balance")).isEqualTo("100");
        assertThat(redisConnection.sync().get("user:APPL")).isNull();
      });
  }

  @Test
  void testTradeStreamBuyLimitOrder(
    MockListener mockListener,
    MockTradeProducer mockTradeProducer,
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
    Trade trade = new Trade(order, "APPL", 100.0, 10);
    redisConnection.sync().set("user:balance", "10000");

    // When
    mockTradeProducer.sendTrade("user", trade);

    // Then
    await()
      .atMost(Duration.ofSeconds(5))
      .untilAsserted(() -> {
        assertThat(mockListener.acceptedTrades).hasSize(1);
        assertThat(mockListener.rejectedOrders).isEmpty();
        assertThat(redisConnection.sync().get("user:balance"))
          .isEqualTo("10000");
        assertThat(redisConnection.sync().get("user:APPL")).isEqualTo("10");
      });
  }

  @Test
  void testTradeStreamSellLimitOrder(
    MockListener mockListener,
    MockTradeProducer mockTradeProducer,
    StatefulRedisConnection<String, String> redisConnection
  ) {
    // Given
    Order order = new Order(
      "user",
      "AAPL",
      10,
      Side.SELL,
      Type.LIMIT,
      100.0,
      "1"
    );
    Trade trade = new Trade(order, "APPL", 100.0, 10);
    redisConnection.sync().set("user:balance", "10000");

    // When
    mockTradeProducer.sendTrade("user", trade);

    // Then
    await()
      .atMost(Duration.ofSeconds(5))
      .untilAsserted(() -> {
        assertThat(mockListener.acceptedTrades).hasSize(1);
        assertThat(mockListener.rejectedOrders).isEmpty();
        assertThat(redisConnection.sync().get("user:balance"))
          .isEqualTo("11000");
      });
  }
}
