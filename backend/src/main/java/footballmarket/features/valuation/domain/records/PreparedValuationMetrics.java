package footballmarket.features.valuation.domain.records;

import footballmarket.features.catalog.domain.exceptions.InvalidPlayerException;
import footballmarket.features.valuation.domain.enums.GeneralPosition;
import footballmarket.features.valuation.domain.enums.MetricSourceSummary;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import java.util.Set;

/** Métricas listas para fórmulas, con valores normalizados e imputaciones. */
public record PreparedValuationMetrics(
    GeneralPosition position,
    MetricSourceSummary sourceSummary,
    LocalDate sourceExtractionDate,
    Map<String, BigDecimal> values,
    Set<String> imputedMetrics,
    BigDecimal confidence) {

  public BigDecimal value(String key) {
    BigDecimal value = this.values.get(key);
    if (value == null) {
      throw new InvalidPlayerException("Falta una métrica preparada para la valoración");
    }
    return value;
  }
}
