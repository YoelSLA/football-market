package footballmarket.config;

import static org.assertj.core.api.Assertions.*;

import java.io.IOException;
import org.junit.jupiter.api.*;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mock.env.MockEnvironment;

/** Configuración de archivo UTF-8 y aislamiento de tests, sin abrir archivos ni arrancar Spring. */
@Tag("lightweight")
class BackendFileLoggingTest {
  @Test
  @DisplayName("Sin configuración explícita conserva un archivo local UTF-8")
  void resolvesDefaultLog() throws IOException {
    MockEnvironment environment = new MockEnvironment();
    environment
        .getPropertySources()
        .addLast(
            new YamlPropertySourceLoader()
                .load("main", new ClassPathResource("application.yml"))
                .getFirst());
    assertThat(environment.getProperty("logging.file.name")).isEqualTo("logs/football-market.log");
    assertThat(environment.getProperty("logging.charset.file")).isEqualTo("UTF-8");
  }

  @Test
  @DisplayName("Resuelve el destino de archivo configurado por el operador")
  void resolvesConfiguredLog() throws IOException {
    MockEnvironment environment = new MockEnvironment();
    environment.setProperty("FOOTBALL_MARKET_LOG_FILE", "test-logs/custom.log");
    environment
        .getPropertySources()
        .addLast(
            new YamlPropertySourceLoader()
                .load("main", new ClassPathResource("application.yml"))
                .getFirst());
    assertThat(environment.getProperty("logging.file.name")).isEqualTo("test-logs/custom.log");
  }

  @Test
  @DisplayName("El perfil test desactiva el archivo de logging del backend")
  void disablesTestFile() throws IOException {
    MockEnvironment environment = new MockEnvironment();
    YamlPropertySourceLoader loader = new YamlPropertySourceLoader();
    environment
        .getPropertySources()
        .addLast(loader.load("main", new ClassPathResource("application.yml")).getFirst());
    environment
        .getPropertySources()
        .addFirst(loader.load("test", new ClassPathResource("application-test.yml")).getFirst());
    assertThat(environment.getProperty("logging.file.name")).isEmpty();
  }
}
