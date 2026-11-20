package footballmarket.features.quote.infraestructure.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import footballmarket.features.quote.application.QuoteRecalculationService;
import footballmarket.features.quote.infraestructure.scheduling.AutomaticQuoteRecalculationScheduler;
import footballmarket.features.valuation.domain.enums.ValuationStrategy;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Tag("lightweight")
class QuotePropertiesTest {
  private final ApplicationContextRunner runner =
      new ApplicationContextRunner().withUserConfiguration(PropertiesConfiguration.class);

  @Nested
  @DisplayName("Configuración del recálculo automático")
  class AutomaticConfiguration {
    @Test
    @DisplayName("Usa estrategia y cadencia diaria a las 12 UTC por defecto")
    void bindsDefaults() {
      // Act / Assert
      runner.run(
          context -> {
            assertThat(context).hasNotFailed();
            QuoteProperties properties = context.getBean(QuoteProperties.class);
            assertThat(properties.strategy()).isEqualTo(ValuationStrategy.POSITIONAL_PERFORMANCE);
            assertThat(properties.schedule().cron()).isEqualTo("0 0 12 * * *");
            assertThat(properties.schedule().enabled()).isTrue();
            assertThat(properties.adjustmentFactor()).isEqualByComparingTo("0.10");
            assertThat(properties.variationCap()).isEqualByComparingTo("0.10");
            assertThat(properties.minPriceFloor()).isEqualTo(1L);
            assertThat(properties.initialAnchor()).isEqualTo(100L);
          });
    }

    @Test
    @DisplayName("Sobrescribe estrategia, parámetros y cadencia desde propiedades")
    void overridesConfiguration() {
      // Act / Assert
      runner
          .withPropertyValues(
              "market.quote.strategy=GENERAL_ATTRIBUTES",
              "market.quote.adjustment-factor=0.25",
              "market.quote.variation-cap=0.20",
              "market.quote.min-price-floor=7",
              "market.quote.schedule.cron=0 30 2 * * TUE",
              "market.quote.schedule.enabled=false")
          .run(
              context -> {
                assertThat(context).hasNotFailed();
                QuoteProperties properties = context.getBean(QuoteProperties.class);
                assertThat(properties.strategy()).isEqualTo(ValuationStrategy.GENERAL_ATTRIBUTES);
                assertThat(properties.adjustmentFactor()).isEqualByComparingTo("0.25");
                assertThat(properties.variationCap()).isEqualByComparingTo("0.20");
                assertThat(properties.minPriceFloor()).isEqualTo(7L);
                assertThat(properties.schedule().cron()).isEqualTo("0 30 2 * * TUE");
                assertThat(properties.schedule().enabled()).isFalse();
              });
    }

    @Test
    @DisplayName("Impide arrancar con una estrategia fuera del catálogo")
    void rejectsUnknownStrategy() {
      // Act / Assert
      runner
          .withPropertyValues("market.quote.strategy=UNKNOWN")
          .run(context -> assertThat(context).hasFailed());
    }

    @Test
    @DisplayName("El disparador impide arrancar con una expresión cron inválida")
    void rejectsInvalidCronAtStartup() {
      // Act / Assert
      runner
          .withUserConfiguration(SchedulingConfiguration.class)
          .withBean(QuoteRecalculationService.class, () -> mock(QuoteRecalculationService.class))
          .withBean(AutomaticQuoteRecalculationScheduler.class)
          .withPropertyValues("market.quote.schedule.cron=invalid")
          .run(
              context -> {
                assertThat(context).hasFailed();
                assertThat(context.getStartupFailure())
                    .hasRootCauseInstanceOf(IllegalStateException.class)
                    .rootCause()
                    .hasMessageContaining("Cron expression must consist of 6 fields");
              });
    }
  }

  @Configuration(proxyBeanMethods = false)
  @EnableConfigurationProperties(QuoteProperties.class)
  static class PropertiesConfiguration {}

  @Configuration(proxyBeanMethods = false)
  @EnableScheduling
  static class SchedulingConfiguration {}
}
