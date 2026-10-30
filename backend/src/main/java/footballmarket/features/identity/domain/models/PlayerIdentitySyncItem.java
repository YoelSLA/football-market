package footballmarket.features.identity.domain.models;

import footballmarket.features.catalog.domain.exceptions.InvalidPlayerException;
import footballmarket.features.identity.domain.enums.PlayerIdentityItemResult;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.List;
import lombok.Getter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** Snapshot histórico independiente del catálogo mutable y decisión exclusiva del jugador. */
@Entity
@Table(
    name = "player_identity_sync_items",
    uniqueConstraints = @UniqueConstraint(columnNames = {"run_id", "player_id"}))
@Getter
public class PlayerIdentitySyncItem {
  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "identity_item_ids")
  @SequenceGenerator(
      name = "identity_item_ids",
      sequenceName = "player_identity_sync_items_id_seq",
      allocationSize = 1)
  private Long id;

  @Column(nullable = false, updatable = false)
  private Long runId;

  @Column(nullable = false, updatable = false)
  private Long playerId;

  private String playerName;
  private Long teamId;
  private String teamName;
  private Long leagueId;
  private String leagueName;
  private String initialExternalId;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(nullable = false, columnDefinition = "jsonb")
  private List<String> initialExternalIds;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private PlayerIdentityItemResult result;

  private String reasonCode;
  private String reasonMessage;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(nullable = false, columnDefinition = "jsonb")
  private List<String> candidateExternalIds = List.of();

  private String confirmedExternalId;
  private Instant decidedAt;

  protected PlayerIdentitySyncItem() {}

  public PlayerIdentitySyncItem(
      Long runId,
      Long playerId,
      String playerName,
      Long teamId,
      String teamName,
      Long leagueId,
      String leagueName,
      List<String> initialExternalIds) {
    if (runId == null || playerId == null || initialExternalIds == null) {
      throw new InvalidPlayerException("El snapshot requiere ejecución y jugador");
    }
    this.runId = runId;
    this.playerId = playerId;
    this.playerName = playerName;
    this.teamId = teamId;
    this.teamName = teamName;
    this.leagueId = leagueId;
    this.leagueName = leagueName;
    this.initialExternalIds = initialExternalIds.stream().distinct().sorted().toList();
    this.initialExternalId =
        this.initialExternalIds.size() == 1 ? this.initialExternalIds.getFirst() : null;
    this.result = PlayerIdentityItemResult.PENDING;
  }

  /** Finaliza un item una vez, validando la pertenencia de la evidencia a su ejecución. */
  public void decide(
      PlayerIdentityItemResult result,
      String reasonCode,
      String reasonMessage,
      List<WhoScoredIdentityCandidate> candidates,
      String confirmedExternalId,
      Instant at) {
    if (this.result != PlayerIdentityItemResult.PENDING
        || result == null
        || result == PlayerIdentityItemResult.PENDING
        || at == null
        || reasonCode == null
        || candidates == null
        || candidates.stream().anyMatch(candidate -> !this.runId.equals(candidate.getRunId()))
        || ((result == PlayerIdentityItemResult.RESOLVED
                || result == PlayerIdentityItemResult.ALREADY_REFERENCED)
            != (confirmedExternalId != null))) {
      throw new InvalidPlayerException("La decisión de identidad es inconsistente");
    }
    this.result = result;
    this.reasonCode = reasonCode;
    this.reasonMessage = reasonMessage;
    this.candidateExternalIds =
        candidates.stream()
            .map(WhoScoredIdentityCandidate::getExternalId)
            .distinct()
            .sorted()
            .toList();
    this.confirmedExternalId = confirmedExternalId;
    this.decidedAt = at;
  }
}
