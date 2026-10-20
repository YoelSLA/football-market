package footballmarket.models;

import footballmarket.models.enums.GeneralPosition;
import footballmarket.models.enums.MetricSourceSummary;
import footballmarket.models.enums.ValuationStrategy;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import lombok.Getter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** Resultado persistente de GENERAL_ATTRIBUTES v1. */
@Entity
@Table(name = "player_attribute_valuations")
@Getter
public class AttributeValuation {
  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "attribute_valuation_ids")
  @SequenceGenerator(
      name = "attribute_valuation_ids",
      sequenceName = "player_attribute_valuations_id_seq",
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

  private BigDecimal shooting;
  private BigDecimal passing;
  private BigDecimal dribbling;
  private BigDecimal defending;
  private BigDecimal physical;
  private BigDecimal saving;
  private BigDecimal distribution;
  private BigDecimal activity;

  @Column(name = "base_grl", nullable = false)
  private BigDecimal baseGrl;

  @Column(name = "discipline_adjustment", nullable = false)
  private BigDecimal disciplineAdjustment;

  @Column(name = "raw_grl", nullable = false)
  private BigDecimal rawGrl;

  @Column(nullable = false)
  private BigDecimal confidence;

  @Column(name = "final_grl", nullable = false)
  private BigDecimal finalGrl;

  @JdbcTypeCode(SqlTypes.ARRAY)
  @Column(name = "imputed_metrics", nullable = false, columnDefinition = "text[]")
  private String[] imputedMetrics;

  @Column(name = "calculated_at", nullable = false)
  private Instant calculatedAt;

  protected AttributeValuation() {}

  public AttributeValuation(
      Player player,
      LocalDate valuationDate,
      GeneralPosition position,
      MetricSourceSummary source,
      LocalDate sourceDate,
      BigDecimal shooting,
      BigDecimal passing,
      BigDecimal dribbling,
      BigDecimal defending,
      BigDecimal physical,
      BigDecimal saving,
      BigDecimal distribution,
      BigDecimal activity,
      BigDecimal baseGrl,
      BigDecimal disciplineAdjustment,
      BigDecimal rawGrl,
      BigDecimal confidence,
      BigDecimal finalGrl,
      String[] imputedMetrics,
      Instant calculatedAt) {
    this.player = player;
    this.valuationDate = valuationDate;
    this.strategyName = ValuationStrategy.GENERAL_ATTRIBUTES;
    this.strategyVersion = "v1";
    this.generalPosition = position;
    this.sourceSummary = source;
    this.sourceExtractionDate = sourceDate;
    this.shooting = scale(shooting);
    this.passing = scale(passing);
    this.dribbling = scale(dribbling);
    this.defending = scale(defending);
    this.physical = scale(physical);
    this.saving = scale(saving);
    this.distribution = scale(distribution);
    this.activity = scale(activity);
    this.baseGrl = scale(baseGrl);
    this.disciplineAdjustment = scale(disciplineAdjustment);
    this.rawGrl = scale(rawGrl);
    this.confidence = scale(confidence);
    this.finalGrl = scale(ValuationMetricPipeline.clamp(finalGrl));
    this.imputedMetrics = imputedMetrics.clone();
    this.calculatedAt = calculatedAt;
  }

  private static BigDecimal scale(BigDecimal value) {
    return value == null ? null : ValuationMetricPipeline.persisted(value);
  }
}
