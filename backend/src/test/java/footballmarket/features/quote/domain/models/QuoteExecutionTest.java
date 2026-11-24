package footballmarket.features.quote.domain.models;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import footballmarket.features.quote.domain.enums.QuoteExecutionFailureReason;
import footballmarket.features.quote.domain.enums.QuoteExecutionOrigin;
import footballmarket.features.quote.domain.enums.QuoteExecutionResult;
import footballmarket.features.quote.domain.enums.QuoteRecalculationType;
import footballmarket.features.quote.domain.exceptions.InvalidQuoteException;
import footballmarket.features.valuation.domain.enums.ValuationStrategy;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
class QuoteExecutionTest {
  @Nested
  @DisplayName("Origen y resultado de las ejecuciones")
  class ExecutionOutcome {
    @Test
    @DisplayName("Rechaza una ejecución sin origen")
    void rejectsMissingOrigin() {
      // Act / Assert
      assertThatThrownBy(() -> execution(null, 0)).isInstanceOf(InvalidQuoteException.class);
    }

    @Test
    @DisplayName("El constructor histórico identifica el recálculo habitual")
    void defaultsLegacyExecutionToSportAndMarket() {
      assertThat(execution(QuoteExecutionOrigin.MANUAL, 0).getRecalculationType())
          .isEqualTo(QuoteRecalculationType.SPORT_AND_MARKET);
    }

    @Test
    @DisplayName("Admite ejecución exclusiva de mercado sin parámetros deportivos")
    void createsMarketOnlyExecutionWithoutSportParameters() {
      QuoteExecution execution = marketOnlyExecution();

      assertThat(execution.getStrategyName()).isNull();
      assertThat(execution.getStrategyVersion()).isNull();
      assertThat(execution.getAdjustmentFactor()).isNull();
      assertThat(execution.getRecalculationType())
          .isEqualTo(QuoteRecalculationType.MARKET_DEMAND_ONLY);
      assertThat(execution.getDemandWindow()).isEqualTo(Duration.ofHours(24));
    }

    @Test
    @DisplayName("Rechaza ejecución exclusiva de mercado sin ventana")
    void rejectsMarketOnlyExecutionWithoutWindow() {
      assertThatThrownBy(
              () ->
                  marketOnlyExecution(
                      null, QuoteRecalculationType.MARKET_DEMAND_ONLY, null, null, null))
          .isInstanceOf(InvalidQuoteException.class);
    }

    @Test
    @DisplayName("Una corrida sin fallos individuales tiene éxito sin motivo de fallo")
    void derivesSuccessfulResult() {
      // Arrange
      QuoteExecution execution = execution(QuoteExecutionOrigin.AUTOMATIC, 0);
      // Act / Assert
      assertThat(execution.result()).isEqualTo(QuoteExecutionResult.SUCCESS);
      assertThat(execution.failureReason()).isNull();
    }

    @Test
    @DisplayName("Deriva el fallo individual tanto al construir como al completar el resumen")
    void derivesIndividualFailure() {
      // Arrange
      QuoteExecution failed = execution(QuoteExecutionOrigin.AUTOMATIC, 1);
      QuoteExecution reserved = context();
      // Act
      reserved.complete(2, 1, 0, 0, 1, Instant.parse("2026-11-04T00:01:00Z"));
      // Assert
      assertThat(failed.result()).isEqualTo(QuoteExecutionResult.FAILED);
      assertThat(failed.failureReason())
          .isEqualTo(QuoteExecutionFailureReason.INDIVIDUAL_PLAYER_FAILURES);
      assertThat(reserved.result()).isEqualTo(failed.result());
      assertThat(reserved.failureReason()).isEqualTo(failed.failureReason());
    }

    @Test
    @DisplayName("Un contexto sin cierre no representa éxito y solo puede cerrarse una vez")
    void rejectsUnfinishedResultAndRepeatedClose() {
      // Arrange
      QuoteExecution execution = context();
      Instant finish = Instant.parse("2026-11-04T00:01:00Z");
      // Act / Assert
      assertThatThrownBy(execution::result).isInstanceOf(InvalidQuoteException.class);
      assertThatThrownBy(() -> execution.complete(2, 1, 0, 0, 0, finish))
          .isInstanceOf(InvalidQuoteException.class);
      assertThat(execution.getFinishedAt()).isNull();
      execution.complete(2, 1, 0, 1, 0, finish);
      assertThat(execution.result()).isEqualTo(QuoteExecutionResult.SUCCESS);
      assertThatThrownBy(() -> execution.complete(2, 0, 0, 2, 0, finish))
          .isInstanceOf(InvalidQuoteException.class);
      assertThat(execution.getRecomputedRows()).isEqualTo(1);
    }
  }

  private QuoteExecution context() {
    return new QuoteExecution(
        LocalDate.of(2026, 11, 4),
        QuoteExecutionOrigin.MANUAL,
        ValuationStrategy.POSITIONAL_PERFORMANCE,
        "v1",
        new BigDecimal("0.10"),
        new BigDecimal("0.10"),
        1L,
        0,
        0,
        0,
        0,
        0,
        Instant.parse("2026-11-04T00:00:00Z"),
        null);
  }

  private QuoteExecution execution(QuoteExecutionOrigin origin, int failures) {
    return new QuoteExecution(
        LocalDate.of(2026, 11, 4),
        origin,
        ValuationStrategy.POSITIONAL_PERFORMANCE,
        "v1",
        new BigDecimal("0.10"),
        new BigDecimal("0.10"),
        1L,
        2,
        2 - failures,
        0,
        0,
        failures,
        Instant.parse("2026-11-04T00:00:00Z"),
        Instant.parse("2026-11-04T00:00:01Z"));
  }

  private QuoteExecution marketOnlyExecution() {
    return marketOnlyExecution(
        Duration.ofHours(24), QuoteRecalculationType.MARKET_DEMAND_ONLY, null, null, null);
  }

  private QuoteExecution marketOnlyExecution(
      Duration demandWindow,
      QuoteRecalculationType recalculationType,
      ValuationStrategy strategyName,
      String strategyVersion,
      BigDecimal adjustmentFactor) {
    return new QuoteExecution(
        LocalDate.of(2026, 11, 4),
        QuoteExecutionOrigin.MANUAL,
        strategyName,
        strategyVersion,
        adjustmentFactor,
        new BigDecimal("0.10"),
        1L,
        0,
        0,
        0,
        0,
        0,
        Instant.parse("2026-11-04T00:00:00Z"),
        null,
        recalculationType,
        demandWindow);
  }
}
