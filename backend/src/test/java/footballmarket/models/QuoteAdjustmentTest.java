package footballmarket.models;

import static org.assertj.core.api.Assertions.assertThat;

import footballmarket.features.quote.domain.models.QuoteAdjustment;
import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@org.junit.jupiter.api.Tag("unit")
class QuoteAdjustmentTest {

  @Nested
  @DisplayName("Ajuste compuesto de la cotización")
  class CompoundAdjustment {

    @Test
    @DisplayName("Aplica la fórmula y redondea el precio half-up")
    void calculatesAndRoundsHalfUp() {
      QuoteAdjustment result =
          QuoteAdjustment.calculate(
              101L, new BigDecimal("0.60"), new BigDecimal("0.50"), new BigDecimal("0.20"), 1L);

      assertThat(result.multiplier()).isEqualByComparingTo("1.050000");
      assertThat(result.price()).isEqualTo(106L);
    }

    @Test
    @DisplayName("Acota una suba por el tope de variación")
    void capsIncrease() {
      QuoteAdjustment result =
          QuoteAdjustment.calculate(
              100L, BigDecimal.ONE, BigDecimal.ONE, new BigDecimal("0.10"), 1L);

      assertThat(result.multiplier()).isEqualByComparingTo("1.100000");
      assertThat(result.price()).isEqualTo(110L);
    }

    @Test
    @DisplayName("Acota una baja por el tope de variación")
    void capsDecrease() {
      QuoteAdjustment result =
          QuoteAdjustment.calculate(
              100L, BigDecimal.ZERO, BigDecimal.ONE, new BigDecimal("0.10"), 1L);

      assertThat(result.multiplier()).isEqualByComparingTo("0.900000");
      assertThat(result.price()).isEqualTo(90L);
    }

    @Test
    @DisplayName("Respeta el piso mínimo cuando el precio ajustado queda por debajo")
    void appliesMinimumFloor() {
      QuoteAdjustment result =
          QuoteAdjustment.calculate(2L, BigDecimal.ZERO, BigDecimal.ONE, BigDecimal.ONE, 2L);

      assertThat(result.price()).isEqualTo(2L);
    }
  }
}
