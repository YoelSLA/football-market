package footballmarket.features.quote.domain.models;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import footballmarket.features.quote.domain.exceptions.InvalidQuoteException;
import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

@org.junit.jupiter.api.Tag("unit")
class QuoteAdjustmentTest {

  @Nested
  @DisplayName("Ajuste compuesto de la cotización")
  class CompoundAdjustment {

    @ParameterizedTest
    @CsvSource({
      "10, 0.050000",
      "-20, -0.100000",
      "0, 0.000000",
      "100, 0.500000",
      "-100, -0.500000",
      "500, 0.500000",
      "-500, -0.500000"
    })
    @DisplayName("Calcula y limita el factor de demanda neta")
    void calculatesBoundedMarketDemandFactor(long netDemandTokens, String expectedFactor) {
      assertThat(QuoteAdjustment.marketDemandFactor(netDemandTokens))
          .isEqualByComparingTo(expectedFactor);
    }

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

    @Test
    @DisplayName("Compone multiplicadores deportivo y de mercado antes del tope")
    void composesSportAndMarketMultipliersBeforeCap() {
      QuoteAdjustment result =
          QuoteAdjustment.calculateWithMarketDemand(
              100L, new BigDecimal("0.60"), BigDecimal.ONE, 10L, new BigDecimal("0.10"), 1L);

      assertThat(result.sportMultiplier()).isEqualByComparingTo("1.100000");
      assertThat(result.marketDemandFactor()).isEqualByComparingTo("0.050000");
      assertThat(result.marketMultiplier()).isEqualByComparingTo("1.050000");
      assertThat(result.multiplier()).isEqualByComparingTo("1.100000");
      assertThat(result.price()).isEqualTo(110L);
    }

    @Test
    @DisplayName("Respeta el precio mínimo tras componer y limitar los multiplicadores")
    void appliesMinimumFloorAfterCombinedAdjustment() {
      QuoteAdjustment result =
          QuoteAdjustment.calculateWithMarketDemand(
              2L, BigDecimal.ZERO, BigDecimal.ONE, -100L, BigDecimal.ONE, 2L);

      assertThat(result.price()).isEqualTo(2L);
    }

    @Test
    @DisplayName("Calcula una cotización usando únicamente la demanda de mercado")
    void calculatesMarketDemandWithoutSportsImpact() {
      QuoteAdjustment result =
          QuoteAdjustment.calculateMarketDemand(100L, 10L, new BigDecimal("0.10"), 1L);

      assertThat(result.sportMultiplier()).isNull();
      assertThat(result.marketDemandFactor()).isEqualByComparingTo("0.050000");
      assertThat(result.marketMultiplier()).isEqualByComparingTo("1.050000");
      assertThat(result.multiplier()).isEqualByComparingTo("1.050000");
      assertThat(result.price()).isEqualTo(105L);
    }

    @Test
    @DisplayName("Con demanda neta cero conserva el precio vigente")
    void leavesPriceUnchangedWithZeroDemand() {
      QuoteAdjustment result =
          QuoteAdjustment.calculateMarketDemand(100L, 0L, new BigDecimal("0.10"), 1L);

      assertThat(result.price()).isEqualTo(100L);
      assertThat(result.marketDemandFactor()).isEqualByComparingTo("0.000000");
    }

    @Test
    @DisplayName("Aplica el límite y el precio mínimo al ajuste exclusivo de mercado")
    void appliesMarketCapAndPriceFloor() {
      QuoteAdjustment result =
          QuoteAdjustment.calculateMarketDemand(100L, -100L, new BigDecimal("0.10"), 95L);

      assertThat(result.multiplier()).isEqualByComparingTo("0.900000");
      assertThat(result.price()).isEqualTo(95L);
    }
  }

  @Nested
  @DisplayName("Validación de parámetros y límites extremos")
  class ValidationAndBoundaries {
    @Test
    @DisplayName("calculateWithMarketDemand rechaza precio actual menor o igual a cero")
    void rejectsInvalidCurrentPriceInCompound() {
      assertThatThrownBy(
              () ->
                  QuoteAdjustment.calculateWithMarketDemand(
                      0L, new BigDecimal("0.60"), BigDecimal.ONE, 0L, new BigDecimal("0.10"), 1L))
          .isInstanceOf(InvalidQuoteException.class);

      assertThatThrownBy(
              () ->
                  QuoteAdjustment.calculateWithMarketDemand(
                      -10L, new BigDecimal("0.60"), BigDecimal.ONE, 0L, new BigDecimal("0.10"), 1L))
          .isInstanceOf(InvalidQuoteException.class);
    }

    @Test
    @DisplayName("calculateWithMarketDemand rechaza parámetros nulos obligatorios")
    void rejectsNullParametersInCompound() {
      assertThatThrownBy(
              () ->
                  QuoteAdjustment.calculateWithMarketDemand(
                      100L, null, BigDecimal.ONE, 0L, new BigDecimal("0.10"), 1L))
          .isInstanceOf(InvalidQuoteException.class);

      assertThatThrownBy(
              () ->
                  QuoteAdjustment.calculateWithMarketDemand(
                      100L, new BigDecimal("0.60"), null, 0L, new BigDecimal("0.10"), 1L))
          .isInstanceOf(InvalidQuoteException.class);

      assertThatThrownBy(
              () ->
                  QuoteAdjustment.calculateWithMarketDemand(
                      100L, new BigDecimal("0.60"), BigDecimal.ONE, 0L, null, 1L))
          .isInstanceOf(InvalidQuoteException.class);
    }

    @Test
    @DisplayName("calculateWithMarketDemand rechaza piso mínimo menor o igual a cero")
    void rejectsInvalidMinPriceFloorInCompound() {
      assertThatThrownBy(
              () ->
                  QuoteAdjustment.calculateWithMarketDemand(
                      100L, new BigDecimal("0.60"), BigDecimal.ONE, 0L, new BigDecimal("0.10"), 0L))
          .isInstanceOf(InvalidQuoteException.class);

      assertThatThrownBy(
              () ->
                  QuoteAdjustment.calculateWithMarketDemand(
                      100L,
                      new BigDecimal("0.60"),
                      BigDecimal.ONE,
                      0L,
                      new BigDecimal("0.10"),
                      -5L))
          .isInstanceOf(InvalidQuoteException.class);
    }

    @Test
    @DisplayName("calculateMarketDemand rechaza precio actual inválido")
    void rejectsInvalidCurrentPriceInMarketDemand() {
      assertThatThrownBy(
              () -> QuoteAdjustment.calculateMarketDemand(0L, 10L, new BigDecimal("0.10"), 1L))
          .isInstanceOf(InvalidQuoteException.class);

      assertThatThrownBy(
              () -> QuoteAdjustment.calculateMarketDemand(-50L, 10L, new BigDecimal("0.10"), 1L))
          .isInstanceOf(InvalidQuoteException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"0.00", "-0.10", "1.01", "2.00"})
    @DisplayName("calculateMarketDemand rechaza variación fuera del rango (0; 1]")
    void rejectsInvalidVariationCapInMarketDemand(String invalidCap) {
      assertThatThrownBy(
              () ->
                  QuoteAdjustment.calculateMarketDemand(100L, 10L, new BigDecimal(invalidCap), 1L))
          .isInstanceOf(InvalidQuoteException.class);
    }

    @Test
    @DisplayName("calculateMarketDemand rechaza variationCap nulo y piso menor o igual a cero")
    void rejectsNullCapOrInvalidFloorInMarketDemand() {
      assertThatThrownBy(() -> QuoteAdjustment.calculateMarketDemand(100L, 10L, null, 1L))
          .isInstanceOf(InvalidQuoteException.class);

      assertThatThrownBy(
              () -> QuoteAdjustment.calculateMarketDemand(100L, 10L, new BigDecimal("0.10"), 0L))
          .isInstanceOf(InvalidQuoteException.class);

      assertThatThrownBy(
              () -> QuoteAdjustment.calculateMarketDemand(100L, 10L, new BigDecimal("0.10"), -1L))
          .isInstanceOf(InvalidQuoteException.class);
    }

    @Test
    @DisplayName("calculateMarketDemand acepta variación máxima de 1.00")
    void acceptsMaxVariationCapInMarketDemand() {
      QuoteAdjustment result =
          QuoteAdjustment.calculateMarketDemand(100L, 100L, BigDecimal.ONE, 1L);

      assertThat(result.multiplier()).isEqualByComparingTo("1.500000");
      assertThat(result.price()).isEqualTo(150L);
    }
  }
}
