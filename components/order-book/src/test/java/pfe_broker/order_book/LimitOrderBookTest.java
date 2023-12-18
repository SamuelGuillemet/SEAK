package pfe_broker.order_book;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import io.micronaut.core.annotation.NonNull;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import io.micronaut.test.support.TestPropertyProvider;
import jakarta.inject.Inject;
import java.time.Duration;
import java.util.Map;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import pfe_broker.avro.MarketData;
import pfe_broker.avro.Order;
import pfe_broker.avro.Side;
import pfe_broker.avro.Type;
import pfe_broker.common.utils.KafkaTestContainer;
import pfe_broker.order_book.mocks.MockMarketDataProducer;
import pfe_broker.order_book.mocks.MockOrderProducer;
import pfe_broker.order_book.mocks.MockTradeListener;

@MicronautTest(transactional = false)
@Testcontainers(disabledWithoutDocker = true)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class LimitOrderBookTest implements TestPropertyProvider {

  @Container
  static final KafkaTestContainer kafka = new KafkaTestContainer();

  @Inject
  OrderBookCatalog orderBooks;

  private final Double lowValue = 90.0;
  private final Double highValue = 120.0;

  @Inject
  MockTradeListener mockTradeListener;

  @Inject
  MockMarketDataProducer mockMarketDataProducer;

  @Inject
  MockOrderProducer mockOrderProducer;

  @Override
  public @NonNull Map<String, String> getProperties() {
    if (!kafka.isRunning()) {
      kafka.start();
    }
    return Map.of(
      "kafka.bootstrap.servers",
      kafka.getBootstrapServers(),
      "kafka.schema.registry.url",
      kafka.getSchemaRegistryUrl()
    );
  }

  @BeforeEach
  void reset(MockTradeListener mockTradeListener) {
    mockTradeListener.trades.clear();
  }

  @Test
  void testAddOrder() {
    String symbol = "APPL";
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

    mockOrderProducer.sendOrder("user:1", order);

    await()
      .atMost(Duration.ofSeconds(5))
      .pollInterval(Duration.ofSeconds(1))
      .untilAsserted(() -> {
        LimitOrderBook orderBook = orderBooks.getOrderBook(symbol);
        assertThat(orderBook).isNotNull();
        assertThat(orderBook.getBuyOrders().size()).isEqualTo(1);
        assertThat(orderBook.getSellOrders().size()).isEqualTo(0);
      });
    System.out.println(orderBooks.getOrderBook(symbol));
  }
}
