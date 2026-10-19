package footballmarket.services.internal;

import footballmarket.integrations.exceptions.WhoScoredStatisticsException;
import footballmarket.integrations.records.WhoScoredStatisticsResponse;
import footballmarket.integrations.records.WhoScoredStatisticsTable;
import footballmarket.models.MetricCatalog;
import footballmarket.models.MetricDefinition;
import footballmarket.models.MetricDerivationApplier;
import footballmarket.models.MetricValueNormalizer;
import footballmarket.models.MetricValueNormalizer.NormalizedMetricValue;
import footballmarket.models.enums.DerivationRule;
import footballmarket.models.enums.MetricAvailability;
import footballmarket.models.enums.MetricSource;
import footballmarket.models.exceptions.InvalidPlayerException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Traduce la respuesta JSON a los contextoscompetitivos del jugador.
 *
 * <p>Solo se emiten las métricas efectivamente publicadas; las demás no se representan y la fila
 * cuenta su ausencia. El selector de liga se encarga de descartar el resto de los contextos.
 */
@Component
public class JsonMetricAssembler {
  private static final Map<String, String> SUMMARY_DERIVATIONS =
      Map.of("goal", "goals_per_match", "assisttotal", "assists_per_match");

  public List<PlayerMetricsContext> assemble(WhoScoredStatisticsResponse response) {
    try {
      return this.assembleValues(response);
    } catch (InvalidPlayerException invalid) {
      throw new WhoScoredStatisticsException("PROVIDER_DATA_INCONSISTENT", null);
    }
  }

  private List<PlayerMetricsContext> assembleValues(WhoScoredStatisticsResponse response) {
    Map<String, List<WhoScoredStatisticsTable>> groups = new LinkedHashMap<>();
    for (WhoScoredStatisticsTable table : response.tables()) {
      groups
          .computeIfAbsent(
              table.competitionId() + ":" + table.seasonKey(), ignored -> new ArrayList<>())
          .add(table);
    }
    List<PlayerMetricsContext> result = new ArrayList<>();
    for (List<WhoScoredStatisticsTable> tables : groups.values()) {
      WhoScoredStatisticsTable context = tables.getFirst();
      Map<String, PlayerMetricsContext.MetricResult> metrics = new LinkedHashMap<>();
      BigDecimal appearances = appearances(tables);
      for (WhoScoredStatisticsTable table : tables) {
        Map<String, String> fields = new LinkedHashMap<>();
        for (WhoScoredStatisticsTable.Cell cell : table.cells()) {
          fields.put(cell.providerField().strip().toLowerCase(Locale.ROOT), cell.text());
        }
        this.apply(metrics, fields, appearances);
      }
      result.add(
          new PlayerMetricsContext(
              context.competitionId(),
              context.teamId(),
              context.seasonKey(),
              context.seasonLabel(),
              metrics));
    }
    return List.copyOf(result);
  }

  private void apply(
      Map<String, PlayerMetricsContext.MetricResult> metrics,
      Map<String, String> fields,
      BigDecimal appearances) {
    for (Map.Entry<String, String> field : fields.entrySet()) {
      String key;
      try {
        key = MetricCatalog.canonicalKey(field.getKey());
      } catch (InvalidPlayerException unknown) {
        continue;
      }
      MetricDefinition definition = MetricCatalog.require(key);
      if (!definition.sources().contains(MetricSource.JSON)) {
        continue;
      }
      boolean alias = key.equals("fouls_committed_per_match");
      boolean derived = definition.derivation() == DerivationRule.DIVIDE_BY_APPEARANCES && !alias;
      NormalizedMetricValue normalized = MetricValueNormalizer.normalize(field.getValue());
      if (normalized.availability() != MetricAvailability.PRESENT) {
        continue;
      }
      if (!derived) {
        this.put(
            metrics,
            key,
            new PlayerMetricsContext.MetricResult(
                normalized.value(), normalized.observedPrecision()));
        continue;
      }
      MetricDerivationApplier.DerivedMetricValue quotient =
          MetricDerivationApplier.perAppearance(normalized.value(), appearances);
      this.put(
          metrics,
          key,
          new PlayerMetricsContext.MetricResult(quotient.value(), quotient.observedPrecision()));
    }
    SUMMARY_DERIVATIONS.forEach(
        (field, key) -> {
          if (!fields.containsKey(field)) {
            return;
          }
          MetricDerivationApplier.DerivedMetricValue quotient =
              MetricDerivationApplier.perAppearance(
                  MetricValueNormalizer.normalize(fields.get(field)).value(), appearances);
          this.put(
              metrics,
              key,
              new PlayerMetricsContext.MetricResult(
                  quotient.value(), quotient.observedPrecision()));
        });
  }

  private void put(
      Map<String, PlayerMetricsContext.MetricResult> metrics,
      String key,
      PlayerMetricsContext.MetricResult result) {
    if (result == null || result.value() == null) {
      return;
    }
    metrics.put(key, result);
  }

  /** Apariciones publicadas en la campaña del jugador; sin ellas no hay métricas derivadas. */
  private BigDecimal appearances(List<WhoScoredStatisticsTable> tables) {
    for (WhoScoredStatisticsTable table : tables) {
      for (WhoScoredStatisticsTable.Cell cell : table.cells()) {
        if (cell.providerField().strip().equalsIgnoreCase("apps")) {
          return MetricValueNormalizer.normalize(cell.text()).value();
        }
      }
    }
    return null;
  }
}
