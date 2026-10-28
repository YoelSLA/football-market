package footballmarket.features.valuation.domain.models;

import footballmarket.features.valuation.domain.enums.GeneralPosition;
import footballmarket.features.valuation.infraestructure.config.ValuationProperties;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/** Operaciones deterministas compartidas de derivación, normalización y precisión. */
public final class ValuationMetricPipeline {
  public static final MathContext CONTEXT = new MathContext(16, RoundingMode.HALF_UP);
  public static final BigDecimal HALF = new BigDecimal("0.5");
  private static final BigDecimal NINETY = new BigDecimal("90");

  private ValuationMetricPipeline() {}

  public static BigDecimal perNinety(BigDecimal total, BigDecimal minutes) {
    return total.multiply(NINETY, CONTEXT).divide(minutes, CONTEXT);
  }

  public static BigDecimal ratio(BigDecimal numerator, BigDecimal denominator) {
    return numerator.divide(denominator, CONTEXT);
  }

  public static BigDecimal median(List<BigDecimal> values) {
    if (values.isEmpty()) {
      return HALF;
    }
    List<BigDecimal> sorted = new ArrayList<>(values);
    Collections.sort(sorted);
    int middle = sorted.size() / 2;
    if (sorted.size() % 2 == 1) {
      return sorted.get(middle);
    }
    return sorted.get(middle - 1).add(sorted.get(middle), CONTEXT).divide(BigDecimal.TWO, CONTEXT);
  }

  public static BigDecimal percentile(BigDecimal value, List<BigDecimal> population) {
    long lower = population.stream().filter(item -> item.compareTo(value) < 0).count();
    long equal = population.stream().filter(item -> item.compareTo(value) == 0).count();
    return BigDecimal.valueOf(lower)
        .add(HALF.multiply(BigDecimal.valueOf(equal), CONTEXT), CONTEXT)
        .divide(BigDecimal.valueOf(population.size()), CONTEXT);
  }

  public static BigDecimal confidence(
      BigDecimal minutes, GeneralPosition position, ValuationProperties properties) {
    int minimum =
        position == GeneralPosition.GOALKEEPER
            ? properties.minimumMinutes().goalkeeper()
            : properties.minimumMinutes().field();
    return clamp(minutes.divide(BigDecimal.valueOf(minimum), CONTEXT));
  }

  public static BigDecimal disciplinePenalty(Map<String, BigDecimal> metrics) {
    return weighted(
        metrics,
        Map.of(
            "fouls_committed_per_match", new BigDecimal("0.20"),
            "yellowCardsPer90", new BigDecimal("0.30"),
            "redCardsPer90", new BigDecimal("0.50")));
  }

  public static BigDecimal adjustment(BigDecimal maximumImpact, BigDecimal penalty) {
    return maximumImpact.multiply(
        BigDecimal.ONE.subtract(penalty.multiply(BigDecimal.TWO)), CONTEXT);
  }

  public static BigDecimal applyConfidence(BigDecimal raw, BigDecimal confidence) {
    return clamp(
        confidence
            .multiply(raw, CONTEXT)
            .add(BigDecimal.ONE.subtract(confidence).multiply(HALF, CONTEXT), CONTEXT));
  }

  public static BigDecimal weighted(
      Map<String, BigDecimal> metrics, Map<String, BigDecimal> weights) {
    BigDecimal result = BigDecimal.ZERO;
    for (Map.Entry<String, BigDecimal> weight : weights.entrySet()) {
      result =
          result.add(metrics.get(weight.getKey()).multiply(weight.getValue(), CONTEXT), CONTEXT);
    }
    return result;
  }

  public static BigDecimal clamp(BigDecimal value) {
    return value.max(BigDecimal.ZERO).min(BigDecimal.ONE);
  }

  public static BigDecimal persisted(BigDecimal value) {
    return value.setScale(6, RoundingMode.HALF_UP);
  }
}
