package footballmarket.features.statistics.domain.records;

import footballmarket.features.catalog.domain.exceptions.InvalidPlayerException;
import footballmarket.features.statistics.domain.enums.DerivationRule;
import footballmarket.features.statistics.domain.enums.MetricSource;
import footballmarket.features.statistics.domain.enums.MetricUnit;
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
}
