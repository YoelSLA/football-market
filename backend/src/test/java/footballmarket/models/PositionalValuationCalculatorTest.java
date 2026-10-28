package footballmarket.models;

import static org.assertj.core.api.Assertions.assertThat;

import footballmarket.config.ValuationProperties;
import footballmarket.features.catalog.domain.models.league.League;
import footballmarket.features.catalog.domain.models.player.Player;
import footballmarket.features.catalog.domain.models.team.Team;
import footballmarket.features.valuation.domain.enums.GeneralPosition;
import footballmarket.features.valuation.domain.enums.MetricSourceSummary;
import footballmarket.features.valuation.domain.records.PreparedValuationMetrics;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@org.junit.jupiter.api.Tag("unit")
class PositionalValuationCalculatorTest {
  @Nested
  @DisplayName("Fórmula posicional de arquero")
  class GoalkeeperFormula {
    @Test
    @DisplayName("No aplica penalización de rendimiento al arquero")
    void omitsGoalkeeperPerformancePenalty() {
      Map<String, BigDecimal> values = neutralValues();
      Player player =
          new Player("Keeper", new Team("Team", new League("League"), true), "Goalkeeper");
      PreparedValuationMetrics metrics =
          new PreparedValuationMetrics(
              GeneralPosition.GOALKEEPER,
              MetricSourceSummary.JSON,
              LocalDate.of(2026, 10, 19),
              values,
              Set.of(),
              BigDecimal.ONE);
      ValuationProperties properties =
          new ValuationProperties(
              new ValuationProperties.MinimumMinutes(270, 540),
              new BigDecimal(".10"),
              new BigDecimal(".05"),
              new BigDecimal(".10"));
      PositionalValuation result =
          PositionalValuationCalculator.calculate(
              player, LocalDate.of(2026, 10, 19), metrics, properties, Instant.EPOCH);
      assertThat(result.getPerformancePenalty()).isNull();
      assertThat(result.getPerformanceAdjustment()).isNull();
      assertThat(result.getFinalScore())
          .isBetween(new BigDecimal("0.000000"), new BigDecimal("1.000000"));
    }
  }

  static Map<String, BigDecimal> neutralValues() {
    Map<String, BigDecimal> values = new HashMap<>();
    for (String key :
        Set.of(
            "saves_per_match",
            "saveOutOfBox",
            "pass_completion_percentage",
            "aerialWinRate",
            "fouls_committed_per_match",
            "yellowCardsPer90",
            "redCardsPer90",
            "successful_long_balls_per_match",
            "passes_per_match",
            "clearances_per_match",
            "aerial_duels_won_per_match")) values.put(key, new BigDecimal(".5"));
    return values;
  }
}
