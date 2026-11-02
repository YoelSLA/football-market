package footballmarket.features.statistics.domain.models;

import footballmarket.features.catalog.domain.exceptions.InvalidPlayerException;
import footballmarket.features.catalog.domain.models.player.Player;
import footballmarket.features.statistics.domain.enums.MetricSource;
import footballmarket.features.statistics.domain.enums.PlayerExtractionResult;
import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;
import lombok.Getter;

/** Resultado histórico de un jugador dentro de una ejecución. */
@Entity
@Table(name = "player_statistics_run_items")
@Getter
public class PlayerStatisticsRunItem {
  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "statistics_item_ids")
  @SequenceGenerator(
      name = "statistics_item_ids",
      sequenceName = "player_statistics_run_items_id_seq",
      allocationSize = 1)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "run_id", nullable = false, updatable = false)
  private PlayerStatisticsExtractionRun run;

  @Column(name = "extraction_date", nullable = false, updatable = false)
  private LocalDate extractionDate;

  @Enumerated(EnumType.STRING)
  @Column(name = "source", nullable = false, updatable = false)
  private MetricSource source;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "player_id", nullable = false, updatable = false)
  private Player player;

  @Column(nullable = false, updatable = false)
  private String providerPlayerId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private PlayerExtractionResult result;

  @Column(nullable = false, updatable = false)
  private Instant startedAt;

  private Instant finishedAt;

  @Column(nullable = false)
  private int contextsCount;

  @Column(nullable = false)
  private int presentMetrics;

  @Column(nullable = false)
  private int absentMetrics;

  private String incidentCode;

  protected PlayerStatisticsRunItem() {}

  public PlayerStatisticsRunItem(
      PlayerStatisticsExtractionRun run,
      Player player,
      String providerPlayerId,
      Instant startedAt) {
    if (run == null
        || player == null
        || providerPlayerId == null
        || providerPlayerId.isBlank()
        || startedAt == null) {
      throw new InvalidPlayerException("El resultado de jugador requiere contexto completo");
    }
    this.run = run;
    this.extractionDate = run.getExtractionDate();
    this.source = run.getSource();
    this.player = player;
    this.providerPlayerId = providerPlayerId.strip();
    this.startedAt = startedAt;
  }

  /** Registra el resultado final inmutable del intento. */
  public void finish(
      PlayerExtractionResult result,
      Instant finishedAt,
      int contextsCount,
      int presentMetrics,
      int absentMetrics,
      String incidentCode) {
    if (this.result != null
        || result == null
        || finishedAt == null
        || finishedAt.isBefore(this.startedAt)
        || contextsCount < 0
        || presentMetrics < 0
        || absentMetrics < 0) {
      throw new InvalidPlayerException("El resultado final del jugador es inconsistente");
    }
    this.result = result;
    this.finishedAt = finishedAt;
    this.contextsCount = contextsCount;
    this.presentMetrics = presentMetrics;
    this.absentMetrics = absentMetrics;
    this.incidentCode = incidentCode;
  }
}
