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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import pfe_broker.avro.Order;
import pfe_broker.avro.Side;
import pfe_broker.avro.Trade;
import pfe_broker.common.utils.KafkaTestContainer;
import pfe_broker.quickfix_server.mocks.MockReportProducer;

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
  static {
    MainApplication.setProperties();
  }

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

  @Test
  public void testReportListener(
    MockReportProducer mockReportProducer,
    ServerApplication serverApplication
  ) {
    Order order = new Order("testuser", "AAPL", 10, Side.BUY);
    Trade trade = new Trade(order, "APPL", 100.0, 10);

    mockReportProducer.sendTrade("testuser:1", trade);

    await()
      .atMost(5, TimeUnit.SECONDS)
      .untilAsserted(() -> {
        assertEquals(1, serverApplication.getExecutionKey());
      });
  }
}
