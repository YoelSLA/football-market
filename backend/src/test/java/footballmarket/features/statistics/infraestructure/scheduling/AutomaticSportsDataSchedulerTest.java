package footballmarket.features.statistics.infraestructure.scheduling;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import footballmarket.features.statistics.application.PlayerStatisticsSynchronizationService;
import footballmarket.features.statistics.application.exceptions.ScheduledStatisticsSyncException;
import footballmarket.features.statistics.application.exceptions.ScheduledStatisticsSyncException.Reason;
import footballmarket.features.statistics.domain.records.PlayerStatisticsSyncRequest;
import footballmarket.features.valuation.application.ValuationEvaluationService;
import footballmarket.features.valuation.application.exceptions.ValuationSnapshotUnavailableException;
import footballmarket.features.valuation.domain.records.ValuationEvaluationSummary;
import footballmarket.integrations.whoscored.exceptions.HeadedBrowserUnavailableException;
import java.time.LocalDate;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InOrder;
import org.slf4j.LoggerFactory;

@Tag("unit")
class AutomaticSportsDataSchedulerTest {
  private final PlayerStatisticsSynchronizationService statistics =
      mock(PlayerStatisticsSynchronizationService.class);
  private final ValuationEvaluationService evaluation = mock(ValuationEvaluationService.class);
  private final AutomaticSportsDataScheduler scheduler =
      new AutomaticSportsDataScheduler(statistics, evaluation, true);
  private final ListAppender<ILoggingEvent> events = new ListAppender<>();
  private final Logger logger =
      (Logger) LoggerFactory.getLogger(AutomaticSportsDataScheduler.class);

  @BeforeEach
  void captureEvents() {
    events.start();
    logger.addAppender(events);
  }

  @AfterEach
  void releaseEvents() {
    logger.detachAppender(events);
    events.stop();
    Thread.interrupted();
  }

  @Nested
  @DisplayName("Secuencia deportiva semanal")
  class WeeklyExecution {
    @Test
    @DisplayName("Espera cada etapa y ejecuta JSON, scraping con fecha propia y evaluación")
    void ejecutaEtapasEnOrden() {
      // Arrange
      PlayerStatisticsSyncRequest request = new PlayerStatisticsSyncRequest(null);
      ValuationEvaluationSummary summary =
          new ValuationEvaluationSummary(LocalDate.of(2026, 10, 15), 4, 1, Map.of(), Map.of());
      when(statistics.synchronizeJsonScheduled())
          .thenAnswer(
              invocation -> {
                verifyNoInteractions(evaluation);
                return false;
              });
      when(statistics.synchronizeScrapingScheduled(request))
          .thenAnswer(
              invocation -> {
                verifyNoInteractions(evaluation);
                return false;
              });
      when(evaluation.evaluate()).thenReturn(summary);

      // Act
      scheduler.synchronize();

      // Verify: solo las fachadas automáticas; no hay recálculo ni operación manual adicional.
      InOrder order = inOrder(statistics, evaluation);
      order.verify(statistics).synchronizeJsonScheduled();
      order.verify(statistics).synchronizeScrapingScheduled(request);
      order.verify(evaluation).evaluate();
      verifyNoMoreInteractions(statistics, evaluation);
      assertThat(events.list)
          .extracting(ILoggingEvent::getFormattedMessage)
          .containsExactly(
              "sports_data_scheduled_started", "sports_data_scheduled_finished result=COMPLETED");
    }

    @ParameterizedTest
    @CsvSource({"true,false", "false,true", "true,true"})
    @DisplayName("Un parcial sin fallo global permite terminar las tres etapas")
    void continuaTrasParcial(boolean jsonPartial, boolean scrapingPartial) {
      // Arrange
      when(statistics.synchronizeJsonScheduled()).thenReturn(jsonPartial);
      when(statistics.synchronizeScrapingScheduled(any())).thenReturn(scrapingPartial);

      // Act
      scheduler.synchronize();

      // Verify
      verify(evaluation).evaluate();
      assertThat(events.list)
          .extracting(ILoggingEvent::getFormattedMessage)
          .contains("sports_data_scheduled_finished result=PARTIAL");
    }

    @Test
    @DisplayName("El disparador deshabilitado no invoca operaciones ni emite eventos")
    void deshabilitadoNoEjecuta() {
      // Arrange
      AutomaticSportsDataScheduler disabled =
          new AutomaticSportsDataScheduler(statistics, evaluation, false);

      // Act
      disabled.synchronize();

      // Verify
      verifyNoInteractions(statistics, evaluation);
      assertThat(events.list).isEmpty();
    }
  }

  @Nested
  @DisplayName("Fallos globales y disponibilidad del disparador")
  class Failures {
    @Test
    @DisplayName("El doble fallo scraping registra cierre fallido sin filtrar causas ni evaluar")
    void scrapingRegistraDobleFalloSeguro() {
      // Arrange
      ScheduledStatisticsSyncException failure =
          new ScheduledStatisticsSyncException(Reason.HEADED_BROWSER_UNAVAILABLE);
      failure.initCause(new HeadedBrowserUnavailableException("token=private-browser"));
      failure.addSuppressed(new IllegalStateException("password=private-close"));
      when(statistics.synchronizeScrapingScheduled(any())).thenThrow(failure);

      // Act / Assert
      assertThatCode(scheduler::synchronize).doesNotThrowAnyException();

      // Verify
      verify(statistics).synchronizeJsonScheduled();
      verify(statistics).synchronizeScrapingScheduled(new PlayerStatisticsSyncRequest(null));
      verifyNoMoreInteractions(statistics);
      verifyNoInteractions(evaluation);
      assertThat(events.list)
          .extracting(ILoggingEvent::getFormattedMessage)
          .contains(
              "sports_data_scheduled_failed stage=SCRAPING cause=HEADED_BROWSER_UNAVAILABLE closeFailed=true")
          .noneMatch(message -> message.contains("private-"));
      assertThat(events.list).allMatch(event -> event.getThrowableProxy() == null);
    }

    @Test
    @DisplayName("Un fallo del cierre se comunica sin afirmar disponibilidad ni filtrar su detalle")
    void comunicaFalloDeCierre() {
      // Arrange
      ScheduledStatisticsSyncException failure =
          new ScheduledStatisticsSyncException(Reason.EXECUTION_FAILED);
      failure.initCause(new IllegalStateException("token=private-original"));
      failure.addSuppressed(new IllegalStateException("password=private-close"));
      when(statistics.synchronizeJsonScheduled()).thenThrow(failure);

      // Act
      scheduler.synchronize();

      // Verify
      assertThat(events.list)
          .extracting(ILoggingEvent::getFormattedMessage)
          .contains(
              "sports_data_scheduled_failed stage=JSON cause=EXECUTION_FAILED closeFailed=true")
          .noneMatch(message -> message.contains("private-"));
      assertThat(events.list).allMatch(event -> event.getThrowableProxy() == null);
      verifyNoInteractions(evaluation);
    }

    @Test
    @DisplayName("Una interrupción al retornar JSON no permite iniciar scraping")
    void interrupcionEntreEtapasDetieneSecuencia() {
      // Arrange
      when(statistics.synchronizeJsonScheduled())
          .thenAnswer(
              invocation -> {
                Thread.currentThread().interrupt();
                return false;
              });

      // Act
      scheduler.synchronize();

      // Assert
      assertThat(Thread.currentThread().isInterrupted()).isTrue();
      // Verify
      verify(statistics).synchronizeJsonScheduled();
      verifyNoMoreInteractions(statistics);
      verifyNoInteractions(evaluation);
    }

    @Test
    @DisplayName("Una causa InterruptedException de evaluación restaura el indicador del hilo")
    void interrupcionAnidadaDeEvaluacion() {
      // Arrange
      IllegalStateException failure = new IllegalStateException(new InterruptedException());
      when(evaluation.evaluate()).thenThrow(failure);

      // Act
      scheduler.synchronize();

      // Assert
      assertThat(Thread.currentThread().isInterrupted()).isTrue();
    }

    @Test
    @DisplayName("Un fallo JSON impide scraping y evaluación y permite un disparo posterior")
    void falloJsonNoDeshabilitaFuturosDisparos() {
      // Arrange
      ScheduledStatisticsSyncException failure =
          new ScheduledStatisticsSyncException(Reason.PROVIDER_UNAVAILABLE);
      when(statistics.synchronizeJsonScheduled()).thenThrow(failure).thenReturn(false);

      // Act / Assert
      assertThatCode(scheduler::synchronize).doesNotThrowAnyException();

      // Verify
      verify(statistics).synchronizeJsonScheduled();
      verifyNoMoreInteractions(statistics);
      verifyNoInteractions(evaluation);
      assertThat(events.list)
          .extracting(ILoggingEvent::getFormattedMessage)
          .contains(
              "sports_data_scheduled_failed stage=JSON cause=PROVIDER_UNAVAILABLE closeFailed=false");

      // Act
      scheduler.synchronize();

      // Verify
      verify(evaluation).evaluate();
      assertThat(events.list)
          .extracting(ILoggingEvent::getFormattedMessage)
          .contains("sports_data_scheduled_finished result=COMPLETED");
    }

    @Test
    @DisplayName("Un fallo scraping detiene la evaluación y conserva su motivo seguro")
    void falloScrapingDetieneEvaluacion() {
      // Arrange
      ScheduledStatisticsSyncException failure =
          new ScheduledStatisticsSyncException(Reason.HEADED_BROWSER_UNAVAILABLE);
      when(statistics.synchronizeScrapingScheduled(any())).thenThrow(failure);

      // Act / Assert
      assertThatCode(scheduler::synchronize).doesNotThrowAnyException();

      // Verify
      verify(statistics).synchronizeJsonScheduled();
      verify(statistics).synchronizeScrapingScheduled(new PlayerStatisticsSyncRequest(null));
      verifyNoInteractions(evaluation);
      assertThat(events.list)
          .extracting(ILoggingEvent::getFormattedMessage)
          .contains(
              "sports_data_scheduled_failed stage=SCRAPING cause=HEADED_BROWSER_UNAVAILABLE closeFailed=false");
    }

    @Test
    @DisplayName("La evaluación sin snapshot registra su identidad sin propagar el rechazo")
    void falloEvaluacionComunicaIdentidad() {
      // Arrange
      when(evaluation.evaluate()).thenThrow(new ValuationSnapshotUnavailableException());

      // Act / Assert
      assertThatCode(scheduler::synchronize).doesNotThrowAnyException();

      // Verify
      assertThat(events.list)
          .extracting(ILoggingEvent::getFormattedMessage)
          .contains(
              "sports_data_scheduled_failed stage=EVALUATION cause=ValuationSnapshotUnavailableException closeFailed=false");
    }

    @Test
    @DisplayName("El log omite mensajes, causas privadas, excepciones suprimidas y trazas")
    void erroresNoFiltranSecretos() {
      // Arrange
      IllegalStateException failure = new IllegalStateException("token=private-token");
      failure.initCause(new RuntimeException("password=private-password"));
      failure.addSuppressed(new RuntimeException("url=private-url"));
      when(statistics.synchronizeJsonScheduled()).thenThrow(failure);

      // Act
      scheduler.synchronize();

      // Verify
      assertThat(events.list)
          .extracting(ILoggingEvent::getFormattedMessage)
          .contains(
              "sports_data_scheduled_failed stage=JSON cause=IllegalStateException closeFailed=false")
          .noneMatch(message -> message.contains("private-") || message.contains("password="));
      assertThat(events.list).allMatch(event -> event.getThrowableProxy() == null);
    }

    @Test
    @DisplayName("La señal de interrupción restaura el hilo y suprime etapas posteriores")
    void conservaInterrupcion() {
      // Arrange
      ScheduledStatisticsSyncException failure =
          new ScheduledStatisticsSyncException(Reason.EXECUTION_INTERRUPTED);
      when(statistics.synchronizeJsonScheduled()).thenThrow(failure);

      // Act
      scheduler.synchronize();

      // Assert
      assertThat(Thread.currentThread().isInterrupted()).isTrue();
      // Verify
      verifyNoInteractions(evaluation);
      verify(statistics).synchronizeJsonScheduled();
      verifyNoMoreInteractions(statistics);
    }

    @Test
    @DisplayName("Un hilo ya interrumpido no inicia llamadas de sincronización")
    void noIniciaConHiloInterrumpido() {
      // Arrange
      Thread.currentThread().interrupt();

      // Act
      scheduler.synchronize();

      // Assert
      assertThat(Thread.currentThread().isInterrupted()).isTrue();
      // Verify
      verifyNoInteractions(statistics, evaluation);
      assertThat(events.list)
          .extracting(ILoggingEvent::getFormattedMessage)
          .contains(
              "sports_data_scheduled_failed stage=JSON cause=EXECUTION_INTERRUPTED closeFailed=false");
    }
  }
}
