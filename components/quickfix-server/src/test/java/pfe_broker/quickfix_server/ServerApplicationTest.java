package pfe_broker.quickfix_server;

import static org.junit.Assert.assertThat;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import io.micronaut.context.annotation.Property;
import io.micronaut.core.annotation.NonNull;
import io.micronaut.test.annotation.TransactionMode;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import io.micronaut.test.support.TestPropertyProvider;
import jakarta.inject.Inject;
import java.time.Duration;
import java.util.Map;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import pfe_broker.common.utils.KafkaTestContainer;
import pfe_broker.models.domains.User;
import pfe_broker.models.repositories.UserRepository;
import pfe_broker.quickfix_server.mocks.MockOrderListener;
import quickfix.SessionID;
import quickfix.field.SenderCompID;
import quickfix.fix42.NewOrderSingle;

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
  static {
    MainApplication.setProperties();
  }

  @Container
  static final KafkaTestContainer kafka = new KafkaTestContainer();

  @Inject
  private ServerApplication serverApplication;

  @Inject
  private UserRepository userRepository;

  private User user;

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

  @BeforeAll
  void setup() {
    user = new User("testuser", "testpassword", 1000.0);
    userRepository.save(user);
  }

  @Test
  public void testOnMessageNewOrderSingle(MockOrderListener mockOrderListener) {
    NewOrderSingle newOrderSingle = new NewOrderSingle();
    newOrderSingle.set(new quickfix.field.Symbol("AAPL"));
    newOrderSingle.set(new quickfix.field.OrderQty(10));
    newOrderSingle.set(new quickfix.field.Side(quickfix.field.Side.BUY));
    newOrderSingle.getHeader().setString(SenderCompID.FIELD, "user1");
    try {
      serverApplication.onMessage(newOrderSingle, new SessionID("FIX.4.2", "user1", "SERVER"));

      await()
      .pollInterval(Duration.ofSeconds(1))
      .atMost(Duration.ofSeconds(10))
      .untilAsserted(() -> {
        assertThat(mockOrderListener.receivedOrders).hasSize(1);
      });

    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
