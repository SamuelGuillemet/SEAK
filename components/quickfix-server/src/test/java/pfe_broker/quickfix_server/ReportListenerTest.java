package pfe_broker.quickfix_server;

import io.micronaut.context.annotation.Property;
import io.micronaut.core.annotation.NonNull;
import io.micronaut.test.annotation.TransactionMode;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import io.micronaut.test.support.TestPropertyProvider;
import jakarta.inject.Inject;
import java.util.Map;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import pfe_broker.avro.Order;
import pfe_broker.avro.Side;
import pfe_broker.avro.Trade;
import pfe_broker.common.utils.KafkaTestContainer;
import pfe_broker.quickfix_server.mocks.MockReportProducer;
import quickfix.SessionID;
import quickfix.fix42.ExecutionReport;

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
    @Inject
      private ServerApplication serverApplication;

    @Captor
    private ArgumentCaptor<ExecutionReport> orderCaptor;

    @Captor
    private ArgumentCaptor<SessionID> keyCaptor;

    @BeforeAll
    void setup() {
      MockitoAnnotations.openMocks(this);
    }

    @Test
    public void testReportListener(MockReportProducer mockReportProducer) {
      Order order = new Order("testuser", "AAPL", 10, Side.BUY);
      Trade trade = new Trade(order, "APPL", 100.0, 10);
      mockReportProducer.sendTrade("testuser", trade);
      ArgumentCaptor<ExecutionReport> orderCaptor = ArgumentCaptor.forClass(ExecutionReport.class);
      ArgumentCaptor<SessionID> keyCaptor = ArgumentCaptor.forClass(SessionID.class);
      try {
        Mockito.verify(serverApplication).sendExecutionReport(orderCaptor.capture(), keyCaptor.capture());
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
}
