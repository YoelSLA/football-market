package footballmarket.common.config;

import static org.assertj.core.api.Assertions.*;

import java.io.IOException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mock.env.MockEnvironment;

/** Configuración de logging sin abrir archivos ni arrancar Spring. */
@Tag("lightweight")
class BackendLoggingConfigurationTest {
  @Nested
  @DisplayName("Salida de logs")
  class LogOutput {
    @Test
    @DisplayName("No configura archivos de logs")
    void disablesFileLogging() throws IOException {
      MockEnvironment environment = new MockEnvironment();
      environment
          .getPropertySources()
          .addLast(
              new YamlPropertySourceLoader()
                  .load("main", new ClassPathResource("application.yml"))
                  .getFirst());

      assertThat(environment.getProperty("logging.file.name")).isNull();
      assertThat(environment.getProperty("logging.charset.file")).isNull();
    }
  }
}
