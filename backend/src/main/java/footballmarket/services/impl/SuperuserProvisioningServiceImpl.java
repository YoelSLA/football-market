package footballmarket.services.impl;

import footballmarket.config.SuperuserProperties;
import footballmarket.models.PlayerTokenEmission;
import footballmarket.models.User;
import footballmarket.models.UserCredit;
import footballmarket.models.enums.UserRole;
import footballmarket.repositories.PlayerTokenEmissionRepository;
import footballmarket.repositories.UserCreditRepository;
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
  private final UserCreditRepository userCreditRepository;
  private final PlayerTokenEmissionRepository emissions;
  private final PasswordEncoder passwordEncoder;
  private final SuperuserProperties properties;

  /**
   * {@inheritDoc}
   *
   * <p>Con configuración deshabilitada no crea un usuario: requiere un SUPERUSER existente y
   * completa su mercado. Todas las ramas exitosas recuperan emisiones pendientes sin reponer stock.
   */
  @Override
  public User provisionSuperuser() {
    if (!this.properties.enabled()) {
      User existing =
          this.findSuperuser()
              .orElseThrow(
                  () ->
                      new SuperuserProvisioningException(
                          "El aprovisionamiento del SUPERUSER está deshabilitado y todavía no"
                              + " existe una cuenta con ese rol."));
      this.ensureMarketInitialized(existing.getId());
      return existing;
    }

    String configuredEmail = this.properties.email().strip().toLowerCase(Locale.ROOT);
    Optional<User> existingSuperuser = this.findSuperuser();

    if (existingSuperuser.isPresent()) {
      this.ensureMatchesConfiguration(existingSuperuser.get(), configuredEmail);
      this.ensureMarketInitialized(existingSuperuser.get().getId());
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

    User provisioned = this.userRepository.save(superuser);
    this.ensureMarketInitialized(provisioned.getId());

    return provisioned;
  }

  /**
   * Completa cuenta e inventario en la transacción de aprovisionamiento, sin reponer emisiones.
   *
   * <p>Las sincronizaciones normales deben comenzar después del aprovisionamiento. No coordina
   * llamadas concurrentes al catálogo ni múltiples instancias. Cualquier fallo revierte también el
   * usuario y la cuenta cuando son nuevos.
   *
   * @param userId identificador del propietario inicial
   * @throws SuperuserProvisioningException si el inventario persistido es inconsistente
   */
  private void ensureMarketInitialized(Long userId) {
    this.ensureCreditAccount(userId);
    this.userCreditRepository.flush();
    if (this.emissions.hasInconsistentInventory()) {
      throw new SuperuserProvisioningException(
          "El inventario de tokens es inconsistente. No se realiza reparación automática.");
    }
    this.emissions.recoverMissingEmissions(userId, PlayerTokenEmission.EMITTED_TOKENS);
  }

  /**
   * Asegura la cuenta de créditos del superusuario con saldo cero.
   *
   * <p>El superusuario es el vendedor del mercado y no compra, de modo que su saldo inicial es
   * cero. La creación es idempotente: si la cuenta ya existe no se sobrescribe ni se restaura.
   *
   * @param userId identificador del superusuario aprovisionado
   */
  private void ensureCreditAccount(Long userId) {
    if (!this.userCreditRepository.existsById(userId)) {
      this.userCreditRepository.save(UserCredit.forSuperuser(userId));
    }
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
