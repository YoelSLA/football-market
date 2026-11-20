package footballmarket.features.quote.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import footballmarket.features.catalog.application.PlayerCatalogService;
import footballmarket.features.catalog.domain.models.player.Player;
import footballmarket.features.market.application.exceptions.MarketNotInitializedException;
import footballmarket.features.quote.application.exceptions.ExecutionAlreadyExistsException;
import footballmarket.features.quote.application.exceptions.InvalidAdjustmentFactorException;
import footballmarket.features.quote.application.exceptions.UnknownQuoteStrategyException;
import footballmarket.features.quote.domain.enums.QuoteExecutionOrigin;
import footballmarket.features.quote.domain.enums.QuoteExecutionResult;
import footballmarket.features.quote.domain.models.QuoteExecution;
import footballmarket.features.valuation.application.ValuationEvaluationService;
import footballmarket.support.IdentityCatalogueFixture;
import footballmarket.support.ResetPlayerCatalogListener;
import footballmarket.support.ScoreRankingTestData;
import footballmarket.support.TestcontainersConfiguration;
import footballmarket.support.ValuationTestData;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
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

@SpringBootTest(
    properties = {
      "market.quote.strategy=GENERAL_ATTRIBUTES", "market.quote.adjustment-factor=0.25",
      "market.quote.variation-cap=0.20", "market.quote.min-price-floor=7"
    })
@ActiveProfiles("test")
@Import({
  TestcontainersConfiguration.class,
  IdentityCatalogueFixture.class,
  ValuationTestData.class,
  ScoreRankingTestData.class
})
@TestExecutionListeners(
    listeners = ResetPlayerCatalogListener.class,
    mergeMode = TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS)
@Tag("integration")
class QuoteRecalculationServiceTest {
  private static final LocalDate QUOTE_DATE = LocalDate.of(2026, 10, 19);
  private static final Instant NOW = Instant.parse("2026-10-19T09:00:00Z");
  @Autowired private QuoteRecalculationService service;
  @Autowired private QuoteMarketInitializationService initialization;
  @Autowired private ValuationEvaluationService valuation;
  @Autowired private PlayerCatalogService players;
  @Autowired private IdentityCatalogueFixture catalogue;
  @Autowired private ValuationTestData data;
  @Autowired private ScoreRankingTestData scores;

  @TestBean(name = "statisticsClock", methodName = "valuationClock")
  private Clock statisticsClock;

  @TestBean(name = "marketClock", methodName = "quoteClock")
  private Clock marketClock;

  static Clock valuationClock() {
    return Clock.fixed(NOW, ZoneOffset.UTC);
  }

  static Clock quoteClock() {
    return new ControlledClock();
  }

  @BeforeEach
  void resetClock() {
    ((ControlledClock) marketClock).time.set(NOW);
    ((ControlledClock) marketClock).nextRead = null;
  }

  @Nested
  @DisplayName("Recálculo diario por jugador")
  class DailyRecalculation {
    @Test
    @DisplayName("Un score neutro existente es recalculado, no arrastrado")
    void neutralScoreIsRecomputed() {
      // Arrange
      prepareMarket(false);
      Player player = players.getActivePlayers(0, 10).getContent().getFirst();
      scores.preparePositionalValuation(player, QUOTE_DATE.minusDays(1), new BigDecimal("0.5"));
      // Act
      QuoteExecution result = service.recalculate("POSITIONAL_PERFORMANCE", null, null, null);
      // Assert
      assertThat(result.getRecomputedRows()).isEqualTo(1);
      assertThat(result.getCarriedForwardRows()).isEqualTo(4);
      assertThat(result.getFailedPlayers()).isZero();
    }

    @Test
    @DisplayName("Registra manuales antes y después de una automática y rechaza otra automática")
    void acceptsRepeatedRun() {
      // Arrange
      prepareMarket(true);
      // Act
      QuoteExecution first = service.recalculate("POSITIONAL_PERFORMANCE", null, null, null);
      QuoteExecution automatic = service.recalculateAutomatic();
      QuoteExecution last = service.recalculate("POSITIONAL_PERFORMANCE", null, null, null);
      // Assert
      assertThat(first.getRecomputedRows()).isEqualTo(5);
      assertThat(automatic.getRecomputedRows()).isEqualTo(5);
      assertThat(last.getRecomputedRows()).isEqualTo(5);
      assertThat(last.getNotRepricedPlayers()).isZero();
      assertThat(last.result()).isEqualTo(QuoteExecutionResult.SUCCESS);
      assertThatThrownBy(service::recalculateAutomatic)
          .isInstanceOf(ExecutionAlreadyExistsException.class);
      assertThat(service.executions(QUOTE_DATE))
          .extracting(QuoteExecution::getId)
          .containsExactly(last.getId(), automatic.getId(), first.getId());
    }

    @Test
    @DisplayName("Reutiliza el mismo score en días sucesivos en vez de congelar el precio")
    void reusesOldScoreNextDay() {
      // Arrange
      prepareMarket(true);
      service.recalculate("POSITIONAL_PERFORMANCE", null, null, null);
      ((ControlledClock) marketClock).time.set(NOW.plusSeconds(86400));
      // Act
      QuoteExecution next = service.recalculate("POSITIONAL_PERFORMANCE", null, null, null);
      // Assert
      assertThat(next.getQuoteDate()).isEqualTo(QUOTE_DATE.plusDays(1));
      assertThat(next.getRecomputedRows()).isEqualTo(5);
      assertThat(next.getNotRepricedPlayers()).isZero();
      assertThat(next.getFailedPlayers()).isZero();
    }

    @Test
    @DisplayName("Sin score arrastra diariamente y audita la configuración automática")
    void carriesWithoutScore() {
      // Arrange
      prepareMarket(false);
      // Act
      QuoteExecution result = service.recalculateAutomatic();
      // Assert
      assertThat(result.getCarriedForwardRows()).isEqualTo(5);
      assertThat(result.getOrigin()).isEqualTo(QuoteExecutionOrigin.AUTOMATIC);
      assertThat(result.getAdjustmentFactor()).isEqualByComparingTo("0.25");
      assertThat(result.getVariationCap()).isEqualByComparingTo("0.20");
      assertThat(result.getMinPriceFloor()).isEqualTo(7);
      assertThat(result.getFailedPlayers()).isZero();
    }

    @Test
    @DisplayName("Recalcular INITIAL no duplica ni desinicializa el mercado")
    void updatesInitialDay() {
      // Arrange
      catalogue.prepare();
      data.prepareConfirmedJsonCampaign(QUOTE_DATE.minusDays(1));
      valuation.evaluate();
      initialization.initialize(QUOTE_DATE);
      // Act
      QuoteExecution first = service.recalculateAutomatic();
      QuoteExecution next = service.recalculate("POSITIONAL_PERFORMANCE", null, null, null);
      // Assert
      assertThat(first.getRecomputedRows()).isEqualTo(5);
      assertThat(next.getRecomputedRows()).isEqualTo(5);
      assertThat(next.getFailedPlayers()).isZero();
    }

    @Test
    @DisplayName("Una valuación llegada después reemplaza la cotización arrastrada del día")
    void lateScoreReplacesDailyQuote() {
      // Arrange
      prepareMarket(false);
      service.recalculate("POSITIONAL_PERFORMANCE", null, null, null);
      valuation.evaluate();
      // Act
      QuoteExecution result = service.recalculate("POSITIONAL_PERFORMANCE", null, null, null);
      // Assert
      assertThat(result.getRecomputedRows()).isEqualTo(5);
      assertThat(result.getNotRepricedPlayers()).isZero();
      assertThat(result.getFailedPlayers()).isZero();
    }

    @Test
    @DisplayName(
        "Dos corridas concurrentes publican cálculos completos sin duplicar la fila diaria")
    void concurrentRunsPublishBothCalculations() throws Exception {
      // Arrange
      prepareMarket(true);
      CountDownLatch start = new CountDownLatch(1);
      ExecutorService executor = Executors.newFixedThreadPool(2);
      try {
        Future<QuoteExecution> manual = executor.submit(() -> runAfter(start, false));
        Future<QuoteExecution> automatic = executor.submit(() -> runAfter(start, true));
        // Act
        start.countDown();
        QuoteExecution first = manual.get(30, TimeUnit.SECONDS);
        QuoteExecution second = automatic.get(30, TimeUnit.SECONDS);
        // Assert
        assertThat(first.getId()).isNotEqualTo(second.getId());
        assertThat(first.getRecomputedRows() + second.getRecomputedRows()).isEqualTo(10);
        assertThat(first.getNotRepricedPlayers() + second.getNotRepricedPlayers()).isZero();
        assertThat(first.getFailedPlayers() + second.getFailedPlayers()).isZero();
        assertThat(service.executions(QUOTE_DATE)).hasSize(2);
      } finally {
        executor.shutdownNow();
      }
    }

    @Test
    @DisplayName("Fija el día UTC inicial aunque el cierre cruce medianoche")
    void fixesUtcDayAtStart() {
      // Arrange
      prepareMarket(false);
      ControlledClock clock = (ControlledClock) marketClock;
      clock.time.set(Instant.parse("2026-10-19T23:59:59Z"));
      clock.nextRead = Instant.parse("2026-10-20T00:00:01Z");
      // Act
      QuoteExecution result = service.recalculateAutomatic();
      // Assert
      assertThat(result.getQuoteDate()).isEqualTo(QUOTE_DATE);
      assertThat(result.getStartedAt()).isEqualTo(Instant.parse("2026-10-19T23:59:59Z"));
      assertThat(result.getFinishedAt()).isEqualTo(Instant.parse("2026-10-20T00:00:01Z"));
      assertThat(result.getCarriedForwardRows()).isEqualTo(5);
    }

    @Test
    @DisplayName("Conserva la precedencia de estrategia, mercado y factor")
    void preservesRejectionPrecedence() {
      // Act / Assert
      assertThatThrownBy(() -> service.recalculate("UNKNOWN", new BigDecimal("2"), null, null))
          .isInstanceOf(UnknownQuoteStrategyException.class);
      assertThatThrownBy(
              () -> service.recalculate("POSITIONAL_PERFORMANCE", new BigDecimal("2"), null, null))
          .isInstanceOf(MarketNotInitializedException.class);
      // Arrange
      prepareMarket(false);
      // Act / Assert
      assertThatThrownBy(
              () -> service.recalculate("POSITIONAL_PERFORMANCE", new BigDecimal("2"), null, null))
          .isInstanceOf(InvalidAdjustmentFactorException.class);
      assertThat(service.executions(QUOTE_DATE)).isEmpty();
    }
  }

  private void prepareMarket(boolean valued) {
    catalogue.prepare();
    data.prepareConfirmedJsonCampaign(QUOTE_DATE.minusDays(1));
    if (valued) {
      valuation.evaluate();
    }
    initialization.initialize(QUOTE_DATE.minusDays(2));
  }

  private QuoteExecution runAfter(CountDownLatch start, boolean automatic) throws Exception {
    if (!start.await(10, TimeUnit.SECONDS)) {
      throw new AssertionError("No se liberó la barrera de inicio");
    }
    return automatic
        ? service.recalculateAutomatic()
        : service.recalculate("POSITIONAL_PERFORMANCE", null, null, null);
  }

  static class ControlledClock extends Clock {
    final AtomicReference<Instant> time = new AtomicReference<>(NOW);
    volatile Instant nextRead;

    @Override
    public ZoneId getZone() {
      return ZoneOffset.UTC;
    }

    @Override
    public Clock withZone(ZoneId zone) {
      return Clock.fixed(time.get(), zone);
    }

    @Override
    public synchronized Instant instant() {
      Instant result = time.get();
      if (nextRead != null) {
        time.set(nextRead);
        nextRead = null;
      }
      return result;
    }
  }
}
