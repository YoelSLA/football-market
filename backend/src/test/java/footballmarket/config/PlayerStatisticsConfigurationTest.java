package footballmarket.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

@org.junit.jupiter.api.Tag("lightweight")
class PlayerStatisticsConfigurationTest {
  private final ApplicationContextRunner runner =
      new ApplicationContextRunner()
          .withUserConfiguration(PlayerStatisticsConfig.class)
          .withPropertyValues(
              "whoscored.statistics.scraping-workers=3",
              "whoscored.statistics.json-workers=3",
              "whoscored.statistics.request-interval-ms=2500",
              "whoscored.statistics.scraping-batch-timeout-ms=1800000",
              "whoscored.statistics.navigation-timeout-ms=20000",
              "whoscored.statistics.query-timeout-ms=20000",
              "whoscored.statistics.filter-third-party-resources=false",
              "whoscored.statistics.headed=true");

  @Nested
  @DisplayName("Binding independiente y validación de estadísticas")
  class Binding {
    @Test
    @DisplayName(
        "Carga estadísticas sin requerir configuración de identidad ni crear cliente o navegador")
    void bindsStatisticsIndependently() {
      // Act / Assert
      runner.run(
          context -> {
            assertThat(context).hasNotFailed().hasSingleBean(PlayerStatisticsProperties.class);
            assertThat(context).doesNotHaveBean(WhoScoredProperties.class);
            assertThat(context).doesNotHaveBean("whoScoredRestClient");
            assertThat(context).doesNotHaveBean(com.microsoft.playwright.Browser.class);
            PlayerStatisticsProperties properties =
                context.getBean(PlayerStatisticsProperties.class);
            assertThat(properties.scrapingWorkers()).isEqualTo(3);
            assertThat(properties.jsonWorkers()).isEqualTo(3);
            assertThat(properties.requestIntervalMs()).isEqualTo(2500);
            assertThat(properties.scrapingBatchTimeoutMs()).isEqualTo(1800000);
            assertThat(properties.navigationTimeoutMs()).isEqualTo(20000);
            assertThat(properties.queryTimeoutMs()).isEqualTo(20000);
            assertThat(properties.filterThirdPartyResources()).isFalse();
            assertThat(properties.headed()).isTrue();
          });
    }

    @ParameterizedTest
    @ValueSource(
        strings = {
          "whoscored.statistics.scraping-workers=0", "whoscored.statistics.json-workers=0",
          "whoscored.statistics.request-interval-ms=0",
              "whoscored.statistics.scraping-batch-timeout-ms=0",
          "whoscored.statistics.navigation-timeout-ms=0", "whoscored.statistics.query-timeout-ms=0",
          "whoscored.statistics.headed=false"
        })
    @DisplayName("Mantiene el rechazo de propiedades inválidas en el binding separado")
    void rejectsInvalidStatistics(String override) {
      // Act / Assert
      runner.withPropertyValues(override).run(context -> assertThat(context).hasFailed());
    }
  }
}
