package footballmarket.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import footballmarket.features.market.application.SuperuserProvisioningService;
import footballmarket.features.market.application.exceptions.InvalidSuperuserConfigurationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Inicialización de arranque del SUPERUSER y validación de su configuración.
 *
 * <p>Se verifica con instancias directas porque lo que se observa es la decisión de aprovisionar y
 * la validez de la configuración. El aprovisionamiento en sí, incluida la persistencia y el hasheo,
 * corresponde a la categoría Service.
 */
@org.junit.jupiter.api.Tag("unit")
class SuperuserInitializationTest {

  private static final String USERNAME = "market.owner";
  private static final String EMAIL = "owner@test.com";
  private static final String PASSWORD = "password123";

  @Nested
  @DisplayName("Configuración de aprovisionamiento del SUPERUSER")
  class Properties {
    @ParameterizedTest
    @DisplayName("Rechaza la configuración habilitada con una propiedad obligatoria ausente")
    @CsvSource(
        nullValues = "NULL",
        value = {
          "NULL,owner@test.com,password123",
          "market.owner,NULL,password123",
          "market.owner,owner@test.com,NULL",
          " ,owner@test.com,password123",
          "market.owner, ,password123",
          "market.owner,owner@test.com, "
        })
    void rechazaConfiguracionHabilitadaEIncompleta(String username, String email, String password) {
      // Act / Assert
      assertThatThrownBy(() -> new SuperuserProperties(true, username, email, password))
          .isInstanceOf(InvalidSuperuserConfigurationException.class);
    }

    @Test
    @DisplayName("Admite la configuración deshabilitada sin identidad ni contraseña")
    void admiteConfiguracionDeshabilitadaVacia() {
      // Act
      SuperuserProperties properties = new SuperuserProperties(false, null, null, null);

      // Assert
      assertThat(properties.enabled()).isFalse();
    }

    @Test
    @DisplayName("Conserva los valores configurados cuando la aprovisionamiento está habilitado")
    void conservaLosValoresConfigurados() {
      // Act
      SuperuserProperties properties = new SuperuserProperties(true, USERNAME, EMAIL, PASSWORD);

      // Assert
      assertThat(properties.username()).isEqualTo(USERNAME);
      assertThat(properties.email()).isEqualTo(EMAIL);
      assertThat(properties.password()).isEqualTo(PASSWORD);
    }

    @Test
    @DisplayName("No expone la contraseña ni las identidades en la representación")
    void noExponeLosValoresSensiblesEnLaRepresentacion() {
      // Act
      SuperuserProperties properties = new SuperuserProperties(true, USERNAME, EMAIL, PASSWORD);

      // Assert
      assertThat(properties.toString())
          .doesNotContain(PASSWORD)
          .doesNotContain(EMAIL)
          .doesNotContain(USERNAME)
          .contains("password=configurado")
          .contains("email=configurado")
          .contains("username=configurado");
    }
  }

  @Nested
  @DisplayName("Arranque de la aplicación")
  class Startup {
    @Test
    @DisplayName("No crea ninguna cuenta de SUPERUSER cuando la inicialización está deshabilitada")
    void noCreaSuperuserCuandoEstaDeshabilitada() {
      // Arrange
      SuperuserProvisioningService provisioningService = mock(SuperuserProvisioningService.class);
      SuperuserStartupConfiguration configuration =
          new SuperuserStartupConfiguration(
              provisioningService, new SuperuserProperties(false, null, null, null));

      // Act: Spring invoca el callback del bean durante el refresh del contexto.
      configuration.provisionSuperuser().afterSingletonsInstantiated();

      // Assert
      verify(provisioningService, never()).provisionSuperuser();
    }

    @Test
    @DisplayName("Aprovisiona el SUPERUSER cuando la inicialización está habilitada")
    void aprovisionaSuperuserCuandoEstaHabilitada() {
      // Arrange
      SuperuserProvisioningService provisioningService = mock(SuperuserProvisioningService.class);
      SuperuserStartupConfiguration configuration =
          new SuperuserStartupConfiguration(
              provisioningService, new SuperuserProperties(true, USERNAME, EMAIL, PASSWORD));

      // Act: Spring invoca el callback del bean durante el refresh del contexto.
      configuration.provisionSuperuser().afterSingletonsInstantiated();

      // Assert
      verify(provisioningService).provisionSuperuser();
    }

    @Test
    @DisplayName("Repite la provisión en cada arranque sin duplicar la cuenta")
    void repiteLaProvisionEnCadaArranque() {
      // Arrange
      SuperuserProvisioningService provisioningService = mock(SuperuserProvisioningService.class);
      SuperuserStartupConfiguration configuration =
          new SuperuserStartupConfiguration(
              provisioningService, new SuperuserProperties(true, USERNAME, EMAIL, PASSWORD));

      // Act: cada arranque de la aplicación vuelve a ejecutar el callback.
      configuration.provisionSuperuser().afterSingletonsInstantiated();
      configuration.provisionSuperuser().afterSingletonsInstantiated();

      // Assert
      verify(provisioningService, times(2)).provisionSuperuser();
    }
  }
}
