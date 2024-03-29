package io.seak.order_book.order_book;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import io.lettuce.core.api.StatefulRedisConnection;
import io.micronaut.core.annotation.NonNull;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import io.micronaut.test.support.TestPropertyProvider;
import io.seak.avro.Order;
import io.seak.avro.OrderBookRequest;
import io.seak.avro.OrderBookRequestType;
import io.seak.avro.Side;
import io.seak.avro.Type;
import io.seak.common.utils.KafkaTestContainer;
import io.seak.common.utils.RedisTestContainer;
import io.seak.common.utils.mocks.MockMarketDataProducer;
import io.seak.order_book.mocks.MockProducer;
import io.seak.order_book.mocks.MockTradeListener;
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
class LimitOrderBookOperationsTest implements TestPropertyProvider {

  @Container
  static final KafkaTestContainer kafka = new KafkaTestContainer();

  @Container
  static final RedisTestContainer redis = new RedisTestContainer();

  @Inject
  OrderBookCatalog orderBooks;

  private final String symbol = "AAPL";

  @Inject
  MockTradeListener mockTradeListener;

  @Inject
  MockMarketDataProducer mockMarketDataProducer;

  @Inject
  MockProducer mockOrderProducer;

  @Override
  public @NonNull Map<String, String> getProperties() {
    if (!kafka.isRunning()) {
      kafka.start();
    }
    kafka.registerTopics("order-book-request");
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
  void reset() {
    mockTradeListener.trades.clear();
    mockTradeListener.orderBookRequests.clear();
    orderBooks.clear();
  }

  @Test
  void testAddBuyOrder() {
    assertThat(orderBooks.getOrderBook(symbol)).isNull();

    Order order = new Order(
      "user",
      symbol,
      10,
      Side.BUY,
      Type.LIMIT,
      80.0,
      "1"
    );

    OrderBookRequest orderBookRequest = new OrderBookRequest(
      OrderBookRequestType.NEW,
      order,
      null
    );

    mockOrderProducer.sendOrderBookRequest("user:1", orderBookRequest);

    await()
      .atMost(Duration.ofSeconds(5))
      .pollInterval(Duration.ofSeconds(1))
      .untilAsserted(() -> {
        LimitOrderBook orderBook = orderBooks.getOrderBook(symbol);
        assertThat(orderBook).isNotNull();
        assertThat(orderBook.getBuyOrders()).hasSize(1);
        assertThat(orderBook.getSellOrders()).isEmpty();
      });
  }

  @Test
  void testReplaceBuyOrder(
    StatefulRedisConnection<String, String> redisConnection
  ) {
    assertThat(orderBooks.getOrderBook(symbol)).isNull();

    redisConnection.sync().set("user:balance", "800");

    Order order = new Order(
      "user",
      symbol,
      10,
      Side.BUY,
      Type.LIMIT,
      80.0,
      "1"
    );

    OrderBookRequest orderBookRequest = new OrderBookRequest(
      OrderBookRequestType.NEW,
      order,
      null
    );

    mockOrderProducer.sendOrderBookRequest("user:1", orderBookRequest);

    await()
      .atMost(Duration.ofSeconds(5))
      .pollInterval(Duration.ofSeconds(1))
      .untilAsserted(() -> {
        LimitOrderBook orderBook = orderBooks.getOrderBook(symbol);
        assertThat(orderBook).isNotNull();
        assertThat(orderBook.getBuyOrders()).hasSize(1);
        assertThat(orderBook.getSellOrders()).isEmpty();
      });

    Order newOrder = new Order(
      "user",
      symbol,
      20,
      Side.BUY,
      Type.LIMIT,
      80.0,
      "2"
    );

    OrderBookRequest newOrderBookRequest = new OrderBookRequest(
      OrderBookRequestType.REPLACE,
      newOrder,
      "1"
    );

    mockOrderProducer.sendOrderBookRequest("user:1", newOrderBookRequest);

    await()
      .atMost(Duration.ofSeconds(5))
      .pollInterval(Duration.ofSeconds(1))
      .untilAsserted(() -> {
        LimitOrderBook orderBook = orderBooks.getOrderBook(symbol);
        assertThat(orderBook).isNotNull();
        assertThat(orderBook.getBuyOrders()).hasSize(1);
        assertThat(orderBook.getSellOrders()).isEmpty();
        assertThat(redisConnection.sync().get("user:balance")).isEqualTo("0");
      });
  }

  @Test
  void testCancelBuyOrder(
    StatefulRedisConnection<String, String> redisConnection
  ) {
    assertThat(orderBooks.getOrderBook(symbol)).isNull();

    redisConnection.sync().set("user:balance", "800");

    Order order = new Order(
      "user",
      symbol,
      10,
      Side.BUY,
      Type.LIMIT,
      80.0,
      "1"
    );

    OrderBookRequest orderBookRequest = new OrderBookRequest(
      OrderBookRequestType.NEW,
      order,
      null
    );

    mockOrderProducer.sendOrderBookRequest("user:1", orderBookRequest);

    await()
      .atMost(Duration.ofSeconds(5))
      .pollInterval(Duration.ofSeconds(1))
      .untilAsserted(() -> {
        LimitOrderBook orderBook = orderBooks.getOrderBook(symbol);
        assertThat(orderBook).isNotNull();
        assertThat(orderBook.getBuyOrders()).hasSize(1);
        assertThat(orderBook.getSellOrders()).isEmpty();
      });

    Order cancelOrder = new Order(
      "user",
      symbol,
      0,
      Side.BUY,
      Type.LIMIT,
      0.0,
      "2"
    );

    OrderBookRequest cancelOrderBookRequest = new OrderBookRequest(
      OrderBookRequestType.CANCEL,
      cancelOrder,
      "1"
    );

    mockOrderProducer.sendOrderBookRequest("user:1", cancelOrderBookRequest);

    await()
      .atMost(Duration.ofSeconds(5))
      .pollInterval(Duration.ofSeconds(1))
      .untilAsserted(() -> {
        LimitOrderBook orderBook = orderBooks.getOrderBook(symbol);
        assertThat(orderBook).isNotNull();
        assertThat(orderBook.getBuyOrders()).isEmpty();
        assertThat(orderBook.getSellOrders()).isEmpty();
        assertThat(redisConnection.sync().get("user:balance"))
          .isEqualTo("1600");
      });
  }

  @Test
  void testAddSellOrder() {
    assertThat(orderBooks.getOrderBook(symbol)).isNull();

    Order order = new Order(
      "user",
      symbol,
      10,
      Side.SELL,
      Type.LIMIT,
      80.0,
      "1"
    );

    OrderBookRequest orderBookRequest = new OrderBookRequest(
      OrderBookRequestType.NEW,
      order,
      null
    );

    mockOrderProducer.sendOrderBookRequest("user:1", orderBookRequest);

    await()
      .atMost(Duration.ofSeconds(5))
      .pollInterval(Duration.ofSeconds(1))
      .untilAsserted(() -> {
        LimitOrderBook orderBook = orderBooks.getOrderBook(symbol);
        assertThat(orderBook).isNotNull();
        assertThat(orderBook.getBuyOrders()).isEmpty();
        assertThat(orderBook.getSellOrders()).hasSize(1);
      });
  }

  @Test
  void testReplaceSellOrder(
    StatefulRedisConnection<String, String> redisConnection
  ) {
    assertThat(orderBooks.getOrderBook(symbol)).isNull();

    redisConnection.sync().set("user:AAPL", "20");

    Order order = new Order(
      "user",
      symbol,
      10,
      Side.SELL,
      Type.LIMIT,
      80.0,
      "1"
    );

    OrderBookRequest orderBookRequest = new OrderBookRequest(
      OrderBookRequestType.NEW,
      order,
      null
    );

    mockOrderProducer.sendOrderBookRequest("user:1", orderBookRequest);

    await()
      .atMost(Duration.ofSeconds(5))
      .pollInterval(Duration.ofSeconds(1))
      .untilAsserted(() -> {
        LimitOrderBook orderBook = orderBooks.getOrderBook(symbol);
        assertThat(orderBook).isNotNull();
        assertThat(orderBook.getBuyOrders()).isEmpty();
        assertThat(orderBook.getSellOrders()).hasSize(1);
      });

    Order newOrder = new Order(
      "user",
      symbol,
      20,
      Side.SELL,
      Type.LIMIT,
      80.0,
      "2"
    );

    OrderBookRequest newOrderBookRequest = new OrderBookRequest(
      OrderBookRequestType.REPLACE,
      newOrder,
      "1"
    );

    mockOrderProducer.sendOrderBookRequest("user:1", newOrderBookRequest);

    await()
      .atMost(Duration.ofSeconds(5))
      .pollInterval(Duration.ofSeconds(1))
      .untilAsserted(() -> {
        LimitOrderBook orderBook = orderBooks.getOrderBook(symbol);
        assertThat(orderBook).isNotNull();
        assertThat(orderBook.getBuyOrders()).isEmpty();
        assertThat(orderBook.getSellOrders()).hasSize(1);
        assertThat(redisConnection.sync().get("user:AAPL")).isEqualTo("10");
      });
  }

  @Test
  void testCancelSellOrder(
    StatefulRedisConnection<String, String> redisConnection
  ) {
    assertThat(orderBooks.getOrderBook(symbol)).isNull();

    redisConnection.sync().set("user:AAPL", "20");

    Order order = new Order(
      "user",
      symbol,
      10,
      Side.SELL,
      Type.LIMIT,
      80.0,
      "1"
    );

    OrderBookRequest orderBookRequest = new OrderBookRequest(
      OrderBookRequestType.NEW,
      order,
      null
    );

    mockOrderProducer.sendOrderBookRequest("user:1", orderBookRequest);

    await()
      .atMost(Duration.ofSeconds(5))
      .pollInterval(Duration.ofSeconds(1))
      .untilAsserted(() -> {
        LimitOrderBook orderBook = orderBooks.getOrderBook(symbol);
        assertThat(orderBook).isNotNull();
        assertThat(orderBook.getBuyOrders()).isEmpty();
        assertThat(orderBook.getSellOrders()).hasSize(1);
      });

    Order cancelOrder = new Order(
      "user",
      symbol,
      0,
      Side.SELL,
      Type.LIMIT,
      0.0,
      "2"
    );

    OrderBookRequest cancelOrderBookRequest = new OrderBookRequest(
      OrderBookRequestType.CANCEL,
      cancelOrder,
      "1"
    );

    mockOrderProducer.sendOrderBookRequest("user:1", cancelOrderBookRequest);

    await()
      .atMost(Duration.ofSeconds(5))
      .pollInterval(Duration.ofSeconds(1))
      .untilAsserted(() -> {
        LimitOrderBook orderBook = orderBooks.getOrderBook(symbol);
        assertThat(orderBook).isNotNull();
        assertThat(orderBook.getBuyOrders()).isEmpty();
        assertThat(orderBook.getSellOrders()).isEmpty();
        assertThat(redisConnection.sync().get("user:AAPL")).isEqualTo("30");
      });
  }

  @Test
  void testOrderBookRejected(
    StatefulRedisConnection<String, String> redisConnection
  ) {
    assertThat(orderBooks.getOrderBook(symbol)).isNull();

    redisConnection.sync().set("user:AAPL", "20");

    Order order = new Order(
      "user",
      symbol,
      10,
      Side.SELL,
      Type.LIMIT,
      80.0,
      "2"
    );

    OrderBookRequest orderBookRequest = new OrderBookRequest(
      OrderBookRequestType.REPLACE,
      order,
      "1"
    );

    mockOrderProducer.sendOrderBookRequest("user:1", orderBookRequest);

    await()
      .atMost(Duration.ofSeconds(5))
      .pollInterval(Duration.ofSeconds(1))
      .untilAsserted(() -> {
        LimitOrderBook orderBook = orderBooks.getOrderBook(symbol);
        assertThat(orderBook).isNotNull();
        assertThat(orderBook.getBuyOrders()).isEmpty();
        assertThat(orderBook.getSellOrders()).isEmpty();
        assertThat(mockTradeListener.orderBookRequests).hasSize(1);
      });
  }
}
