package footballmarket.models.records;

import footballmarket.models.enums.GeneralPosition;
import footballmarket.models.enums.MetricSourceSummary;
import footballmarket.models.exceptions.InvalidPlayerException;
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
