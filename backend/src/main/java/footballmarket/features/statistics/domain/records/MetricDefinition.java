package footballmarket.features.statistics.domain.records;

import footballmarket.features.catalog.domain.exceptions.InvalidPlayerException;
import footballmarket.features.statistics.domain.enums.DerivationRule;
import footballmarket.features.statistics.domain.enums.MetricSource;
import footballmarket.features.statistics.domain.enums.MetricUnit;
import footballmarket.features.statistics.domain.models.MetricDerivationApplier;
import footballmarket.features.statistics.domain.models.MetricValueNormalizer;
import java.math.BigDecimal;
import java.util.EnumMap;
import java.util.Map;
import java.util.Set;

/** Definición inmutable de una métrica admitida por el catálogo. */
public record MetricDefinition(
    String key,
    MetricUnit unit,
    Set<MetricSource> sources,
    Map<MetricSource, Set<String>> providerFields,
    boolean optional,
    DerivationRule derivation) {

  public MetricDefinition {
    if (key == null
        || !key.matches("[a-z0-9]+(?:_[a-z0-9]+)*")
        || unit == null
        || sources == null
        || sources.isEmpty()
        || providerFields == null
        || derivation == null
        || !providerFields.keySet().equals(sources)
        || providerFields.values().stream()
            .anyMatch(
                fields ->
                    fields == null
                        || fields.isEmpty()
                        || fields.stream().anyMatch(String::isBlank))) {
      throw new InvalidPlayerException("La definición de métrica es inconsistente");
    }
    sources = Set.copyOf(sources);
    EnumMap<MetricSource, Set<String>> copiedFields = new EnumMap<>(MetricSource.class);
    providerFields.forEach((source, fields) -> copiedFields.put(source, Set.copyOf(fields)));
    providerFields = Map.copyOf(copiedFields);
  }

  /**
   * Regla efectiva por canal: el detalle nativo de scraping ya publica tasas por partido.
   *
   * <p>Solo goles/asistencias del resumen conservan división en scraping. La regla base y sus
   * consumidores JSON permanecen intactos; no se infieren acumulados a partir de un nombre técnico.
   */
  public DerivationRule derivationFor(MetricSource source) {
    if (source == null || !this.sources.contains(source)) {
      throw new InvalidPlayerException("La métrica no pertenece al canal solicitado");
    }
    if (source == MetricSource.SCRAPING
        && this.unit == MetricUnit.PER_MATCH
        && !Set.of("goals_per_match", "assists_per_match").contains(this.key)) {
      return DerivationRule.NONE;
    }
    return this.derivation;
  }

  /**
   * Normaliza según la unidad del canal; solo exige apps cuando la entrada es un acumulado real.
   */
  public MetricValueNormalizer.NormalizedMetricValue normalizeFor(
      MetricSource source, String published, BigDecimal appearances) {
    DerivationRule rule = this.derivationFor(source);
    MetricValueNormalizer.NormalizedMetricValue normalized =
        MetricValueNormalizer.normalize(published);
    if (rule == DerivationRule.NONE) {
      return normalized;
    }
    MetricDerivationApplier.DerivedMetricValue quotient =
        MetricDerivationApplier.perAppearance(normalized.value(), appearances);
    return new MetricValueNormalizer.NormalizedMetricValue(
        quotient.value(), quotient.availability(), quotient.observedPrecision());
  }
}
