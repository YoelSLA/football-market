package footballmarket.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.web.client.RestClient;

class WhoScoredConfigurationTest {
  private final ApplicationContextRunner runner =
      new ApplicationContextRunner()
          .withUserConfiguration(WhoScoredClientConfig.class)
          .withPropertyValues(
              "whoscored.base-url=https://provider.example",
              "whoscored.user-agent=identity-test",
              "whoscored.page-size=3",
              "whoscored.request-interval-ms=2500",
              "whoscored.connect-timeout-ms=5000",
              "whoscored.read-timeout-ms=20000",
              "whoscored.max-pages-per-league=10000",
              "whoscored.max-response-bytes=5242880",
              "whoscored.competitions[0].league-id=101",
              "whoscored.competitions[0].tournament-id=2",
              "whoscored.competitions[1].league-id=102",
              "whoscored.competitions[1].tournament-id=3",
              "whoscored.competitions[2].league-id=103",
              "whoscored.competitions[2].tournament-id=4",
              "whoscored.competitions[3].league-id=104",
              "whoscored.competitions[3].tournament-id=5",
              "whoscored.competitions[4].league-id=105",
              "whoscored.competitions[4].tournament-id=22");

  @Nested
  @DisplayName("Binding y cliente dedicado")
  class Binding {
    @Test
    @DisplayName("Una configuración válida crea únicamente su cliente dedicado")
    void bindsValidConfiguration() {
      // Act / Assert
      runner.run(
          context -> {
            assertThat(context).hasNotFailed().hasSingleBean(WhoScoredProperties.class);
            assertThat(context.getBean("whoScoredRestClient")).isInstanceOf(RestClient.class);
            assertThat(context.getBean(WhoScoredProperties.class).competitions()).hasSize(5);
          });
    }

    @ParameterizedTest
    @ValueSource(
        strings = {
          "whoscored.base-url=http://provider.example",
          "whoscored.page-size=0",
          "whoscored.page-size=101",
          "whoscored.user-agent=",
          "whoscored.request-interval-ms=0",
          "whoscored.connect-timeout-ms=0",
          "whoscored.read-timeout-ms=0",
          "whoscored.max-pages-per-league=0",
          "whoscored.max-response-bytes=0",
          "whoscored.competitions[1].league-id=101",
          "whoscored.competitions[1].tournament-id=2",
          "whoscored.competitions[1].tournament-id=0"
        })
    @DisplayName("Rechaza configuración inválida durante startup antes de red")
    void rejectsInvalidConfiguration(String override) {
      // Act / Assert
      runner.withPropertyValues(override).run(context -> assertThat(context).hasFailed());
    }

    @Test
    @DisplayName("Acepta cinco competiciones distintas ajenas a las observadas")
    void acceptsAnyFiveDistinctCompetitions() {
      // Act / Assert
      runner
          .withPropertyValues(
              "whoscored.competitions[0].league-id=201",
              "whoscored.competitions[0].tournament-id=11",
              "whoscored.competitions[1].league-id=202",
              "whoscored.competitions[1].tournament-id=12",
              "whoscored.competitions[2].league-id=203",
              "whoscored.competitions[2].tournament-id=13",
              "whoscored.competitions[3].league-id=204",
              "whoscored.competitions[3].tournament-id=14",
              "whoscored.competitions[4].league-id=205",
              "whoscored.competitions[4].tournament-id=15")
          .run(
              context ->
                  assertThat(context)
                      .hasNotFailed()
                      .getBean(WhoScoredProperties.class)
                      .satisfies(properties -> assertThat(properties.competitions()).hasSize(5)));
    }

    @Test
    @DisplayName("Rechaza User Agent con CR LF")
    void rejectsHeaderInjection() {
      // Act / Assert
      runner
          .withPropertyValues("whoscored.user-agent=agent\r\nAuthorization: secret")
          .run(context -> assertThat(context).hasFailed());
    }
  }
}
