package footballmarket.models;

import footballmarket.models.enums.ExtractionRunStatus;
import footballmarket.models.enums.MetricSource;
import footballmarket.models.exceptions.InvalidPlayerException;
import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;
import lombok.Getter;

/** Ejecución auditable de un canal de estadísticas. */
@Entity
@Table(name = "player_statistics_extraction_runs")
@Getter
public class PlayerStatisticsExtractionRun {
  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "statistics_run_ids")
  @SequenceGenerator(
      name = "statistics_run_ids",
      sequenceName = "player_statistics_extraction_runs_id_seq",
      allocationSize = 1)
  private Long id;

  @Column(nullable = false, updatable = false)
  private LocalDate extractionDate;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, updatable = false)
  private MetricSource source;

  @Column(nullable = false, updatable = false)
  private Instant startedAt;

  private Instant finishedAt;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private ExtractionRunStatus status;

  @Column(nullable = false)
  private long eligiblePlayers;

  @Column(nullable = false)
  private long excludedWithoutReference;

  @Column(nullable = false)
  private long selectedPlayers;

  @Column(nullable = false)
  private long completedPlayers;

  @Column(nullable = false)
  private long noDataPlayers;

  @Column(nullable = false)
  private long failedPlayers;

  @Column(nullable = false)
  private long pendingPlayers;

  @Column(nullable = false)
  private long presentMetrics;

  @Column(nullable = false)
  private long absentMetrics;

  @Column(nullable = false)
  private long notDerivableMetrics;

  @Column(nullable = false)
  private long providerCalls;

  private String failureCode;

  protected PlayerStatisticsExtractionRun() {}

  public PlayerStatisticsExtractionRun(
      LocalDate extractionDate, MetricSource source, Instant startedAt) {
    if (extractionDate == null || source == null || startedAt == null) {
      throw new InvalidPlayerException("La ejecución de estadísticas es inconsistente");
    }
    this.extractionDate = extractionDate;
    this.source = source;
    this.startedAt = startedAt;
    this.status = ExtractionRunStatus.RUNNING;
  }

  /** Cierra una ejecución una sola vez con sus contadores finales. */
  public void finish(
      ExtractionRunStatus status,
      Instant finishedAt,
      long eligiblePlayers,
      long excludedWithoutReference,
      long selectedPlayers,
      long completedPlayers,
      long noDataPlayers,
      long failedPlayers,
      long pendingPlayers,
      long presentMetrics,
      long absentMetrics,
      long notDerivableMetrics,
      long providerCalls,
      String failureCode) {
    if (this.status != ExtractionRunStatus.RUNNING
        || status == null
        || status == ExtractionRunStatus.RUNNING
        || finishedAt == null
        || finishedAt.isBefore(this.startedAt)
        || hasNegative(
            eligiblePlayers,
            excludedWithoutReference,
            selectedPlayers,
            completedPlayers,
            noDataPlayers,
            failedPlayers,
            pendingPlayers,
            presentMetrics,
            absentMetrics,
            notDerivableMetrics,
            providerCalls)
        || selectedPlayers != completedPlayers + noDataPlayers + failedPlayers
        || pendingPlayers > eligiblePlayers) {
      throw new InvalidPlayerException("Los resultados de estadísticas son inconsistentes");
    }
    this.status = status;
    this.finishedAt = finishedAt;
    this.eligiblePlayers = eligiblePlayers;
    this.excludedWithoutReference = excludedWithoutReference;
    this.selectedPlayers = selectedPlayers;
    this.completedPlayers = completedPlayers;
    this.noDataPlayers = noDataPlayers;
    this.failedPlayers = failedPlayers;
    this.pendingPlayers = pendingPlayers;
    this.presentMetrics = presentMetrics;
    this.absentMetrics = absentMetrics;
    this.notDerivableMetrics = notDerivableMetrics;
    this.providerCalls = providerCalls;
    this.failureCode = failureCode;
  }

  private static boolean hasNegative(long... values) {
    for (long value : values) {
      if (value < 0) {
        return true;
      }
    }
    return false;
  }
}
