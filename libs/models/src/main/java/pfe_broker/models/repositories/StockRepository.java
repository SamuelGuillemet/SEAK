package pfe_broker.models.repositories;

import io.micronaut.data.annotation.Id;
import io.micronaut.data.annotation.Repository;
import io.micronaut.data.jpa.repository.JpaRepository;
import java.util.Optional;
import pfe_broker.models.domains.Stock;
import pfe_broker.models.domains.User;

@Repository
public interface StockRepository extends JpaRepository<Stock, Long> {
  Optional<Stock> findBySymbolAndUser(String symbol, User user);

  void updateQuantity(@Id long id, int quantity);
}
