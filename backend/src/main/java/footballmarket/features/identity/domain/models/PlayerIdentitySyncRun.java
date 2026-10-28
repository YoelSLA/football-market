package footballmarket.features.identity.domain.models;

import footballmarket.features.catalog.domain.exceptions.InvalidPlayerException;
import footballmarket.features.identity.domain.enums.PlayerIdentityPassType;
import footballmarket.features.identity.domain.enums.PlayerIdentityRunStatus;
import footballmarket.features.identity.domain.records.PlayerIdentitySyncCounters;
import jakarta.persistence.*;
import java.time.Instant;
import lombok.Getter;

/** Ejecución auditable; cada invocación genera su propia identidad y no reabre otras. */
@Entity
@Table(name = "player_identity_sync_runs")
@Getter
public class PlayerIdentitySyncRun {
  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "identity_run_ids")
  @SequenceGenerator(
      name = "identity_run_ids",
      sequenceName = "player_identity_sync_runs_id_seq",
      allocationSize = 1)
  private Long id;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private PlayerIdentityPassType passType;

  private Long sourceRunId;

  @Column(nullable = false)
  private Instant startedAt;

  private Instant finishedAt;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private PlayerIdentityRunStatus status;

  private String failureReason;

  @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.JSON)
  @Column(columnDefinition = "jsonb")
  private PlayerIdentitySyncCounters counters;

  protected PlayerIdentitySyncRun() {}

  public PlayerIdentitySyncRun(
      PlayerIdentityPassType passType, Long sourceRunId, Instant startedAt) {
    if (passType == null
        || startedAt == null
        || (passType == PlayerIdentityPassType.CURRENT && sourceRunId != null)
        || (passType == PlayerIdentityPassType.HISTORICAL
            && (sourceRunId == null || sourceRunId < 1))) {
      throw new InvalidPlayerException("El pase y su origen son inconsistentes");
    }
    this.passType = passType;
    this.sourceRunId = sourceRunId;
    this.startedAt = startedAt;
    this.status = PlayerIdentityRunStatus.RUNNING;
  }

  /**
   * Cierra una sola vez con resultados finales y descubrimiento completo o explícitamente fallido.
   */
  public void finish(
      PlayerIdentitySyncCounters counters,
      long completedLeagues,
      Instant at,
      String failureReason) {
    if (this.status != PlayerIdentityRunStatus.RUNNING
        || counters == null
        || counters.pendingPlayers() != 0
        || at == null
        || at.isBefore(this.startedAt)
        || completedLeagues < 0
        || completedLeagues > 5) {
      throw new InvalidPlayerException("La ejecución no puede cerrarse con trabajo pendiente");
    }
    boolean empty = counters.selectedPlayers() == 0;
    boolean complete =
        empty
            || (completedLeagues == 5
                && counters.technicalErrors() == 0
                && counters.providerErrors() == 0
                && failureReason == null);
    this.status =
        complete
            ? PlayerIdentityRunStatus.COMPLETED
            : completedLeagues > 0 || counters.finalNonTechnicalPlayers() > 0
                ? PlayerIdentityRunStatus.PARTIAL
                : PlayerIdentityRunStatus.FAILED;
    this.counters = counters;
    this.finishedAt = at;
    this.failureReason = failureReason;
  }
}
