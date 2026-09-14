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

@Service
@RequiredArgsConstructor
public class AuthenticationServiceImpl implements AuthenticationService {

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;
  private final JWTProvider jwtProvider;

  @Override
  public void register(User user) {
    String normalizedEmail = user.getEmail().toLowerCase(Locale.ROOT);

    if (userRepository.existsByEmail(normalizedEmail)) {
      throw new EmailAlreadyRegisteredException("El email ya esta registrado.");
    }

    user.setEmail(normalizedEmail);
    user.setPassword(passwordEncoder.encode(user.getPassword()));

    userRepository.save(user);
  }

  @Override
  public String login(String email, String password) {
    String normalizedEmail = email.toLowerCase(Locale.ROOT);

    User user =
        userRepository
            .findByEmail(normalizedEmail)
            .orElseThrow(() -> new InvalidCredentialsException("Credenciales invalidas."));

    if (!passwordEncoder.matches(password, user.getPassword())) {
      throw new InvalidCredentialsException("Credenciales invalidas.");
    }

    return jwtProvider.generateToken(user.getEmail());
  }
}
