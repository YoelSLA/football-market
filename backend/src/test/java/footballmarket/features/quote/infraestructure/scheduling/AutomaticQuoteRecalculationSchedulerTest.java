package footballmarket.features.quote.infraestructure.scheduling;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import footballmarket.features.market.application.exceptions.MarketNotInitializedException;
import footballmarket.features.quote.application.QuoteRecalculationService;
import footballmarket.features.quote.infraestructure.config.QuoteProperties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

@Tag("unit")
class AutomaticQuoteRecalculationSchedulerTest {
  private final QuoteRecalculationService service = mock(QuoteRecalculationService.class);
  private final ListAppender<ILoggingEvent> events = new ListAppender<>();
  private final Logger logger =
      (Logger) LoggerFactory.getLogger(AutomaticQuoteRecalculationScheduler.class);

  @BeforeEach
  void captureEvents() {
    events.start();
    logger.addAppender(events);
  }

  @AfterEach
  void releaseEvents() {
    logger.detachAppender(events);
    events.stop();
  }

  @Nested
  @DisplayName("Disparos automáticos")
  class AutomaticTriggers {
    @Test
    @DisplayName("Registra el mercado no inicializado sin propagar el rechazo")
    void rejectsUninitializedMarket() {
      // Arrange
      when(service.recalculateAutomatic()).thenThrow(new MarketNotInitializedException());
      AutomaticQuoteRecalculationScheduler scheduler = scheduler(true);
      // Act / Assert
      assertThatCode(scheduler::recalculate).doesNotThrowAnyException();
      // Verify
      assertThat(events.list)
          .extracting(ILoggingEvent::getFormattedMessage)
          .anyMatch(
              message ->
                  message.startsWith("quote_automatic_recalculation_rejected ")
                      && message.endsWith("reason=MARKET_NOT_INITIALIZED"));
    }

    @Test
    @DisplayName("Registra la clase del fallo y permite atender el siguiente disparo")
    void failureDoesNotDisableNextTrigger() {
      // Arrange
      when(service.recalculateAutomatic())
          .thenThrow(new IllegalStateException("detalle privado"))
          .thenReturn(null);
      AutomaticQuoteRecalculationScheduler scheduler = scheduler(true);
      // Act / Assert
      assertThatCode(scheduler::recalculate).doesNotThrowAnyException();
      assertThatCode(scheduler::recalculate).doesNotThrowAnyException();
      // Verify
      verify(service, org.mockito.Mockito.times(2)).recalculateAutomatic();
      assertThat(events.list)
          .extracting(ILoggingEvent::getFormattedMessage)
          .anyMatch(
              message ->
                  message.startsWith("quote_automatic_recalculation_failed ")
                      && message.endsWith("cause=IllegalStateException"))
          .noneMatch(message -> message.contains("detalle privado"));
    }

    @Test
    @DisplayName("Invoca el caso de uso y registra el disparo cuando está habilitado")
    void triggersAutomaticRecalculation() {
      // Arrange
      AutomaticQuoteRecalculationScheduler scheduler = scheduler(true);
      // Act
      scheduler.recalculate();
      // Verify
      verify(service).recalculateAutomatic();
      assertThat(events.list)
          .extracting(ILoggingEvent::getFormattedMessage)
          .anyMatch(
              message -> message.startsWith("quote_automatic_recalculation_triggered quoteDate="));
    }

    @Test
    @DisplayName("Permanece silencioso y no invoca el servicio cuando está deshabilitado")
    void disabledTriggerDoesNothing() {
      // Arrange
      AutomaticQuoteRecalculationScheduler scheduler = scheduler(false);
      // Act
      scheduler.recalculate();
      // Verify
      verifyNoInteractions(service);
      assertThat(events.list).isEmpty();
    }

    @Test
    @DisplayName("Cada disparo del mismo día vuelve a delegar sin exclusión por fecha")
    void delegatesRepeatedTriggers() {
      // Arrange
      AutomaticQuoteRecalculationScheduler scheduler = scheduler(true);
      // Act / Assert
      assertThatCode(scheduler::recalculate).doesNotThrowAnyException();
      assertThatCode(scheduler::recalculate).doesNotThrowAnyException();
      // Verify
      verify(service, org.mockito.Mockito.times(2)).recalculateAutomatic();
    }
  }

  private AutomaticQuoteRecalculationScheduler scheduler(boolean enabled) {
    QuoteProperties.Schedule schedule = new QuoteProperties.Schedule(null, enabled);
    QuoteProperties properties = new QuoteProperties(null, null, null, null, null, schedule);
    return new AutomaticQuoteRecalculationScheduler(service, properties);
  }
}
