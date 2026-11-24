package footballmarket.features.quote.domain.models;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import footballmarket.features.catalog.domain.models.league.League;
import footballmarket.features.catalog.domain.models.player.Player;
import footballmarket.features.catalog.domain.models.team.Team;
import footballmarket.features.quote.domain.enums.QuoteExecutionOrigin;
import footballmarket.features.quote.domain.enums.QuoteOrigin;
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
class PlayerQuoteTest {
  private static final LocalDate EFFECTIVE_DATE = LocalDate.of(2026, 11, 23);
  private static final Instant CALCULATED_AT = Instant.parse("2026-11-23T10:00:00Z");

  @Nested
  @DisplayName("Auditoría histórica de cotizaciones")
  class HistoricalAudit {
    @Test
    @DisplayName("Conserva campos de auditoría nulos en una cotización histórica")
    void preservesNullMarketAuditForHistoricalQuote() {
      PlayerQuote quote =
          new PlayerQuote(
              player(),
              EFFECTIVE_DATE,
              105L,
              100L,
              new BigDecimal("1.050000"),
              new BigDecimal("0.600000"),
              ValuationStrategy.POSITIONAL_PERFORMANCE,
              "v1",
              EFFECTIVE_DATE.minusDays(1),
              QuoteOrigin.RECOMPUTED,
              CALCULATED_AT,
              execution());

      assertThat(quote.getSportMultiplier()).isNull();
      assertThat(quote.getMarketDemandFactor()).isNull();
      assertThat(quote.getMarketMultiplier()).isNull();
      assertThat(quote.getMarketNetDemandTokens()).isNull();
    }
  }

  @Nested
  @DisplayName("Trazabilidad de cotizaciones de mercado")
  class MarketAudit {
    @Test
    @DisplayName("Conserva los componentes de una cotización exclusiva de mercado")
    void createsMarketDemandQuoteWithAuditComponents() {
      PlayerQuote quote =
          new PlayerQuote(
              player(),
              EFFECTIVE_DATE,
              105L,
              100L,
              new BigDecimal("1.050000"),
              null,
              null,
              null,
              null,
              QuoteOrigin.MARKET_DEMAND,
              CALCULATED_AT,
              marketExecution(),
              null,
              new BigDecimal("0.050000"),
              new BigDecimal("1.050000"),
              10L);

      assertThat(quote.getOrigin()).isEqualTo(QuoteOrigin.MARKET_DEMAND);
      assertThat(quote.getScore()).isNull();
      assertThat(quote.getStrategyName()).isNull();
      assertThat(quote.getSportMultiplier()).isNull();
      assertThat(quote.getMarketDemandFactor()).isEqualByComparingTo("0.050000");
      assertThat(quote.getMarketMultiplier()).isEqualByComparingTo("1.050000");
      assertThat(quote.getMarketNetDemandTokens()).isEqualTo(10L);
    }

    @Test
    @DisplayName("Rechaza cotización de mercado con datos deportivos")
    void rejectsMarketDemandQuoteWithSportData() {
      assertThatThrownBy(
              () ->
                  new PlayerQuote(
                      player(),
                      EFFECTIVE_DATE,
                      105L,
                      100L,
                      new BigDecimal("1.050000"),
                      new BigDecimal("0.600000"),
                      ValuationStrategy.POSITIONAL_PERFORMANCE,
                      "v1",
                      EFFECTIVE_DATE.minusDays(1),
                      QuoteOrigin.MARKET_DEMAND,
                      CALCULATED_AT,
                      marketExecution(),
                      null,
                      new BigDecimal("0.050000"),
                      new BigDecimal("1.050000"),
                      10L))
          .isInstanceOf(InvalidQuoteException.class);
    }

    @Test
    @DisplayName("Rechaza auditoría parcial en una cotización recalculada")
    void rejectsPartialMarketAudit() {
      assertThatThrownBy(
              () ->
                  new PlayerQuote(
                      player(),
                      EFFECTIVE_DATE,
                      105L,
                      100L,
                      new BigDecimal("1.050000"),
                      new BigDecimal("0.600000"),
                      ValuationStrategy.POSITIONAL_PERFORMANCE,
                      "v1",
                      EFFECTIVE_DATE.minusDays(1),
                      QuoteOrigin.RECOMPUTED,
                      CALCULATED_AT,
                      execution(),
                      new BigDecimal("1.050000"),
                      null,
                      null,
                      null))
          .isInstanceOf(InvalidQuoteException.class);
    }
  }

  private Player player() {
    return new Player("Jugador", new Team("Equipo", new League("Liga"), true), "Forward");
  }

  private QuoteExecution execution() {
    return new QuoteExecution(
        EFFECTIVE_DATE,
        QuoteExecutionOrigin.MANUAL,
        ValuationStrategy.POSITIONAL_PERFORMANCE,
        "v1",
        new BigDecimal("0.50"),
        new BigDecimal("0.10"),
        1L,
        0,
        0,
        0,
        0,
        0,
        CALCULATED_AT,
        null);
  }

  private QuoteExecution marketExecution() {
    return new QuoteExecution(
        EFFECTIVE_DATE,
        QuoteExecutionOrigin.MANUAL,
        null,
        null,
        null,
        new BigDecimal("0.10"),
        1L,
        0,
        0,
        0,
        0,
        0,
        CALCULATED_AT,
        null,
        QuoteRecalculationType.MARKET_DEMAND_ONLY,
        Duration.ofHours(24));
  }
}
