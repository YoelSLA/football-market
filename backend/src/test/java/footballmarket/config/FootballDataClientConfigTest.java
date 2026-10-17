package footballmarket.config;

import static org.assertj.core.api.Assertions.*;

import footballmarket.integrations.exceptions.InvalidFootballDataConfigurationException;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

class FootballDataClientConfigTest {

  @Nested
  @DisplayName("Validación de la URL del proveedor")
  class ProviderUrl {
    @ParameterizedTest
    @DisplayName("Rechaza URLs del proveedor que exponen credenciales o usan transporte inseguro")
    @ValueSource(
        strings = {
          "http://provider.example",
          "https://user:password@provider.example",
          "https://provider.example?token=secret",
          "https://provider.example#fragment"
        })
    void rechazaUrlsInsegurasDelProveedor(String url) {
      FootballDataProperties properties =
          new FootballDataProperties("test-only", url, List.of("PL", "BL1", "PD", "SA", "FL1"));
      assertThatThrownBy(() -> new FootballDataClientConfig().footballDataRestClient(properties))
          .isInstanceOf(InvalidFootballDataConfigurationException.class);
    }
  }

  @Nested
  @DisplayName("Configuración del cliente y protección de la clave")
  class ClientConfiguration {
    @ParameterizedTest
    @MethodSource("invalidCompetitions")
    @DisplayName("Rechaza ligas ausentes, vacías o en blanco")
    void rechazaLigasInvalidas(List<String> competitions) {
      assertThatThrownBy(
              () -> new FootballDataProperties("key", "https://provider.example", competitions))
          .isInstanceOf(InvalidFootballDataConfigurationException.class);
    }

    static java.util.stream.Stream<List<String>> invalidCompetitions() {
      return java.util.stream.Stream.of(null, List.of(), List.of(" "));
    }

    @Test
    @DisplayName("Conserva el orden configurado de las cinco ligas")
    void conservaLigasOrdenadas() {
      FootballDataProperties properties =
          new FootballDataProperties(
              "private-test-key",
              "https://provider.example",
              List.of("PL", "BL1", "PD", "SA", "FL1"));
      assertThat(properties.competitions()).containsExactly("PL", "BL1", "PD", "SA", "FL1");
    }

    @Test
    @DisplayName("No expone la clave privada en la representación de la configuración")
    void ocultaLaClave() {
      FootballDataProperties properties =
          new FootballDataProperties(
              "private-test-key",
              "https://provider.example",
              List.of("PL", "BL1", "PD", "SA", "FL1"));
      assertThat(properties.toString()).doesNotContain("private-test-key");
    }

    @Test
    @DisplayName("Construye el cliente HTTP con una configuración válida")
    void construyeCliente() {
      FootballDataProperties properties =
          new FootballDataProperties(
              "private-test-key",
              "https://provider.example",
              List.of("PL", "BL1", "PD", "SA", "FL1"));
      assertThat(new FootballDataClientConfig().footballDataRestClient(properties)).isNotNull();
    }

    @Test
    @DisplayName("Rechaza una clave de proveedor en blanco al construir el cliente")
    void rechazaClaveEnBlanco() {
      FootballDataProperties properties =
          new FootballDataProperties(
              " ", "https://provider.example", List.of("PL", "BL1", "PD", "SA", "FL1"));
      assertThatThrownBy(() -> new FootballDataClientConfig().footballDataRestClient(properties))
          .isInstanceOf(InvalidFootballDataConfigurationException.class);
    }
  }
}
