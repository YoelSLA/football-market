package footballmarket.config;

import static org.assertj.core.api.Assertions.assertThat;

import footballmarket.integrations.exceptions.InvalidFootballDataConfigurationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

class FootballDataPropertiesTest {
  private final ApplicationContextRunner runner =
      new ApplicationContextRunner()
          .withUserConfiguration(PropertiesConfiguration.class)
          .withPropertyValues(
              "football-data.api-key=test-only", "football-data.base-url=https://provider.example");

  @Configuration(proxyBeanMethods = false)
  @EnableConfigurationProperties(FootballDataProperties.class)
  static class PropertiesConfiguration {}

  @Nested
  @DisplayName("Configuración de ligas al iniciar")
  class Competitions {
    @ParameterizedTest
    @DisplayName("Inicia con cualquier lista no vacía de ligas únicas, conservando el orden")
    @ValueSource(
        strings = {"PL,BL1,PD,SA,FL1", "BL1,PL,PD,SA,FL1", "PL", "PL,CL", "PL,BL1,PD,SA,FL1,CL"})
    void acceptsUniqueCompetitions(String competitions) {
      runner
          .withPropertyValues("football-data.competitions=" + competitions)
          .run(
              context -> {
                assertThat(context).hasNotFailed();
                assertThat(context.getBean(FootballDataProperties.class).competitions())
                    .containsExactly(competitions.split(","));
              });
    }

    @ParameterizedTest
    @DisplayName("Impide el arranque con una lista vacía o códigos duplicados")
    @ValueSource(strings = {"", "PL,BL1,PD,SA,FL1,FL1", "PL,BL1,PD,SA,SA", "PL,PL"})
    void rechazaLigasInvalidasAlIniciar(String competitions) {
      runner
          .withPropertyValues("football-data.competitions=" + competitions)
          .run(
              context ->
                  assertThat(context.getStartupFailure())
                      .hasRootCauseInstanceOf(InvalidFootballDataConfigurationException.class));
    }

    @Test
    @DisplayName("Impide el arranque cuando falta la configuración de ligas")
    void rechazaAusenciaDeLigasAlIniciar() {
      runner.run(
          context ->
              assertThat(context.getStartupFailure())
                  .hasRootCauseInstanceOf(InvalidFootballDataConfigurationException.class));
    }
  }
}
