package footballmarket.models;

import static org.assertj.core.api.Assertions.assertThat;

import footballmarket.features.valuation.domain.models.ValuationMetricPipeline;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@org.junit.jupiter.api.Tag("unit")
class ValuationMetricPipelineTest {
  @Nested
  @DisplayName("Normalización e imputación compartidas")
  class Pipeline {
    @Test
    @DisplayName("Calcula rango medio estable para valores empatados")
    void calculatesMidrankPercentile() {
      BigDecimal result =
          ValuationMetricPipeline.percentile(
              BigDecimal.TWO,
              List.of(BigDecimal.ONE, BigDecimal.TWO, BigDecimal.TWO, BigDecimal.TEN));
      assertThat(result).isEqualByComparingTo("0.5");
    }

    @Test
    @DisplayName("Promedia los dos valores centrales de una mediana par")
    void calculatesEvenMedian() {
      assertThat(ValuationMetricPipeline.median(List.of(BigDecimal.ONE, BigDecimal.valueOf(3))))
          .isEqualByComparingTo("2");
    }

    @Test
    @DisplayName("Aplica clamp antes del redondeo persistido")
    void clampsAndRounds() {
      assertThat(
              ValuationMetricPipeline.persisted(
                  ValuationMetricPipeline.clamp(new BigDecimal("1.2345678"))))
          .isEqualByComparingTo("1.000000");
    }
  }
}
