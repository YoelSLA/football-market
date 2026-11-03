package footballmarket.features.valuation.domain.models;

import footballmarket.features.catalog.domain.models.player.Player;
import footballmarket.features.valuation.domain.enums.GeneralPosition;
import footballmarket.features.valuation.domain.enums.MetricSourceSummary;
import footballmarket.features.valuation.domain.enums.ValuationStrategy;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import lombok.Getter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** Resultado persistente de POSITIONAL_PERFORMANCE v1. */
@Entity
@Table(name = "player_positional_valuations")
@Getter
public class PositionalValuation {
  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "positional_valuation_ids")
  @SequenceGenerator(
      name = "positional_valuation_ids",
      sequenceName = "player_positional_valuations_id_seq",
      allocationSize = 1)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "player_id", nullable = false)
  private Player player;

  @Column(name = "valuation_date", nullable = false)
  private LocalDate valuationDate;

  @Enumerated(EnumType.STRING)
  @Column(name = "strategy_name", nullable = false)
  private ValuationStrategy strategyName;

  @Column(name = "strategy_version", nullable = false)
  private String strategyVersion;

  @Enumerated(EnumType.STRING)
  @Column(name = "general_position", nullable = false)
  private GeneralPosition generalPosition;

  @Enumerated(EnumType.STRING)
  @Column(name = "source_summary", nullable = false)
  private MetricSourceSummary sourceSummary;

  @Column(name = "source_extraction_date", nullable = false)
  private LocalDate sourceExtractionDate;

  @Column(name = "positive_score", nullable = false)
  private BigDecimal positiveScore;

  @Column(name = "performance_penalty")
  private BigDecimal performancePenalty;

  @Column(name = "performance_adjustment")
  private BigDecimal performanceAdjustment;

  @Column(name = "discipline_penalty", nullable = false)
  private BigDecimal disciplinePenalty;

  @Column(name = "discipline_adjustment", nullable = false)
  private BigDecimal disciplineAdjustment;

  @Column(name = "raw_score", nullable = false)
  private BigDecimal rawScore;

  @Column(nullable = false)
  private BigDecimal confidence;

  @Column(name = "final_score", nullable = false)
  private BigDecimal finalScore;

  @JdbcTypeCode(SqlTypes.ARRAY)
  @Column(name = "imputed_metrics", nullable = false, columnDefinition = "text[]")
  private String[] imputedMetrics;

  @Column(name = "calculated_at", nullable = false)
  private Instant calculatedAt;

  protected PositionalValuation() {}

  public PositionalValuation(
      Player player,
      LocalDate valuationDate,
      GeneralPosition generalPosition,
      MetricSourceSummary sourceSummary,
      LocalDate sourceExtractionDate,
      BigDecimal positiveScore,
      BigDecimal performancePenalty,
      BigDecimal performanceAdjustment,
      BigDecimal disciplinePenalty,
      BigDecimal disciplineAdjustment,
      BigDecimal rawScore,
      BigDecimal confidence,
      BigDecimal finalScore,
      String[] imputedMetrics,
      Instant calculatedAt) {
    this.player = player;
    this.valuationDate = valuationDate;
    this.strategyName = ValuationStrategy.POSITIONAL_PERFORMANCE;
    this.strategyVersion = "v1";
    this.generalPosition = generalPosition;
    this.sourceSummary = sourceSummary;
    this.sourceExtractionDate = sourceExtractionDate;
    this.positiveScore = ValuationMetricPipeline.persisted(positiveScore);
    this.performancePenalty =
        performancePenalty == null ? null : ValuationMetricPipeline.persisted(performancePenalty);
    this.performanceAdjustment =
        performanceAdjustment == null
            ? null
            : ValuationMetricPipeline.persisted(performanceAdjustment);
    this.disciplinePenalty = ValuationMetricPipeline.persisted(disciplinePenalty);
    this.disciplineAdjustment = ValuationMetricPipeline.persisted(disciplineAdjustment);
    this.rawScore = ValuationMetricPipeline.persisted(rawScore);
    this.confidence = ValuationMetricPipeline.persisted(confidence);
    this.finalScore = ValuationMetricPipeline.persisted(ValuationMetricPipeline.clamp(finalScore));
    this.imputedMetrics = imputedMetrics.clone();
    this.calculatedAt = calculatedAt;
  }
}
