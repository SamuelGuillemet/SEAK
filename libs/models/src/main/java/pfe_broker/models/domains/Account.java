package pfe_broker.models.domains;

import io.micronaut.core.annotation.NonNull;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
@Table(name = "account")
public class Account {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private long id;

  @Column(nullable = false, unique = true)
  private String username;

  @Column(nullable = false, name = "first_name")
  private String firstName;

  @Column(nullable = false, name = "last_name")
  private String lastName;

  @Enumerated(EnumType.STRING)
  private Scope scope;

  @Column(nullable = false)
  private boolean enabled;

  @Column(nullable = false)
  private String password;

  @Column(nullable = false)
  private Double balance;

  @OneToMany(
    mappedBy = "account",
    targetEntity = Stock.class,
    fetch = FetchType.EAGER,
    cascade = CascadeType.ALL
  )
  private List<Stock> stocks = new ArrayList<>();

  public Account(
    @NonNull String username,
    @NonNull String password,
    @NonNull String firstName,
    @NonNull String lastName,
    @NonNull Scope scope,
    @NonNull boolean enabled,
    Double balance
  ) {
    if (balance < 0) {
      throw new IllegalArgumentException("Balance cannot be negative");
    }
    this.username = username;
    this.password = password;
    this.firstName = firstName;
    this.lastName = lastName;
    this.scope = scope;
    this.enabled = enabled;
    this.balance = balance;
  }
}