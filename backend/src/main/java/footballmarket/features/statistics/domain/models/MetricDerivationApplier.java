package footballmarket.features.statistics.domain.models;

import footballmarket.features.catalog.domain.exceptions.InvalidPlayerException;
import footballmarket.features.statistics.domain.enums.MetricAvailability;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;
import java.util.Set;

/** Aplica derivaciones por aparición y aliases técnicos del catálogo. */
public final class MetricDerivationApplier {
  private static final Set<String> FOUL_ALIASES = Set.of("foulspergame", "foulcommitted");

  private MetricDerivationApplier() {}

  public static DerivedMetricValue perAppearance(BigDecimal accumulated, BigDecimal appearances) {
    if (accumulated == null) {
      return new DerivedMetricValue(null, MetricAvailability.ABSENT, null);
    }
    if (appearances == null || appearances.signum() == 0) {
      return new DerivedMetricValue(null, MetricAvailability.NOT_DERIVABLE, null);
    }
    if (appearances.signum() < 0) {
      throw new InvalidPlayerException("Las apariciones no pueden ser negativas");
    }
    BigDecimal value = accumulated.divide(appearances, 6, RoundingMode.HALF_UP);
    return new DerivedMetricValue(value, MetricAvailability.PRESENT, 6);
  }

  public static String canonicalKey(String providerField) {
    if (providerField == null || providerField.isBlank()) {
      throw new InvalidPlayerException("La columna externa es obligatoria");
    }
    String normalized = providerField.strip().toLowerCase(Locale.ROOT);
    return FOUL_ALIASES.contains(normalized) ? "fouls_committed_per_match" : normalized;
  }

  public record DerivedMetricValue(
      BigDecimal value, MetricAvailability availability, Integer observedPrecision) {}
}
