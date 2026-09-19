package footballmarket.services.impl;

import footballmarket.models.User;
import footballmarket.repositories.UserRepository;
import footballmarket.security.JWTProvider;
import footballmarket.services.AuthenticationService;
import footballmarket.services.exceptions.EmailAlreadyRegisteredException;
import footballmarket.services.exceptions.InvalidCredentialsException;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
/** Implementación transaccional del registro y autenticación. */
public class AuthenticationServiceImpl implements AuthenticationService {

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;
  private final JWTProvider jwtProvider;

  @Override
  /** {@inheritDoc} */
  public void register(User user) {
    String normalizedEmail = user.getEmail().toLowerCase(Locale.ROOT);

    if (this.userRepository.existsByEmail(normalizedEmail)) {
      throw new EmailAlreadyRegisteredException("El email ya esta registrado.");
    }

    user.setEmail(normalizedEmail);
    user.setPassword(this.passwordEncoder.encode(user.getPassword()));

    this.userRepository.save(user);
  }

  @Override
  /** {@inheritDoc} */
  public String login(String email, String password) {
    String normalizedEmail = email.toLowerCase(Locale.ROOT);

    User user =
        this.userRepository
            .findByEmail(normalizedEmail)
            .orElseThrow(() -> new InvalidCredentialsException("Credenciales invalidas."));

    if (!this.passwordEncoder.matches(password, user.getPassword())) {
      throw new InvalidCredentialsException("Credenciales invalidas.");
    }

    return this.jwtProvider.generateToken(user.getEmail());
  }
}
