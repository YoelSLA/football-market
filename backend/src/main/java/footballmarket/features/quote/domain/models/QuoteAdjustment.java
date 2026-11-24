package footballmarket.features.quote.domain.models;

import footballmarket.features.quote.domain.exceptions.InvalidQuoteException;
import java.math.BigDecimal;
import java.math.RoundingMode;

/** Resultado inmutable del ajuste compuesto de una cotización. */
public record QuoteAdjustment(
    long price,
    BigDecimal multiplier,
    BigDecimal sportMultiplier,
    BigDecimal marketDemandFactor,
    BigDecimal marketMultiplier) {
  private static final BigDecimal NEUTRAL_SCORE = new BigDecimal("0.5");
  private static final BigDecimal MAX_MARKET_FACTOR = new BigDecimal("0.50");
  private static final BigDecimal TOKENS_PER_FULL_FACTOR = new BigDecimal("100");

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
    return QuoteAdjustment.calculateWithMarketDemand(
        currentPrice, score, adjustmentFactor, 0L, variationCap, minPriceFloor);
  }

  /**
   * Calcula el factor de mercado lineal para la demanda neta, acotado a ±50%.
   *
   * @param netDemandTokens tokens comprados menos tokens vendidos
   * @return factor de mercado redondeado a seis decimales
   */
  public static BigDecimal marketDemandFactor(long netDemandTokens) {
    return BigDecimal.valueOf(netDemandTokens)
        .divide(TOKENS_PER_FULL_FACTOR, 6, RoundingMode.HALF_UP)
        .multiply(MAX_MARKET_FACTOR)
        .max(MAX_MARKET_FACTOR.negate())
        .min(MAX_MARKET_FACTOR)
        .setScale(6, RoundingMode.HALF_UP);
  }

  /**
   * Calcula una cotización aplicando únicamente demanda de mercado, límite y piso.
   *
   * @param currentPrice precio vigente positivo
   * @param netDemandTokens tokens comprados menos tokens vendidos
   * @param variationCap variación máxima respecto de uno
   * @param minPriceFloor piso positivo del precio resultante
   * @return ajuste calculado sin contribución deportiva
   */
  public static QuoteAdjustment calculateMarketDemand(
      long currentPrice, long netDemandTokens, BigDecimal variationCap, long minPriceFloor) {
    if (currentPrice <= 0
        || variationCap == null
        || variationCap.compareTo(BigDecimal.ZERO) <= 0
        || variationCap.compareTo(BigDecimal.ONE) > 0
        || minPriceFloor <= 0) {
      throw new InvalidQuoteException("Los parámetros del ajuste de cotización son inválidos");
    }
    BigDecimal marketDemandFactor = QuoteAdjustment.marketDemandFactor(netDemandTokens);
    BigDecimal marketMultiplier = BigDecimal.ONE.add(marketDemandFactor);
    BigDecimal multiplier =
        marketMultiplier
            .max(BigDecimal.ONE.subtract(variationCap))
            .min(BigDecimal.ONE.add(variationCap))
            .setScale(6, RoundingMode.HALF_UP);
    long adjustedPrice =
        BigDecimal.valueOf(currentPrice)
            .multiply(multiplier)
            .setScale(0, RoundingMode.HALF_UP)
            .longValueExact();
    return new QuoteAdjustment(
        Math.max(minPriceFloor, adjustedPrice),
        multiplier,
        null,
        marketDemandFactor,
        marketMultiplier.setScale(6, RoundingMode.HALF_UP));
  }

  /**
   * Calcula los multiplicadores deportivo y de mercado, limita su producto y aplica el piso.
   *
   * @param currentPrice precio vigente positivo
   * @param score score normalizado de la valuación
   * @param adjustmentFactor factor de sensibilidad deportiva de la corrida
   * @param netDemandTokens tokens comprados menos tokens vendidos
   * @param variationCap variación máxima respecto de uno
   * @param minPriceFloor piso positivo del precio resultante
   * @return ajuste calculado y listo para persistir
   */
  public static QuoteAdjustment calculateWithMarketDemand(
      long currentPrice,
      BigDecimal score,
      BigDecimal adjustmentFactor,
      long netDemandTokens,
      BigDecimal variationCap,
      long minPriceFloor) {
    if (currentPrice <= 0
        || score == null
        || adjustmentFactor == null
        || variationCap == null
        || minPriceFloor <= 0) {
      throw new InvalidQuoteException("Los parámetros del ajuste de cotización son inválidos");
    }
    BigDecimal sportMultiplier =
        BigDecimal.ONE.add(adjustmentFactor.multiply(score.subtract(NEUTRAL_SCORE)));
    BigDecimal marketDemandFactor = QuoteAdjustment.marketDemandFactor(netDemandTokens);
    BigDecimal marketMultiplier = BigDecimal.ONE.add(marketDemandFactor);
    BigDecimal multiplier =
        sportMultiplier
            .multiply(marketMultiplier)
            .max(BigDecimal.ONE.subtract(variationCap))
            .min(BigDecimal.ONE.add(variationCap))
            .setScale(6, RoundingMode.HALF_UP);
    long adjustedPrice =
        BigDecimal.valueOf(currentPrice)
            .multiply(multiplier)
            .setScale(0, RoundingMode.HALF_UP)
            .longValueExact();
    return new QuoteAdjustment(
        Math.max(minPriceFloor, adjustedPrice),
        multiplier,
        sportMultiplier.setScale(6, RoundingMode.HALF_UP),
        marketDemandFactor,
        marketMultiplier.setScale(6, RoundingMode.HALF_UP));
  }
}
