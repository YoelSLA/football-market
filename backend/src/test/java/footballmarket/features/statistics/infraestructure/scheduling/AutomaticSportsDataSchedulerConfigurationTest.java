package footballmarket.features.statistics.infraestructure.scheduling;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import footballmarket.features.quote.application.QuoteRecalculationService;
import footballmarket.features.statistics.application.PlayerStatisticsSynchronizationService;
import footballmarket.features.valuation.application.ValuationEvaluationService;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.support.CronExpression;

@Tag("lightweight")
class AutomaticSportsDataSchedulerConfigurationTest {
  private final ApplicationContextRunner runner =
      new ApplicationContextRunner()
          .withBean(
              PlayerStatisticsSynchronizationService.class,
              () -> mock(PlayerStatisticsSynchronizationService.class))
          .withBean(ValuationEvaluationService.class, () -> mock(ValuationEvaluationService.class))
          .withBean(AutomaticSportsDataScheduler.class);

  @Nested
  @DisplayName("Programación semanal fija y aislamiento del perfil test")
  class SchedulingConfiguration {
    @Test
    @DisplayName("El cron anotado dispara el domingo a mediodía de Buenos Aires")
    void cronSemanalConZonaExplicita() throws NoSuchMethodException {
      // Arrange
      Scheduled scheduled =
          AutomaticSportsDataScheduler.class
              .getMethod("synchronize")
              .getAnnotation(Scheduled.class);
      ZoneId zone = ZoneId.of("America/Argentina/Buenos_Aires");
      ZonedDateTime before = ZonedDateTime.of(2026, 11, 7, 12, 0, 0, 0, zone);

      // Act
      ZonedDateTime next = CronExpression.parse(scheduled.cron()).next(before);

      // Assert
      assertThat(scheduled.cron()).isEqualTo("0 0 12 * * SUN");
      assertThat(scheduled.zone()).isEqualTo(zone.getId());
      assertThat(next).isEqualTo(ZonedDateTime.of(2026, 11, 8, 12, 0, 0, 0, zone));
    }

    @Test
    @DisplayName("Sin configuración adicional el disparador está habilitado")
    void habilitadoPorDefecto() {
      // Act / Assert
      runner
          .withBean(QuoteRecalculationService.class, () -> mock(QuoteRecalculationService.class))
          .run(
              context -> {
                assertThat(context).hasNotFailed();
                context.getBean(AutomaticSportsDataScheduler.class).synchronize();
                verify(context.getBean(ValuationEvaluationService.class)).evaluate();
                verifyNoInteractions(context.getBean(QuoteRecalculationService.class));
              });
    }

    @Test
    @DisplayName("El YAML real del perfil test mantiene inerte el disparador")
    void perfilTestNoEjecutaOperaciones() {
      // Act / Assert
      runner
          .withInitializer(new ConfigDataApplicationContextInitializer())
          .withPropertyValues(
              "spring.profiles.active=test",
              "spring.config.location=classpath:/application-test.yml")
          .run(
              context -> {
                assertThat(context).hasNotFailed();
                context.getBean(AutomaticSportsDataScheduler.class).synchronize();
                verifyNoInteractions(
                    context.getBean(PlayerStatisticsSynchronizationService.class),
                    context.getBean(ValuationEvaluationService.class));
              });
    }
  }
}
