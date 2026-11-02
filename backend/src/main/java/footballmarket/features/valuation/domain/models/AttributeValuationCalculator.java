package footballmarket.features.valuation.domain.models;

import footballmarket.features.catalog.domain.models.player.Player;
import footballmarket.features.valuation.domain.enums.GeneralPosition;
import footballmarket.features.valuation.domain.records.PreparedValuationMetrics;
import footballmarket.features.valuation.infraestructure.config.ValuationProperties;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;

/** Fórmulas deterministas de atributos generales y GRL. */
public final class AttributeValuationCalculator {
  private AttributeValuationCalculator() {}

  public static AttributeValuation calculate(
      Player player,
      LocalDate date,
      PreparedValuationMetrics metrics,
      ValuationProperties properties,
      Instant calculatedAt) {
    BigDecimal disciplinePenalty = ValuationMetricPipeline.disciplinePenalty(metrics.values());
    BigDecimal disciplineAdjustment =
        ValuationMetricPipeline.adjustment(properties.disciplineMaximumImpact(), disciplinePenalty);
    BigDecimal shooting = null, passing = null, dribbling = null, defending = null, physical = null;
    BigDecimal saving = null, distribution = null, activity = null, base;
    if (metrics.position() == GeneralPosition.GOALKEEPER) {
      saving = weighted(metrics, "saves_per_match", ".60", "saveOutOfBox", ".40");
      distribution =
          weighted(
              metrics,
              "pass_completion_percentage",
              ".60",
              "successful_long_balls_per_match",
              ".25",
              "passes_per_match",
              ".15");
      activity =
          weighted(
              metrics,
              "clearances_per_match",
              ".50",
              "aerial_duels_won_per_match",
              ".30",
              "passes_per_match",
              ".20");
      base =
          saving
              .multiply(new BigDecimal(".55"))
              .add(distribution.multiply(new BigDecimal(".30")))
              .add(activity.multiply(new BigDecimal(".15")));
    } else {
      shooting =
          weighted(
              metrics,
              "goalsPer90",
              ".60",
              "shots_per_match",
              ".25",
              "outside_box_goals_per_match",
              ".15");
      passing =
          weighted(
              metrics,
              "pass_completion_percentage",
              ".30",
              "key_passes_per_match",
              ".25",
              "assistsPer90",
              ".20",
              "successful_long_balls_per_match",
              ".15",
              "passes_per_match",
              ".10");
      BigDecimal dribblingPositive =
          weighted(metrics, "dribbleSuccess", ".55", "successful_dribbles_per_match", ".30");
      BigDecimal dribblingPenalty =
          weighted(metrics, "dispossessed_per_match", ".60", "bad_controls_per_match", ".40");
      dribbling =
          ValuationMetricPipeline.clamp(
              dribblingPositive.add(
                  ValuationMetricPipeline.adjustment(
                      properties.attributePenaltyMaximumImpact(), dribblingPenalty)));
      BigDecimal defendingPositive =
          weighted(
              metrics,
              "tackles_per_match",
              ".35",
              "interceptions_per_match",
              ".30",
              "clearances_per_match",
              ".20",
              "blocksPerMatch",
              ".15");
      BigDecimal defendingPenalty = metrics.value("dribbled_past_per_match");
      defending =
          ValuationMetricPipeline.clamp(
              defendingPositive.add(
                  ValuationMetricPipeline.adjustment(
                      properties.attributePenaltyMaximumImpact(), defendingPenalty)));
      physical = weighted(metrics, "aerialWinRate", ".60", "aerial_duels_won_per_match", ".40");
      Map<String, BigDecimal> attributes =
          Map.of(
              "shooting",
              shooting,
              "passing",
              passing,
              "dribbling",
              dribbling,
              "defending",
              defending,
              "physical",
              physical);
      base = ValuationMetricPipeline.weighted(attributes, grlWeights(metrics.position()));
    }
    BigDecimal raw = base.add(disciplineAdjustment, ValuationMetricPipeline.CONTEXT);
    BigDecimal result = ValuationMetricPipeline.applyConfidence(raw, metrics.confidence());
    return new AttributeValuation(
        player,
        date,
        metrics.position(),
        metrics.sourceSummary(),
        metrics.sourceExtractionDate(),
        shooting,
        passing,
        dribbling,
        defending,
        physical,
        saving,
        distribution,
        activity,
        base,
        disciplineAdjustment,
        raw,
        metrics.confidence(),
        result,
        metrics.imputedMetrics().toArray(String[]::new),
        calculatedAt);
  }

  private static BigDecimal weighted(PreparedValuationMetrics metrics, String... entries) {
    java.util.LinkedHashMap<String, BigDecimal> weights = new java.util.LinkedHashMap<>();
    for (int index = 0; index < entries.length; index += 2)
      weights.put(entries[index], new BigDecimal(entries[index + 1]));
    return ValuationMetricPipeline.weighted(metrics.values(), weights);
  }

  private static Map<String, BigDecimal> grlWeights(GeneralPosition position) {
    return switch (position) {
      case FORWARD ->
          Map.of(
              "shooting",
              new BigDecimal(".35"),
              "passing",
              new BigDecimal(".20"),
              "dribbling",
              new BigDecimal(".25"),
              "defending",
              new BigDecimal(".08"),
              "physical",
              new BigDecimal(".12"));
      case MIDFIELDER ->
          Map.of(
              "shooting",
              new BigDecimal(".16"),
              "passing",
              new BigDecimal(".30"),
              "dribbling",
              new BigDecimal(".22"),
              "defending",
              new BigDecimal(".20"),
              "physical",
              new BigDecimal(".12"));
      case DEFENDER ->
          Map.of(
              "shooting",
              new BigDecimal(".09"),
              "passing",
              new BigDecimal(".18"),
              "dribbling",
              new BigDecimal(".13"),
              "defending",
              new BigDecimal(".35"),
              "physical",
              new BigDecimal(".25"));
      case GOALKEEPER -> Map.of();
    };
  }
}
