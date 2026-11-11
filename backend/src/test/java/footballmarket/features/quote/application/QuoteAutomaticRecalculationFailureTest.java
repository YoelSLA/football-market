package footballmarket.features.quote.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import footballmarket.features.catalog.application.PlayerCatalogService;
import footballmarket.features.catalog.domain.models.player.Player;
import footballmarket.features.market.application.exceptions.MarketNotInitializedException;
import footballmarket.features.quote.application.exceptions.ExecutionAlreadyExistsException;
import footballmarket.features.quote.domain.enums.QuoteExecutionFailureReason;
import footballmarket.features.quote.domain.enums.QuoteExecutionOrigin;
import footballmarket.features.quote.domain.enums.QuoteExecutionResult;
import footballmarket.features.quote.domain.models.QuoteExecution;
import footballmarket.support.IdentityCatalogueFixture;
import footballmarket.support.QuoteTestData;
import footballmarket.support.ResetPlayerCatalogListener;
import footballmarket.support.TestcontainersConfiguration;
import footballmarket.support.ValuationTestData;
import java.time.Clock;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.bean.override.convention.TestBean;

@SpringBootTest
@ActiveProfiles("test")
@Import({
  TestcontainersConfiguration.class,
  IdentityCatalogueFixture.class,
  ValuationTestData.class,
  QuoteTestData.class
})
@TestExecutionListeners(
    listeners = ResetPlayerCatalogListener.class,
    mergeMode = TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS)
@Tag("integration")
class QuoteAutomaticRecalculationFailureTest {
  private static final LocalDate QUOTE_DATE = LocalDate.of(2026, 11, 4);
  private static final LocalDate MARKET_DATE = QUOTE_DATE.minusDays(1);
  private static final Instant NOW = Instant.parse("2026-11-04T09:00:00Z");

  @Autowired private QuoteRecalculationService service;
  @Autowired private QuoteMarketInitializationService initialization;
  @Autowired private PlayerCatalogService players;
  @Autowired private IdentityCatalogueFixture catalogue;
  @Autowired private ValuationTestData data;
  @Autowired private QuoteTestData quoteData;

  @TestBean(name = "marketClock", methodName = "controlledClock", enforceOverride = true)
  private Clock clock;

  static Clock controlledClock() {
    return new FailureClock();
  }

  @BeforeEach
  void resetClock() {
    ((FailureClock) clock).readsUntilFailure.set(-1);
  }

  @Nested
  @DisplayName("Errores automáticos y reintento según el registro existente")
  class AutomaticFailures {
    @Test
    @DisplayName("Un fallo de cierre no expone el contexto como resumen y permite otra corrida")
    void retriesManuallyWithoutFinishedSummary() {
      // Arrange: falla el reloj al cerrar el resumen, después de la reserva y del recorrido real.
      catalogue.prepare();
      data.prepareConfirmedJsonCampaign(MARKET_DATE);
      initialization.initialize(MARKET_DATE);
      ((FailureClock) clock).readsUntilFailure.set(2);
      // Act / Assert
      assertThatThrownBy(service::recalculateAutomatic).isInstanceOf(DateTimeException.class);
      assertThat(service.executions(QUOTE_DATE)).isEmpty();
      // Verify: el contexto sin cierre sigue fuera de la consulta; no se recupera ni compensa.
      assertThatThrownBy(service::recalculateAutomatic)
          .isInstanceOf(ExecutionAlreadyExistsException.class);
      QuoteExecution retry = service.recalculate("POSITIONAL_PERFORMANCE", null, null, null);
      assertThat(retry.getQuoteDate()).isEqualTo(QUOTE_DATE);
      assertThat(retry.getOrigin()).isEqualTo(QuoteExecutionOrigin.MANUAL);
      assertThat(retry.getPricedPlayers()).isEqualTo(5);
      assertThat(retry.getNotRepricedPlayers()).isZero();
      assertThat(retry.getCarriedForwardRows()).isEqualTo(5);
      assertThat(retry.getFailedPlayers()).isZero();
      assertThat(service.executions(QUOTE_DATE))
          .extracting(QuoteExecution::getId)
          .containsExactly(retry.getId());
    }

    @Test
    @DisplayName("Rechaza el mercado sin inicializar y permite reintentar tras inicializarlo")
    void rejectsUninitializedMarketWithoutConsumingDate() {
      // Arrange
      catalogue.prepare();
      data.prepareConfirmedJsonCampaign(MARKET_DATE);
      // Act / Assert
      assertThatThrownBy(service::recalculateAutomatic)
          .isInstanceOf(MarketNotInitializedException.class);
      assertThat(service.executions(QUOTE_DATE)).isEmpty();
      // Verify
      initialization.initialize(MARKET_DATE);
      QuoteExecution retry = service.recalculate("POSITIONAL_PERFORMANCE", null, null, null);
      assertThat(retry.getQuoteDate()).isEqualTo(QUOTE_DATE);
      assertThat(retry.getCarriedForwardRows()).isEqualTo(5);
      assertThat(retry.getFailedPlayers()).isZero();
    }

    @Test
    @DisplayName("Una automática con fallos impide otra automática pero admite reintento manual")
    void recordedIndividualFailuresAllowManualRetry() {
      // Arrange: cinco jugadores cotizables, pero solo cuatro tienen cotización inicial.
      catalogue.prepare();
      data.prepareConfirmedJsonCampaign(MARKET_DATE);
      List<Player> population = players.getActivePlayers(0, 10).getContent();
      for (Player player : population.subList(0, 4)) {
        quoteData.persistInitial(player, MARKET_DATE, NOW.minusSeconds(86400));
      }
      // Act
      QuoteExecution execution = service.recalculateAutomatic();
      // Assert
      assertThat(execution.getOrigin()).isEqualTo(QuoteExecutionOrigin.AUTOMATIC);
      assertThat(execution.getPricedPlayers()).isEqualTo(5);
      assertThat(execution.getCarriedForwardRows()).isEqualTo(4);
      assertThat(execution.getFailedPlayers()).isEqualTo(1);
      assertThat(execution.result()).isEqualTo(QuoteExecutionResult.FAILED);
      assertThat(execution.failureReason())
          .isEqualTo(QuoteExecutionFailureReason.INDIVIDUAL_PLAYER_FAILURES);
      // Verify
      assertThatThrownBy(service::recalculateAutomatic)
          .isInstanceOf(ExecutionAlreadyExistsException.class);
      quoteData.persistInitial(population.get(4), MARKET_DATE, NOW.minusSeconds(86400));
      QuoteExecution retry = service.recalculate("POSITIONAL_PERFORMANCE", null, null, null);
      assertThat(retry.getOrigin()).isEqualTo(QuoteExecutionOrigin.MANUAL);
      assertThat(retry.getId()).isNotEqualTo(execution.getId());
      assertThat(service.executions(QUOTE_DATE))
          .extracting(QuoteExecution::getId)
          .containsExactlyInAnyOrder(execution.getId(), retry.getId());
      assertThat(retry.getCarriedForwardRows()).isEqualTo(5);
      assertThat(retry.getNotRepricedPlayers()).isZero();
      assertThat(retry.getFailedPlayers()).isZero();
      assertThat(service.executions(QUOTE_DATE))
          .filteredOn(saved -> saved.getId().equals(execution.getId()))
          .singleElement()
          .satisfies(
              saved -> {
                assertThat(saved.getId()).isEqualTo(execution.getId());
                assertThat(saved.getOrigin()).isEqualTo(QuoteExecutionOrigin.AUTOMATIC);
                assertThat(saved.result()).isEqualTo(QuoteExecutionResult.FAILED);
                assertThat(saved.failureReason())
                    .isEqualTo(QuoteExecutionFailureReason.INDIVIDUAL_PLAYER_FAILURES);
              });
    }
  }

  /** Reloj de prueba que inyecta un fallo técnico sin sustituir servicios ni persistencia. */
  static class FailureClock extends Clock {
    private final AtomicInteger readsUntilFailure = new AtomicInteger(-1);

    @Override
    public ZoneId getZone() {
      return ZoneOffset.UTC;
    }

    @Override
    public Clock withZone(ZoneId zone) {
      return Clock.fixed(NOW, zone);
    }

    @Override
    public Instant instant() {
      if (readsUntilFailure.getAndUpdate(value -> value > 0 ? value - 1 : value) == 1) {
        throw new DateTimeException("Fallo técnico simulado al finalizar la corrida");
      }
      return NOW;
    }
  }
}
