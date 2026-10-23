package footballmarket.models;

import footballmarket.models.exceptions.InvalidQuoteException;
import java.math.BigDecimal;
import java.math.RoundingMode;

/** Resultado inmutable del ajuste compuesto de una cotización. */
public record QuoteAdjustment(long price, BigDecimal multiplier) {
  private static final BigDecimal NEUTRAL_SCORE = new BigDecimal("0.5");

  /**
   * Calcula el multiplicador acotado y el precio entero en centésimas.
   *
   * @param currentPrice precio vigente positivo
   * @param score score normalizado de la valuación
   * @param adjustmentFactor factor de sensibilidad de la corrida
   * @param variationCap variación máxima respecto de uno
   * @param minPriceFloor piso positivo del precio resultante
   * @return ajuste calculado y listo para persistir
   */
  public static QuoteAdjustment calculate(
      long currentPrice,
      BigDecimal score,
      BigDecimal adjustmentFactor,
      BigDecimal variationCap,
      long minPriceFloor) {
    if (currentPrice <= 0
        || score == null
        || adjustmentFactor == null
        || variationCap == null
        || minPriceFloor <= 0) {
      throw new InvalidQuoteException("Los parámetros del ajuste de cotización son inválidos");
    }
    BigDecimal rawMultiplier =
        BigDecimal.ONE.add(adjustmentFactor.multiply(score.subtract(NEUTRAL_SCORE)));
    BigDecimal multiplier =
        rawMultiplier
            .max(BigDecimal.ONE.subtract(variationCap))
            .min(BigDecimal.ONE.add(variationCap))
            .setScale(6, RoundingMode.HALF_UP);
    long adjustedPrice =
        BigDecimal.valueOf(currentPrice)
            .multiply(multiplier)
            .setScale(0, RoundingMode.HALF_UP)
            .longValueExact();
    return new QuoteAdjustment(Math.max(minPriceFloor, adjustedPrice), multiplier);
  }
}
