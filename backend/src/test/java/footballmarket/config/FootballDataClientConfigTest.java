package footballmarket.config;

import static org.assertj.core.api.Assertions.*;

import footballmarket.integrations.exceptions.InvalidFootballDataConfigurationException;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class FootballDataClientConfigTest {

  @Nested
  @DisplayName("Validación de la URL del proveedor")
  class ProviderUrl {
    @ParameterizedTest
    @ValueSource(
        strings = {
          "http://provider.example",
          "https://user:password@provider.example",
          "https://provider.example?token=secret",
          "https://provider.example#fragment"
        })
    void rechazaUrlsInsegurasDelProveedor(String url) {
      var properties =
          new FootballDataProperties("test-only", url, List.of("PL", "BL1", "PD", "SA", "FL1"));
      assertThatThrownBy(() -> new FootballDataClientConfig().footballDataRestClient(properties))
          .isInstanceOf(InvalidFootballDataConfigurationException.class);
    }
  }

  @Nested
  @DisplayName("Configuración del cliente y protección de la clave")
  class ClientConfiguration {
    @Test
    void exigeLigasOrdenadasNoVaciasYOcultaLaClave() {
      assertThatThrownBy(() -> new FootballDataProperties("key", "https://provider.example", null))
          .isInstanceOf(InvalidFootballDataConfigurationException.class);
      assertThatThrownBy(
              () -> new FootballDataProperties("key", "https://provider.example", List.of()))
          .isInstanceOf(InvalidFootballDataConfigurationException.class);
      assertThatThrownBy(
              () -> new FootballDataProperties("key", "https://provider.example", List.of(" ")))
          .isInstanceOf(InvalidFootballDataConfigurationException.class);
      var properties =
          new FootballDataProperties(
              "private-test-key",
              "https://provider.example",
              List.of("PL", "BL1", "PD", "SA", "FL1"));
      assertThat(properties.competitions()).containsExactly("PL", "BL1", "PD", "SA", "FL1");
      assertThat(properties.toString()).doesNotContain("private-test-key");
      assertThat(new FootballDataClientConfig().footballDataRestClient(properties)).isNotNull();
      assertThatThrownBy(
              () ->
                  new FootballDataClientConfig()
                      .footballDataRestClient(
                          new FootballDataProperties(
                              " ",
                              "https://provider.example",
                              List.of("PL", "BL1", "PD", "SA", "FL1"))))
          .isInstanceOf(InvalidFootballDataConfigurationException.class);
    }
  }
}
