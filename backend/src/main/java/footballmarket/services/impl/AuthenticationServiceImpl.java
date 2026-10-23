package footballmarket.services.impl;

import footballmarket.models.User;
import footballmarket.models.UserCredit;
import footballmarket.repositories.UserCreditRepository;
import footballmarket.repositories.UserRepository;
import footballmarket.security.JWTProvider;
import footballmarket.services.AuthenticationService;
import footballmarket.services.exceptions.CurrentUserNotFoundException;
import footballmarket.services.exceptions.EmailAlreadyRegisteredException;
import footballmarket.services.exceptions.InvalidCredentialsException;
import footballmarket.services.exceptions.UsernameAlreadyRegisteredException;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Implementación transaccional del registro y autenticación. */
@Service
@Transactional
@RequiredArgsConstructor
public class AuthenticationServiceImpl implements AuthenticationService {

  private final UserRepository userRepository;
  private final UserCreditRepository userCreditRepository;
  private final PasswordEncoder passwordEncoder;
  private final JWTProvider jwtProvider;

  /** {@inheritDoc} */
  @Override
  @Transactional(readOnly = true)
  public User getCurrentUser(String subjectEmail) {
    if (subjectEmail == null || subjectEmail.isBlank()) {
      throw new CurrentUserNotFoundException();
    }
    return this.userRepository
        .findByEmail(subjectEmail.toLowerCase(Locale.ROOT))
        .orElseThrow(CurrentUserNotFoundException::new);
  }

  /**
   * {@inheritDoc}
   *
   * <p>El nombre de usuario llega ya normalizado a minúsculas por la invariante del modelo, por lo
   * que la comprobación de duplicados es insensible a mayúsculas y minúsculas. El rol no se recibe
   * del cliente: la asignación de USER es una invariante del modelo construida por el mapper.
   */
  @Override
  public void register(User user) {
    String normalizedEmail = user.getEmail().toLowerCase(Locale.ROOT);
    String normalizedUsername = user.getUsername().toLowerCase(Locale.ROOT);

    if (this.userRepository.existsByEmail(normalizedEmail)) {
      throw new EmailAlreadyRegisteredException("El email ya esta registrado.");
    }

    if (this.userRepository.existsByUsername(normalizedUsername)) {
      throw new UsernameAlreadyRegisteredException("El nombre de usuario ya esta en uso.");
    }

    user.setEmail(normalizedEmail);
    user.setPassword(this.passwordEncoder.encode(user.getPassword()));

    User registered = this.userRepository.save(user);

    // El alta y su saldo inicial se confirman juntos: si no puede asignarse el saldo, no se
    // confirma el alta. La inserción es idempotente y no sobrescribe un saldo existente.
    if (!this.userCreditRepository.existsById(registered.getId())) {
      this.userCreditRepository.save(UserCredit.forUser(registered.getId()));
    }
  }

  /** {@inheritDoc} */
  @Override
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
