package footballmarket.features.valuation.domain.models;

import footballmarket.features.statistics.domain.enums.MetricSource;
import footballmarket.features.statistics.domain.models.MetricCatalog;
import footballmarket.features.statistics.domain.models.PlayerStatistics;
import footballmarket.features.statistics.domain.records.MetricDefinition;
import footballmarket.features.valuation.domain.enums.GeneralPosition;
import footballmarket.features.valuation.domain.enums.MetricSourceSummary;
import footballmarket.features.valuation.domain.enums.ValuationExclusionReason;
import footballmarket.features.valuation.domain.records.ValuationCandidate;
import java.math.BigDecimal;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/** Aplica primacía JSON, fallback permitido y elegibilidad funcional. */
public final class ValuationMetricAssembler {
  private static final Set<String> COMMON =
      Set.of(
          "minutes_played_total",
          "appearances_total",
          "fouls_committed_per_match",
          "yellow_cards_total",
          "red_cards_total");
  private static final Set<String> FIELD =
      Set.of(
          "goals_total",
          "assists_total",
          "shots_per_match",
          "outside_box_goals_per_match",
          "pass_completion_percentage",
          "key_passes_per_match",
          "successful_long_balls_per_match",
          "passes_per_match",
          "failed_dribbles_per_match",
          "total_dribbles_per_match",
          "successful_dribbles_per_match",
          "dispossessed_per_match",
          "bad_controls_per_match",
          "tackles_per_match",
          "interceptions_per_match",
          "clearances_per_match",
          "blocked_passes_per_match",
          "blocked_crosses_per_match",
          "dribbled_past_per_match",
          "aerial_duels_won_per_match",
          "total_aerial_duels_per_match");
  private static final Set<String> GOALKEEPER =
      Set.of(
          "saves_per_match",
          "outside_box_saves_per_match",
          "pass_completion_percentage",
          "successful_long_balls_per_match",
          "passes_per_match",
          "clearances_per_match",
          "aerial_duels_won_per_match",
          "total_aerial_duels_per_match");

  private ValuationMetricAssembler() {}

  public static Assembly assemble(ValuationCandidate candidate) {
    if (candidate.jsonStatistics() == null) {
      return Assembly.excluded(ValuationExclusionReason.MISSING_OR_UNCONFIRMED_SNAPSHOT);
    }
    if (!PlayerStatistics.RESOLVED.equals(candidate.jsonStatistics().getContextStatus())
        || candidate.populationKey().leagueId() == null
        || candidate.populationKey().seasonKey() == null) {
      return Assembly.excluded(ValuationExclusionReason.UNRESOLVED_LEAGUE_CONTEXT);
    }
    Map<String, BigDecimal> values = new LinkedHashMap<>();
    EnumSet<MetricSource> sources = EnumSet.noneOf(MetricSource.class);
    Set<String> required = requiredMetrics(candidate.position());
    for (MetricDefinition definition : MetricCatalog.definitions()) {
      if (!required.contains(definition.key())) {
        continue;
      }
      BigDecimal value = candidate.jsonStatistics().metricValue(definition.key());
      if (value != null) {
        values.put(definition.key(), value);
        sources.add(MetricSource.JSON);
      } else if (definition.sources().contains(MetricSource.SCRAPING)
          && candidate.scrapingStatistics() != null) {
        value = candidate.scrapingStatistics().metricValue(definition.key());
        if (value != null) {
          values.put(definition.key(), value);
          sources.add(MetricSource.SCRAPING);
        }
      }
    }
    MetricSourceSummary summary =
        sources.size() > 1
            ? MetricSourceSummary.MIXED
            : sources.contains(MetricSource.SCRAPING)
                ? MetricSourceSummary.SCRAPING
                : MetricSourceSummary.JSON;
    if (!values.keySet().containsAll(required)) {
      return Assembly.excluded(values, summary, ValuationExclusionReason.MISSING_REQUIRED_METRICS);
    }
    if (values.get("appearances_total").compareTo(BigDecimal.ZERO) <= 0) {
      return Assembly.excluded(values, summary, ValuationExclusionReason.NO_APPEARANCES);
    }
    return Assembly.prepared(values, summary);
  }

  public static Set<String> requiredMetrics(GeneralPosition position) {
    Set<String> required = new LinkedHashSet<>(COMMON);
    if (position == GeneralPosition.GOALKEEPER) {
      required.addAll(GOALKEEPER);
    } else {
      required.addAll(FIELD);
      if (position == GeneralPosition.FORWARD) {
        required.add("offsides_per_match");
      } else if (position == GeneralPosition.MIDFIELDER) {
        required.add("successful_crosses_per_match");
      }
    }
    return Set.copyOf(required);
  }

  /** Resultado de ensamblado, preparado o excluido por una única causa. */
  public record Assembly(
      Map<String, BigDecimal> values,
      MetricSourceSummary sourceSummary,
      Optional<ValuationExclusionReason> exclusion) {
    private static Assembly prepared(
        Map<String, BigDecimal> values, MetricSourceSummary sourceSummary) {
      return new Assembly(Map.copyOf(values), sourceSummary, Optional.empty());
    }

    private static Assembly excluded(ValuationExclusionReason reason) {
      return new Assembly(Map.of(), null, Optional.of(reason));
    }

    private static Assembly excluded(
        Map<String, BigDecimal> values,
        MetricSourceSummary sourceSummary,
        ValuationExclusionReason reason) {
      return new Assembly(Map.copyOf(values), sourceSummary, Optional.of(reason));
    }
  }
}
