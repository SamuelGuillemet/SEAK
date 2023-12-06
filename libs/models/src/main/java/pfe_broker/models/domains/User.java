package pfe_broker.models.domains;

import io.micronaut.core.annotation.NonNull;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Entity
@Table(name = "users")
public class User {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private long id;

  @Column(nullable = false, unique = true)
  private String username;

  @Column(nullable = false)
  private String password;

  @Column(nullable = false)
  private Double balance;

  @OneToMany(
    mappedBy = "user",
    targetEntity = Stock.class,
    fetch = FetchType.EAGER,
    cascade = CascadeType.ALL
  )
  private List<Stock> stocks = new ArrayList<>();

  public User(
    @NonNull String username,
    @NonNull String password,
    Double balance
  ) {
    if (balance < 0) {
      throw new IllegalArgumentException("Balance cannot be negative");
    }
    this.username = username;
    this.password = password;
    this.balance = balance;
  }

  public boolean checkPassword(String password) {
    return this.password.equals(password);
  }
}
