package pfe_broker.models.services;

import jakarta.inject.Singleton;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import pfe_broker.models.domains.Account;
import pfe_broker.models.repositories.AccountRepository;

@Singleton
public class UserAuthenticationService implements PasswordEncoder {

  private final PasswordEncoder passwordEncoder;
  private final AccountRepository userRepository;

  public UserAuthenticationService(AccountRepository userRepository) {
    this.passwordEncoder = new BCryptPasswordEncoder();
    this.userRepository = userRepository;
  }

  @Override
  public String encode(CharSequence rawPassword) {
    return passwordEncoder.encode(rawPassword);
  }

  @Override
  public boolean matches(CharSequence rawPassword, String encodedPassword) {
    return passwordEncoder.matches(rawPassword, encodedPassword);
  }

  public boolean userAuthentication(String username, String password) {
    Account user = userRepository.findByUsername(username).orElse(null);
    if (user == null) {
      return false;
    }
    if (!user.isEnabled()) {
      return false;
    }
    return matches(password, user.getPassword());
  }
}
