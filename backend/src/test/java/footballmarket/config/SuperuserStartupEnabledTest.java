package footballmarket.config;

import static org.assertj.core.api.Assertions.assertThat;

import footballmarket.models.User;
import footballmarket.models.enums.UserRole;
import footballmarket.services.AuthenticationService;
import footballmarket.services.SuperuserProvisioningService;
import footballmarket.support.TestcontainersConfiguration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

/**
 * Arranque real del contexto con el aprovisionamiento del SUPERUSER habilitado.
 *
 * <p>Las demás pruebas de contexto arrancan con el aprovisionamiento deshabilitado, de modo que el
 * camino de arranque habilitado solo quedaba cubierto con un servicio sustituido. Esta clase
 * levanta el contexto completo contra PostgreSQL real con migraciones de Flyway reales y comprueba
 * que la cuenta se crea efectivamente, conserva su identidad configurada y puede iniciar sesión.
 */
@SpringBootTest(
    properties = {
      "footballmarket.superuser.enabled=true",
      "footballmarket.superuser.username=market.owner",
      "footballmarket.superuser.email=owner@test.com",
      "footballmarket.superuser.password=password123"
    })
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
class SuperuserStartupEnabledTest {

  private static final String CONFIGURED_EMAIL = "owner@test.com";
  private static final String CONFIGURED_USERNAME = "market.owner";
  private static final String CONFIGURED_PASSWORD = "password123";

  @Autowired private SuperuserProvisioningService provisioningService;

  @Autowired private AuthenticationService authenticationService;

  @Nested
  @DisplayName("Arranque con el aprovisionamiento habilitado")
  class EnabledStartup {
    @Test
    @DisplayName("Crea el SUPERUSER durante el arranque con la identidad configurada")
    void creaElSuperuserDuranteElArranque() {
      // Act
      User superuser = provisioningService.findSuperuser().orElseThrow();

      // Assert
      assertThat(superuser.getId()).isNotNull();
      assertThat(superuser.getRole()).isEqualTo(UserRole.SUPERUSER);
      assertThat(superuser.getEmail()).isEqualTo(CONFIGURED_EMAIL);
      assertThat(superuser.getUsername()).isEqualTo(CONFIGURED_USERNAME);
    }

    @Test
    @DisplayName(
        "El SUPERUSER creado al arrancar puede iniciar sesión con su contraseña configurada")
    void elSuperuserPuedeIniciarSesion() {
      // Act / Assert
      assertThat(authenticationService.login(CONFIGURED_EMAIL, CONFIGURED_PASSWORD)).isNotBlank();
    }

    @Test
    @DisplayName("Almacena cifrada la contraseña configurada del SUPERUSER")
    void almacenaCifradaLaContrasenaConfigurada() {
      // Act
      User superuser = provisioningService.findSuperuser().orElseThrow();

      // Assert
      assertThat(superuser.getPassword()).isNotEqualTo(CONFIGURED_PASSWORD);
    }

    @Test
    @DisplayName("Reejecutar la provisión no crea un segundo SUPERUSER ni altera el existente")
    void reejecutarLaProvisionNoCreaUnSegundoSuperuser() {
      // Arrange
      User creado = provisioningService.findSuperuser().orElseThrow();

      // Act
      User segundo = provisioningService.provisionSuperuser();

      // Assert
      assertThat(segundo.getId()).isEqualTo(creado.getId());
      assertThat(segundo.getEmail()).isEqualTo(CONFIGURED_EMAIL);
      assertThat(segundo.getUsername()).isEqualTo(CONFIGURED_USERNAME);
    }
  }
}
