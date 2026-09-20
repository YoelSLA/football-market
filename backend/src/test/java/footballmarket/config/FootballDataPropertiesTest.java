package footballmarket.config;

import static org.assertj.core.api.Assertions.assertThat;

import footballmarket.integrations.exceptions.InvalidFootballDataConfigurationException;
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

  @ParameterizedTest
  @ValueSource(strings = {"PL,BL1,PD,SA,FL1", "BL1,PL,PD,SA,FL1", "FL1,SA,PD,BL1,PL"})
  void startsWithExactlyTheFiveLeagues(String competitions) {
    this.runner
        .withPropertyValues("football-data.competitions=" + competitions)
        .run(
            context -> {
              assertThat(context).hasNotFailed();
              assertThat(context.getBean(FootballDataProperties.class).competitions())
                  .containsExactly(competitions.split(","));
            });
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "",
        "PL,BL1,PD,SA",
        "PL,BL1,PD,SA,FL1,CL",
        "PL,BL1,PD,SA,FL1,FL1",
        "PL,BL1,PD,SA,SA",
        "PL,BL1,PD,SA,CL"
      })
  void rejectsInvalidLeaguesDuringStartup(String competitions) {
    this.runner
        .withPropertyValues("football-data.competitions=" + competitions)
        .run(
            context ->
                assertThat(context.getStartupFailure())
                    .hasRootCauseInstanceOf(InvalidFootballDataConfigurationException.class));
  }

  @Test
  void rejectsMissingLeaguesDuringStartup() {
    this.runner.run(
        context ->
            assertThat(context.getStartupFailure())
                .hasRootCauseInstanceOf(InvalidFootballDataConfigurationException.class));
  }

  @Configuration(proxyBeanMethods = false)
  @EnableConfigurationProperties(FootballDataProperties.class)
  static class PropertiesConfiguration {}
}
