package footballmarket.features.valuation.application.internal;

import footballmarket.features.catalog.domain.models.player.Player;
import footballmarket.features.statistics.domain.models.PlayerStatistics;
import footballmarket.features.valuation.domain.enums.GeneralPosition;
import footballmarket.features.valuation.domain.enums.ValuationExclusionReason;
import footballmarket.features.valuation.domain.models.ValuationMetricAssembler;
import footballmarket.features.valuation.domain.models.ValuationMetricPipeline;
import footballmarket.features.valuation.domain.records.PreparedValuationMetrics;
import footballmarket.features.valuation.domain.records.ValuationCandidate;
import footballmarket.features.valuation.domain.records.ValuationPopulationKey;
import footballmarket.features.valuation.infraestructure.config.ValuationProperties;
import java.math.BigDecimal;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/** Construye candidatos, derivadas, imputaciones y percentiles sin acceder a proveedores. */
@Component
public class ValuationPopulationPreparer {
  private final ValuationProperties properties;

  public ValuationPopulationPreparer(ValuationProperties properties) {
    this.properties = properties;
  }

  public Preparation prepare(ValuationSnapshotSelector.Snapshot snapshot) {
    Map<Long, PlayerStatistics> scraping =
        snapshot.scrapingRows().stream()
            .collect(Collectors.toMap(row -> row.getPlayer().getId(), Function.identity()));
    List<State> states = new ArrayList<>();
    EnumMap<ValuationExclusionReason, Long> exclusions = zeroExclusions();
    for (PlayerStatistics json : snapshot.jsonRows()) {
      try {
        GeneralPosition position =
            GeneralPosition.fromPlayerPosition(json.getPlayer().getPosition());
        Long leagueId =
            json.getPlayer().getLeague() == null ? null : json.getPlayer().getLeague().getId();
        ValuationCandidate candidate =
            new ValuationCandidate(
                json.getPlayer(),
                json,
                scraping.get(json.getPlayer().getId()),
                position,
                new ValuationPopulationKey(position, leagueId, json.getSeasonKey()));
        ValuationMetricAssembler.Assembly assembly = ValuationMetricAssembler.assemble(candidate);
        State state =
            new State(
                candidate,
                new LinkedHashMap<>(assembly.values()),
                new LinkedHashSet<>(),
                assembly.sourceSummary(),
                assembly.exclusion().orElse(null));
        states.add(state);
        assembly.exclusion().ifPresent(reason -> exclusions.merge(reason, 1L, Long::sum));
      } catch (RuntimeException invalidPosition) {
        states.add(State.failed(json.getPlayer()));
      }
    }
    this.derive(states);
    this.normalize(states);
    List<PreparedPlayer> prepared = new ArrayList<>();
    long preparationFailures = 0;
    for (State state : states) {
      if (state.failed) {
        preparationFailures++;
        continue;
      }
      if (state.exclusion != null) continue;
      BigDecimal confidence =
          ValuationMetricPipeline.confidence(
              state.rawMinutes, state.candidate.position(), this.properties);
      prepared.add(
          new PreparedPlayer(
              state.candidate.player(),
              new PreparedValuationMetrics(
                  state.candidate.position(),
                  state.source,
                  snapshot.extractionDate(),
                  Map.copyOf(state.values),
                  Set.copyOf(state.imputed),
                  confidence)));
    }
    Set<Long> snapshotPlayerIds =
        snapshot.jsonRows().stream()
            .map(row -> row.getPlayer().getId())
            .collect(Collectors.toSet());
    long missingSnapshot =
        snapshot.eligiblePlayerIds().stream().filter(id -> !snapshotPlayerIds.contains(id)).count();
    if (missingSnapshot > 0)
      exclusions.merge(
          ValuationExclusionReason.MISSING_OR_UNCONFIRMED_SNAPSHOT, missingSnapshot, Long::sum);
    return new Preparation(
        List.copyOf(prepared), exclusions, preparationFailures, snapshot.eligiblePlayerIds());
  }

  private void derive(List<State> states) {
    deriveRatio(states, "goalsPer90", "goals_total", "minutes_played_total", true, false);
    deriveRatio(states, "assistsPer90", "assists_total", "minutes_played_total", true, false);
    deriveRatio(
        states, "yellowCardsPer90", "yellow_cards_total", "minutes_played_total", true, false);
    deriveRatio(states, "redCardsPer90", "red_cards_total", "minutes_played_total", true, false);
    deriveRatio(
        states,
        "dribbleSuccess",
        "failed_dribbles_per_match",
        "total_dribbles_per_match",
        false,
        true);
    deriveRatio(
        states,
        "aerialWinRate",
        "aerial_duels_won_per_match",
        "total_aerial_duels_per_match",
        false,
        false);
    deriveRatio(
        states, "saveOutOfBox", "outside_box_saves_per_match", "saves_per_match", false, false);
    for (State state : states) {
      BigDecimal passes = state.values.get("blocked_passes_per_match");
      BigDecimal crosses = state.values.get("blocked_crosses_per_match");
      if (passes != null && crosses != null)
        state.values.put("blocksPerMatch", passes.add(crosses));
    }
  }

  private void deriveRatio(
      List<State> states,
      String target,
      String numerator,
      String denominator,
      boolean perNinety,
      boolean invert) {
    Map<ValuationPopulationKey, List<BigDecimal>> donors = new HashMap<>();
    for (State state : states) {
      if (state.failed) continue;
      BigDecimal n = state.values.get(numerator), d = state.values.get(denominator);
      if (n != null && d != null && d.signum() > 0) {
        BigDecimal value =
            perNinety
                ? ValuationMetricPipeline.perNinety(n, d)
                : ValuationMetricPipeline.ratio(n, d);
        if (invert) value = BigDecimal.ONE.subtract(value);
        state.values.put(target, value);
        donors
            .computeIfAbsent(state.candidate.populationKey(), ignored -> new ArrayList<>())
            .add(value);
      }
    }
    for (State state : states) {
      if (state.failed
          || state.values.containsKey(target)
          || !state.values.containsKey(numerator)
          || !state.values.containsKey(denominator)) continue;
      state.values.put(
          target,
          ValuationMetricPipeline.median(
              donors.getOrDefault(state.candidate.populationKey(), List.of())));
      state.imputed.add(target);
    }
  }

  private void normalize(List<State> states) {
    Map<GroupMetric, List<BigDecimal>> populations = new HashMap<>();
    for (State state : states)
      if (!state.failed)
        for (Map.Entry<String, BigDecimal> metric : state.values.entrySet())
          populations
              .computeIfAbsent(
                  new GroupMetric(state.candidate.populationKey(), metric.getKey()),
                  ignored -> new ArrayList<>())
              .add(metric.getValue());
    for (State state : states) {
      if (state.failed) continue;
      Map<String, BigDecimal> normalized = new LinkedHashMap<>();
      for (Map.Entry<String, BigDecimal> metric : state.values.entrySet()) {
        List<BigDecimal> population =
            populations.get(new GroupMetric(state.candidate.populationKey(), metric.getKey()));
        normalized.put(
            metric.getKey(), ValuationMetricPipeline.percentile(metric.getValue(), population));
      }
      state.values.clear();
      state.values.putAll(normalized);
    }
  }

  private static EnumMap<ValuationExclusionReason, Long> zeroExclusions() {
    EnumMap<ValuationExclusionReason, Long> result = new EnumMap<>(ValuationExclusionReason.class);
    for (ValuationExclusionReason reason : ValuationExclusionReason.values())
      result.put(reason, 0L);
    return result;
  }

  public record PreparedPlayer(Player player, PreparedValuationMetrics metrics) {}

  public record Preparation(
      List<PreparedPlayer> players,
      Map<ValuationExclusionReason, Long> exclusions,
      long preparationFailures,
      List<Long> snapshotPlayerIds) {}

  private record GroupMetric(ValuationPopulationKey population, String metric) {}

  private static final class State {
    private final ValuationCandidate candidate;
    private final Map<String, BigDecimal> values;
    private final Set<String> imputed;
    private final footballmarket.features.valuation.domain.enums.MetricSourceSummary source;
    private final ValuationExclusionReason exclusion;
    private final boolean failed;
    private final BigDecimal rawMinutes;

    private State(
        ValuationCandidate candidate,
        Map<String, BigDecimal> values,
        Set<String> imputed,
        footballmarket.features.valuation.domain.enums.MetricSourceSummary source,
        ValuationExclusionReason exclusion) {
      this.candidate = candidate;
      this.values = values;
      this.imputed = imputed;
      this.source = source;
      this.exclusion = exclusion;
      this.failed = false;
      this.rawMinutes = values.get("minutes_played_total");
    }

    private State(Player player) {
      this.candidate = null;
      this.values = Map.of();
      this.imputed = Set.of();
      this.source = null;
      this.exclusion = null;
      this.failed = true;
      this.rawMinutes = null;
    }

    private static State failed(Player player) {
      return new State(player);
    }
  }
}
