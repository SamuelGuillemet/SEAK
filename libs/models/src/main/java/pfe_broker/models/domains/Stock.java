package pfe_broker.models.domains;

import io.micronaut.core.annotation.NonNull;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@NoArgsConstructor
@Entity
@Table(
  name = "stock",
  uniqueConstraints = {
    @UniqueConstraint(columnNames = { "symbol", "account_id" }),
  }
)
public class Stock {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private long id;

  @Column(nullable = false)
  private String symbol;

  @Column(nullable = false)
  private int quantity;

  @ManyToOne(targetEntity = Account.class, fetch = FetchType.EAGER)
  @ToString.Exclude
  private Account account;

  public Stock(@NonNull String symbol, int quantity, @NonNull Account account) {
    if (quantity < 0) {
      throw new IllegalArgumentException("Quantity cannot be negative");
    }
    this.symbol = symbol;
    this.quantity = quantity;
    this.account = account;
  }
}
