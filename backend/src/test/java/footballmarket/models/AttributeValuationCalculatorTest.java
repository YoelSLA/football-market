package footballmarket.models;

import static org.assertj.core.api.Assertions.assertThat;

import footballmarket.config.ValuationProperties;
import footballmarket.models.enums.GeneralPosition;
import footballmarket.models.enums.MetricSourceSummary;
import footballmarket.models.records.PreparedValuationMetrics;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class AttributeValuationCalculatorTest {
  @Nested
  @DisplayName("Atributos exclusivos de arquero")
  class GoalkeeperAttributes {
    @Test
    @DisplayName("Calcula SAV DIS y ACT sin atributos de campo")
    void calculatesGoalkeeperShape() {
      Player player =
          new Player("Keeper", new Team("Team", new League("League"), true), "Goalkeeper");
      PreparedValuationMetrics metrics =
          new PreparedValuationMetrics(
              GeneralPosition.GOALKEEPER,
              MetricSourceSummary.JSON,
              LocalDate.of(2026, 10, 19),
              PositionalValuationCalculatorTest.neutralValues(),
              Set.of(),
              BigDecimal.ONE);
      ValuationProperties properties =
          new ValuationProperties(
              new ValuationProperties.MinimumMinutes(270, 540),
              new BigDecimal(".10"),
              new BigDecimal(".05"),
              new BigDecimal(".10"));
      AttributeValuation result =
          AttributeValuationCalculator.calculate(
              player, LocalDate.of(2026, 10, 19), metrics, properties, Instant.EPOCH);
      assertThat(result.getSaving()).isNotNull();
      assertThat(result.getDistribution()).isNotNull();
      assertThat(result.getActivity()).isNotNull();
      assertThat(result.getShooting()).isNull();
    }
  }
}
