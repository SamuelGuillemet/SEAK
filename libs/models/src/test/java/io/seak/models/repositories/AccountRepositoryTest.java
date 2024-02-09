package io.seak.models.repositories;

import io.micronaut.context.annotation.Property;
import io.micronaut.test.annotation.TransactionMode;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import io.seak.models.domains.Account;
import io.seak.models.domains.Scope;
import jakarta.inject.Inject;
import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.testcontainers.junit.jupiter.Testcontainers;

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
class AccountRepositoryTest {

  @Inject
  private AccountRepository accountRepository;

  @Test
  void testFindByUsername() {
    // Create a sample user
    Account user = new Account(
      "testuser",
      "testpassword",
      "Test",
      "User",
      Scope.USER,
      true
    );
    accountRepository.save(user);

    // Call the repository method
    Optional<Account> foundUser = accountRepository.findByUsername("testuser");

    // Assert that the stock is found
    Assertions.assertTrue(foundUser.isPresent());
    Assertions.assertEquals(user.getId(), foundUser.get().getId());
  }
}
