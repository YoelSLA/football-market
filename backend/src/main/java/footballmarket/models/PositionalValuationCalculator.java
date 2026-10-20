package footballmarket.models;

import footballmarket.config.ValuationProperties;
import footballmarket.models.enums.GeneralPosition;
import footballmarket.models.records.PreparedValuationMetrics;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;

/** Fórmulas deterministas de rendimiento específico por posición. */
public final class PositionalValuationCalculator {
  private PositionalValuationCalculator() {}

  public static PositionalValuation calculate(
      Player player,
      LocalDate date,
      PreparedValuationMetrics metrics,
      ValuationProperties properties,
      Instant calculatedAt) {
    Map<String, BigDecimal> weights = positiveWeights(metrics.position());
    BigDecimal positive = ValuationMetricPipeline.weighted(metrics.values(), weights);
    BigDecimal disciplinePenalty = ValuationMetricPipeline.disciplinePenalty(metrics.values());
    BigDecimal disciplineAdjustment =
        ValuationMetricPipeline.adjustment(properties.disciplineMaximumImpact(), disciplinePenalty);
    BigDecimal performancePenalty = null;
    BigDecimal performanceAdjustment = null;
    BigDecimal raw;
    if (metrics.position() == GeneralPosition.GOALKEEPER) {
      raw = positive.add(disciplineAdjustment, ValuationMetricPipeline.CONTEXT);
    } else {
      performancePenalty =
          ValuationMetricPipeline.weighted(metrics.values(), penaltyWeights(metrics.position()));
      performanceAdjustment =
          ValuationMetricPipeline.adjustment(
              properties.performanceMaximumImpact(), performancePenalty);
      raw =
          positive
              .add(performanceAdjustment, ValuationMetricPipeline.CONTEXT)
              .add(disciplineAdjustment, ValuationMetricPipeline.CONTEXT);
    }
    BigDecimal result = ValuationMetricPipeline.applyConfidence(raw, metrics.confidence());
    return new PositionalValuation(
        player,
        date,
        metrics.position(),
        metrics.sourceSummary(),
        metrics.sourceExtractionDate(),
        positive,
        performancePenalty,
        performanceAdjustment,
        disciplinePenalty,
        disciplineAdjustment,
        raw,
        metrics.confidence(),
        result,
        metrics.imputedMetrics().toArray(String[]::new),
        calculatedAt);
  }

  private static Map<String, BigDecimal> positiveWeights(GeneralPosition position) {
    return switch (position) {
      case FORWARD ->
          weights(
              "goalsPer90",
              ".30",
              "assistsPer90",
              ".20",
              "shots_per_match",
              ".15",
              "key_passes_per_match",
              ".15",
              "successful_dribbles_per_match",
              ".10",
              "dribbleSuccess",
              ".10");
      case MIDFIELDER ->
          weights(
              "pass_completion_percentage",
              ".20",
              "key_passes_per_match",
              ".15",
              "assistsPer90",
              ".15",
              "passes_per_match",
              ".10",
              "successful_long_balls_per_match",
              ".10",
              "successful_crosses_per_match",
              ".05",
              "successful_dribbles_per_match",
              ".10",
              "tackles_per_match",
              ".075",
              "interceptions_per_match",
              ".075");
      case DEFENDER ->
          weights(
              "tackles_per_match",
              ".20",
              "interceptions_per_match",
              ".20",
              "clearances_per_match",
              ".15",
              "blocksPerMatch",
              ".15",
              "aerialWinRate",
              ".15",
              "aerial_duels_won_per_match",
              ".15");
      case GOALKEEPER ->
          weights(
              "saves_per_match",
              ".40",
              "saveOutOfBox",
              ".20",
              "pass_completion_percentage",
              ".20",
              "aerialWinRate",
              ".20");
    };
  }

  private static Map<String, BigDecimal> penaltyWeights(GeneralPosition position) {
    return switch (position) {
      case FORWARD ->
          weights(
              "offsides_per_match",
              ".30",
              "dispossessed_per_match",
              ".40",
              "bad_controls_per_match",
              ".30");
      case MIDFIELDER -> weights("dispossessed_per_match", ".55", "bad_controls_per_match", ".45");
      case DEFENDER -> weights("dribbled_past_per_match", "1.0");
      case GOALKEEPER -> Map.of();
    };
  }

  private static Map<String, BigDecimal> weights(String... entries) {
    java.util.LinkedHashMap<String, BigDecimal> result = new java.util.LinkedHashMap<>();
    for (int index = 0; index < entries.length; index += 2)
      result.put(entries[index], new BigDecimal(entries[index + 1]));
    return Map.copyOf(result);
  }
}
