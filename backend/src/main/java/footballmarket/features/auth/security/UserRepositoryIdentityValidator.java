package footballmarket.features.auth.security;

import footballmarket.features.auth.infraestructure.UserRepository;
import footballmarket.security.PersistedIdentityValidator;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Valida la existencia persistida de un usuario a partir del sujeto del token consultando la base
 * de datos mediante {@link UserRepository#existsByEmail(String)}.
 */
@Component
@RequiredArgsConstructor
public class UserRepositoryIdentityValidator implements PersistedIdentityValidator {

  private final UserRepository userRepository;

  /**
   * {@inheritDoc}
   *
   * <p>Normaliza el correo del sujeto a minúsculas conforme a las reglas del dominio de
   * autenticación. Cualquier excepción de acceso a datos o infraestructura de persistencia escala
   * sin capturarse para que Spring la gestione como un fallo del servidor.
   */
  @Override
  @Transactional(readOnly = true)
  public boolean exists(String subject) {
    if (subject == null || subject.isBlank()) {
      return false;
    }
    return this.userRepository.existsByEmail(subject.toLowerCase(Locale.ROOT));
  }
}
