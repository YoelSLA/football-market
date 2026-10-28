package footballmarket.models;

import footballmarket.models.enums.MetricAvailability;
import footballmarket.models.exceptions.InvalidPlayerException;
import java.math.BigDecimal;

/** Normaliza valores publicados sin pérdida binaria ni escala artificial. */
public final class MetricValueNormalizer {
  private MetricValueNormalizer() {}

  public static NormalizedMetricValue normalize(String publishedValue) {
    if (publishedValue == null) {
      return new NormalizedMetricValue(null, MetricAvailability.ABSENT, null);
    }
    String normalized = publishedValue.strip();
    if (normalized.isEmpty() || normalized.equals("-") || normalized.equals("N/A")) {
      return new NormalizedMetricValue(null, MetricAvailability.ABSENT, null);
    }
    normalized = normalized.replace(',', '.');
    try {
      BigDecimal value = new BigDecimal(normalized);
      return new NormalizedMetricValue(
          value, MetricAvailability.PRESENT, Math.max(0, value.scale()));
    } catch (NumberFormatException failure) {
      throw new InvalidPlayerException("El valor publicado no es un decimal válido");
    }
  }

  public record NormalizedMetricValue(
      BigDecimal value, MetricAvailability availability, Integer observedPrecision) {}
}
