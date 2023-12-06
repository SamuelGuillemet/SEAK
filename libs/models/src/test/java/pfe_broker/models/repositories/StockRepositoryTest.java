package pfe_broker.models.repositories;

import io.micronaut.context.annotation.Property;
import io.micronaut.test.annotation.TransactionMode;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.testcontainers.junit.jupiter.Testcontainers;
import pfe_broker.models.domains.Stock;
import pfe_broker.models.domains.User;

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
class StockRepositoryTest {
  static {
    System.setProperty("micronaut.config.files", "classpath:data.yml");
  }

  @Inject
  private UserRepository userRepository;

  @Inject
  private StockRepository stockRepository;

  User user;

  @BeforeAll
  void setup() {
    user = new User("testuser", "testpassword", 1000.0);
    userRepository.save(user);
  }

  @Test
  void testFindBySymbolAndUser() {
    // Create a sample stock
    Stock stock = new Stock("AAPL", 10, user);
    stockRepository.save(stock);

    // Call the repository method
    Optional<Stock> foundStock = stockRepository.findBySymbolAndUser(
      "AAPL",
      user
    );

    // Assert that the stock is found
    Assertions.assertTrue(foundStock.isPresent());
    Assertions.assertEquals(stock.getId(), foundStock.get().getId());
  }

  @Test
  void testUpdateQuantity() {
    // Create a sample stock
    Stock stock = new Stock("GOOGL", 10, user);
    Long id = stockRepository.save(stock).getId();

    // Call the repository method
    stockRepository.updateQuantity(id, 20);

    // Refresh the stock
    Optional<Stock> refreshStock = stockRepository.findById(id);

    // Assert that the quantity is updated
    Assertions.assertTrue(refreshStock.isPresent());
    Assertions.assertEquals(20, refreshStock.get().getQuantity());
  }
}
