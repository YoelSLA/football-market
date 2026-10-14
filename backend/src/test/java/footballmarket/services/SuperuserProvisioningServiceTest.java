package footballmarket.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import footballmarket.config.SuperuserProperties;
import footballmarket.models.User;
import footballmarket.models.enums.UserRole;
import footballmarket.services.exceptions.SuperuserProvisioningException;
import footballmarket.support.TestcontainersConfiguration;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

/**
 * Aprovisionamiento del SUPERUSER con persistencia real.
 *
 * <p>El componente bajo prueba es el Service. La configuración se sustituye porque es la entrada
 * del caso de uso y no forma parte del comportamiento que se verifica; el resto de colaboradores,
 * incluidos el repositorio, la persistencia y el hasheo de contraseñas, son reales.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
@Transactional
class SuperuserProvisioningServiceTest {

  private static final String CONFIGURED_EMAIL = "owner@test.com";
  private static final String CONFIGURED_USERNAME = "market.owner";
  private static final String CONFIGURED_PASSWORD = "password123";

  @Autowired private SuperuserProvisioningService provisioningService;
  @Autowired private AuthenticationService authenticationService;

  @MockitoBean private SuperuserProperties properties;

  private void enableProvisioning(String email) {
    when(properties.enabled()).thenReturn(true);
    when(properties.username()).thenReturn(CONFIGURED_USERNAME);
    when(properties.email()).thenReturn(email);
    when(properties.password()).thenReturn(CONFIGURED_PASSWORD);
  }

  private void disableProvisioning() {
    when(properties.enabled()).thenReturn(false);
  }

  @Nested
  @DisplayName("Aprovisionamiento del dueño inicial de los tokens")
  class Provisioning {
    @Test
    @DisplayName("Crea el SUPERUSER con la identidad configurada y el rol SUPERUSER")
    void creaElSuperuserConLaIdentidadConfigurada() {
      // Arrange
      enableProvisioning(CONFIGURED_EMAIL);

      // Act
      User superuser = provisioningService.provisionSuperuser();

      // Assert
      assertThat(superuser.getId()).isNotNull();
      assertThat(superuser.getRole()).isEqualTo(UserRole.SUPERUSER);
      assertThat(superuser.getEmail()).isEqualTo(CONFIGURED_EMAIL);
      assertThat(superuser.getUsername()).isEqualTo(CONFIGURED_USERNAME);
    }

    @Test
    @DisplayName("Normaliza el email configurado del SUPERUSER a minúsculas")
    void normalizaElEmailConfigurado() {
      // Arrange
      enableProvisioning("Owner@Test.com");

      // Act
      User superuser = provisioningService.provisionSuperuser();

      // Assert
      assertThat(superuser.getEmail()).isEqualTo(CONFIGURED_EMAIL);
    }

    @Test
    @DisplayName("Almacena la contraseña del SUPERUSER cifrada y permite iniciar sesión")
    void almacenaLaContrasenaCifrada() {
      // Arrange
      enableProvisioning(CONFIGURED_EMAIL);

      // Act
      User superuser = provisioningService.provisionSuperuser();

      // Assert
      assertThat(superuser.getPassword()).isNotEqualTo(CONFIGURED_PASSWORD);

      // Verify
      assertThat(authenticationService.login(CONFIGURED_EMAIL, CONFIGURED_PASSWORD)).isNotBlank();
    }

    @Test
    @DisplayName("No crea un segundo SUPERUSER al aprovisionar repetidamente")
    void noCreaUnSegundoSuperuserAlAprovisionarRepetidamente() {
      // Arrange
      enableProvisioning(CONFIGURED_EMAIL);
      User first = provisioningService.provisionSuperuser();

      // Act
      User second = provisioningService.provisionSuperuser();

      // Assert
      assertThat(second.getId()).isEqualTo(first.getId());
      assertThat(second.getEmail()).isEqualTo(first.getEmail());
      assertThat(second.getUsername()).isEqualTo(first.getUsername());
    }

    @Test
    @DisplayName("Rechaza aprovisionar cuando el email configurado ya pertenece a un usuario")
    void rechazaAprovisionarConEmailOcupado() {
      // Arrange
      authenticationService.register(new User("public.user", CONFIGURED_EMAIL, "password"));
      enableProvisioning(CONFIGURED_EMAIL);

      // Act / Assert
      assertThatThrownBy(() -> provisioningService.provisionSuperuser())
          .isInstanceOf(SuperuserProvisioningException.class);
    }

    @Test
    @DisplayName("Rechaza aprovisionar cuando ya existe un SUPERUSER con otra identidad")
    void rechazaAprovisionarConOtraIdentidadExistente() {
      // Arrange
      enableProvisioning(CONFIGURED_EMAIL);
      provisioningService.provisionSuperuser();
      when(properties.email()).thenReturn("otro@test.com");

      // Act / Assert
      assertThatThrownBy(() -> provisioningService.provisionSuperuser())
          .isInstanceOf(SuperuserProvisioningException.class);

      // Verify
      assertThat(provisioningService.findSuperuser())
          .hasValueSatisfying(
              superuser -> assertThat(superuser.getEmail()).isEqualTo(CONFIGURED_EMAIL));
    }
  }

  @Nested
  @DisplayName("Recuperación del dueño inicial de los tokens")
  class Retrieval {
    @Test
    @DisplayName("Recupera el SUPERUSER persistido para las operaciones posteriores del mercado")
    void recuperaElSuperuserPersistido() {
      // Arrange
      enableProvisioning(CONFIGURED_EMAIL);
      User created = provisioningService.provisionSuperuser();

      // Act
      Optional<User> found = provisioningService.findSuperuser();

      // Assert
      assertThat(found)
          .hasValueSatisfying(
              superuser -> assertThat(superuser.getId()).isEqualTo(created.getId()));
    }

    @Test
    @DisplayName("No encuentra SUPERUSER antes de aprovisionarlo")
    void noEncuentraSuperuserAntesDeAprovisionarlo() {
      // Arrange
      disableProvisioning();

      // Act
      Optional<User> found = provisioningService.findSuperuser();

      // Assert
      assertThat(found).isEmpty();
    }
  }

  @Nested
  @DisplayName("Aprovisionamiento deshabilitado")
  class DisabledProvisioning {
    @Test
    @DisplayName("Rechaza aprovisionar cuando la inicialización está deshabilitada")
    void rechazaAprovisionarCuandoEstaEstaDeshabilitada() {
      // Arrange
      disableProvisioning();

      // Act / Assert
      assertThatThrownBy(() -> provisioningService.provisionSuperuser())
          .isInstanceOf(SuperuserProvisioningException.class);
    }
  }
}
