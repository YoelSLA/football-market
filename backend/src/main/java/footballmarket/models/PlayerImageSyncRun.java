package footballmarket.models;

import footballmarket.models.enums.PlayerImageSyncRunStatus;
import footballmarket.models.exceptions.InvalidPlayerException;
import footballmarket.models.records.PlayerImageSyncCounters;
import footballmarket.models.records.PlayerImageSyncSummary;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import java.time.Duration;
import java.time.Instant;
import lombok.Getter;

/** Ejecución persistida con contabilidad incremental y estado final derivado de sus resultados. */
@Entity
@Table(name = "player_image_sync_runs")
@Getter
public class PlayerImageSyncRun {
  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "player_image_sync_runs_ids")
  @SequenceGenerator(
      name = "player_image_sync_runs_ids",
      sequenceName = "player_image_sync_runs_id_seq",
      allocationSize = 1)
  private Long id;

  @Column(nullable = false)
  private Instant startedAt;

  private Instant finishedAt;

  @Column(name = "force", nullable = false)
  private boolean force;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 32)
  private PlayerImageSyncRunStatus status;

  private String failureReason;

  private int evaluated;
  private int processed;
  private int found;
  private int notFound;
  private int retryableErrors;
  private int failed;
  private int conflicts;
  private int skippedFound;
  private int skippedRetryWindow;
  private int skippedFailed;
  private int interrupted;

  protected PlayerImageSyncRun() {}

  public PlayerImageSyncRun(boolean force, Instant startedAt) {
    if (startedAt == null) {
      throw new InvalidPlayerException("El inicio de la ejecución es obligatorio");
    }
    this.force = force;
    this.startedAt = startedAt;
    this.status = PlayerImageSyncRunStatus.RUNNING;
  }

  public PlayerImageSyncCounters counters() {
    return new PlayerImageSyncCounters(
        this.evaluated,
        this.processed,
        this.found,
        this.notFound,
        this.retryableErrors,
        this.failed,
        this.conflicts,
        this.skippedFound,
        this.skippedRetryWindow,
        this.skippedFailed,
        this.interrupted);
  }

  /** Cuenta al activo inspeccionado en cuanto se abre su único item, aun si se interrumpe. */
  public void evaluate() {
    if (this.status != PlayerImageSyncRunStatus.RUNNING) {
      throw new InvalidPlayerException("Solo puede evaluarse un jugador en un run activo");
    }
    this.evaluated++;
  }

  /** Agrega el resultado de un item abierto sin volver a sumar la evaluación. */
  public void result(
      footballmarket.models.enums.PlayerImageSyncRunItemResult result,
      footballmarket.models.enums.PlayerImageSkipReason reason,
      boolean attempted,
      boolean conflict) {
    this.record(this.counters().addOutcome(result, reason, attempted, conflict));
  }

  /** Sustituye los contadores después de confirmar un item individual. */
  public void record(PlayerImageSyncCounters counters) {
    if (this.status != PlayerImageSyncRunStatus.RUNNING
        || counters == null
        || counters.evaluated() < this.evaluated) {
      throw new InvalidPlayerException("No puede actualizarse la contabilidad de este run");
    }
    this.evaluated = counters.evaluated();
    this.processed = counters.processed();
    this.found = counters.found();
    this.notFound = counters.notFound();
    this.retryableErrors = counters.retryableErrors();
    this.failed = counters.failed();
    this.conflicts = counters.conflicts();
    this.skippedFound = counters.skippedFound();
    this.skippedRetryWindow = counters.skippedRetryWindow();
    this.skippedFailed = counters.skippedFailed();
    this.interrupted = counters.interrupted();
  }

  /** Cierra normalmente sin convertir una ausencia de imagen en error de ejecución. */
  public void complete(Instant at) {
    this.requireRunningAndTime(at);
    this.status =
        this.retryableErrors > 0 || this.failed > 0 || this.conflicts > 0
            ? PlayerImageSyncRunStatus.PARTIAL
            : PlayerImageSyncRunStatus.COMPLETED;
    this.finishedAt = at;
  }

  /** Cierra ante un fallo global o la recuperación de una ejecución huérfana. */
  public void fail(Instant at, String reason) {
    this.requireRunningAndTime(at);
    if (reason == null || reason.isBlank()) {
      throw new InvalidPlayerException("El fallo global requiere un motivo");
    }
    this.status = PlayerImageSyncRunStatus.FAILED;
    this.failureReason = reason;
    this.finishedAt = at;
  }

  public PlayerImageSyncSummary summary() {
    Long duration =
        this.finishedAt == null
            ? null
            : Duration.between(this.startedAt, this.finishedAt).toMillis();
    return new PlayerImageSyncSummary(
        this.id,
        this.status,
        this.force,
        this.startedAt,
        this.finishedAt,
        duration,
        this.failureReason,
        this.counters());
  }

  private void requireRunningAndTime(Instant at) {
    if (this.status != PlayerImageSyncRunStatus.RUNNING
        || at == null
        || at.isBefore(this.startedAt)) {
      throw new InvalidPlayerException("No puede cerrarse esta ejecución");
    }
  }
}
