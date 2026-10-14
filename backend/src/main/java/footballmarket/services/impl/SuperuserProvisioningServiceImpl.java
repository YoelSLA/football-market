package footballmarket.services.impl;

import footballmarket.config.SuperuserProperties;
import footballmarket.models.User;
import footballmarket.models.enums.UserRole;
import footballmarket.repositories.UserRepository;
import footballmarket.services.SuperuserProvisioningService;
import footballmarket.services.exceptions.SuperuserProvisioningException;
import java.util.Locale;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementación transaccional e idempotente del aprovisionamiento del SUPERUSER.
 *
 * <p>El SUPERUSER no se crea mediante el registro público. Esta clase es el mecanismo interno y
 * seguro que lo genera a partir de la configuración del entorno de ejecución: la contraseña se
 * hashea con BCrypt y nunca se persiste ni se registra en texto plano.
 */
@Service
@Transactional
@RequiredArgsConstructor
public class SuperuserProvisioningServiceImpl implements SuperuserProvisioningService {

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;
  private final SuperuserProperties properties;

  /**
   * {@inheritDoc}
   *
   * <p>No hace nada cuando el aprovisionamiento está deshabilitado y no existe todavía un
   * SUPERUSER, de modo que una instalación opta por crearlo de forma explícita.
   */
  @Override
  public User provisionSuperuser() {
    if (!this.properties.enabled()) {
      return this.findSuperuser()
          .orElseThrow(
              () ->
                  new SuperuserProvisioningException(
                      "El aprovisionamiento del SUPERUSER está deshabilitado y todavía no existe"
                          + " una cuenta con ese rol."));
    }

    String configuredEmail = this.properties.email().strip().toLowerCase(Locale.ROOT);
    Optional<User> existingSuperuser = this.findSuperuser();

    if (existingSuperuser.isPresent()) {
      this.ensureMatchesConfiguration(existingSuperuser.get(), configuredEmail);
      return existingSuperuser.get();
    }

    this.ensureEmailAvailable(configuredEmail);

    User superuser =
        new User(
            null,
            this.properties.username(),
            configuredEmail,
            this.properties.password(),
            UserRole.SUPERUSER);

    superuser.setPassword(this.passwordEncoder.encode(superuser.getPassword()));

    return this.userRepository.save(superuser);
  }

  /** {@inheritDoc} */
  @Override
  @Transactional(readOnly = true)
  public Optional<User> findSuperuser() {
    return this.userRepository.findByRole(UserRole.SUPERUSER);
  }

  /**
   * Verifica que el SUPERUSER persistido corresponde a la configuración de arranque.
   *
   * @param existing superusuario persistido
   * @param configuredEmail correo electrónico normalizado de la configuración
   * @throws SuperuserProvisioningException si el SUPERUSER existente corresponde a otra identidad
   *     configurada
   */
  private void ensureMatchesConfiguration(User existing, String configuredEmail) {
    if (!existing.getEmail().toLowerCase(Locale.ROOT).equals(configuredEmail)) {
      throw new SuperuserProvisioningException(
          "Ya existe un SUPERUSER con una identidad distinta a la configurada. No se crea una"
              + " segunda cuenta ni se modifica la existente.");
    }
  }

  /**
   * Verifica que el correo electrónico configurado no esté ocupado por otro usuario.
   *
   * @param configuredEmail correo electrónico normalizado de la configuración
   * @throws SuperuserProvisioningException si el correo pertenece a un usuario existente
   */
  private void ensureEmailAvailable(String configuredEmail) {
    if (this.userRepository.existsByEmail(configuredEmail)) {
      throw new SuperuserProvisioningException(
          "El email configurado para el SUPERUSER ya pertenece a un usuario existente.");
    }
  }
}
