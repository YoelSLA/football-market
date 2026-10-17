package footballmarket.models;

import footballmarket.models.enums.PlayerImageResolutionStatus;
import footballmarket.models.enums.PlayerImageSkipReason;
import footballmarket.models.enums.PlayerImageSyncRunItemResult;
import footballmarket.models.exceptions.InvalidPlayerException;
import footballmarket.models.records.PlayerImageSyncRunItemRecord;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import lombok.Getter;

/** Auditoría de una evaluación individual dentro de una ejecución de imágenes. */
@Entity
@Table(
    name = "player_image_sync_run_items",
    uniqueConstraints =
        @UniqueConstraint(
            name = "uk_player_image_sync_run_items_run_player",
            columnNames = {"run_id", "player_id"}))
@Getter
public class PlayerImageSyncRunItem {
  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "player_image_sync_run_items_ids")
  @SequenceGenerator(
      name = "player_image_sync_run_items_ids",
      sequenceName = "player_image_sync_run_items_id_seq",
      allocationSize = 1)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "run_id", nullable = false)
  private PlayerImageSyncRun run;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "player_id", nullable = false)
  private Player player;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 32)
  private PlayerImageResolutionStatus previousState;

  @Enumerated(EnumType.STRING)
  @Column(length = 32)
  private PlayerImageResolutionStatus finalState;

  @Enumerated(EnumType.STRING)
  @Column(length = 32)
  private PlayerImageSyncRunItemResult result;

  private boolean identityResolved;
  private boolean imageFoundOrUpdated;

  @Enumerated(EnumType.STRING)
  @Column(length = 32)
  private PlayerImageSkipReason skipReason;

  @Column(name = "conflict")
  private boolean conflict;

  private boolean errorOccurred;

  @Column(length = 512)
  private String outcomeDetail;

  private Instant finishedAt;

  protected PlayerImageSyncRunItem() {}

  public PlayerImageSyncRunItem(
      PlayerImageSyncRun run, Player player, PlayerImageResolutionStatus previousState) {
    if (run == null || player == null || previousState == null) {
      throw new InvalidPlayerException("El item requiere ejecución, jugador y estado anterior");
    }
    this.run = run;
    this.player = player;
    this.previousState = previousState;
  }

  /** Finaliza un item con un motivo seguro y una resolución durable. */
  public void finish(
      PlayerImageResolutionStatus finalState,
      PlayerImageSyncRunItemResult result,
      PlayerImageSkipReason skipReason,
      boolean identityResolved,
      boolean imageFoundOrUpdated,
      boolean conflict,
      boolean errorOccurred,
      String outcomeDetail,
      Instant at) {
    if (this.finishedAt != null
        || finalState == null
        || result == null
        || at == null
        || (result == PlayerImageSyncRunItemResult.SKIPPED) != (skipReason != null)
        || (conflict && result != PlayerImageSyncRunItemResult.FAILED)
        || result == PlayerImageSyncRunItemResult.INTERRUPTED) {
      throw new InvalidPlayerException("El resultado de la evaluación es inconsistente");
    }
    this.finalState = finalState;
    this.result = result;
    this.skipReason = skipReason;
    this.identityResolved = identityResolved;
    this.imageFoundOrUpdated = imageFoundOrUpdated;
    this.conflict = conflict;
    this.errorOccurred = errorOccurred;
    this.outcomeDetail = outcomeDetail;
    this.finishedAt = at;
  }

  /** Interrumpe exclusivamente un item inconcluso, sin alterar la resolución del jugador. */
  public void interrupt(PlayerImageResolutionStatus durableState, Instant at) {
    if (this.finishedAt != null || durableState == null || at == null) {
      throw new InvalidPlayerException("Solo se puede interrumpir un item inconcluso");
    }
    this.finalState = durableState;
    this.result = PlayerImageSyncRunItemResult.INTERRUPTED;
    this.errorOccurred = true;
    this.outcomeDetail = "Interrumpido por reinicio";
    this.finishedAt = at;
  }

  public PlayerImageSyncRunItemRecord detail() {
    return new PlayerImageSyncRunItemRecord(
        this.player.getId(),
        this.previousState,
        this.finalState,
        this.result,
        this.identityResolved,
        this.imageFoundOrUpdated,
        this.skipReason != null,
        this.skipReason,
        this.conflict,
        this.errorOccurred,
        this.outcomeDetail);
  }
}
