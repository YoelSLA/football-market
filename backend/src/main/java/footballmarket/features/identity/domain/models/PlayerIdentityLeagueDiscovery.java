package footballmarket.features.identity.domain.models;

import footballmarket.features.catalog.domain.exceptions.InvalidPlayerException;
import footballmarket.features.identity.domain.enums.PlayerIdentityLeagueStatus;
import jakarta.persistence.*;
import java.time.Instant;
import lombok.Getter;

/** Ámbito capturado; una liga solo publica candidatos después de completar todas sus páginas. */
@Entity
@Table(
    name = "player_identity_league_discoveries",
    uniqueConstraints = @UniqueConstraint(columnNames = {"run_id", "league_id"}))
@Getter
public class PlayerIdentityLeagueDiscovery {
  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "identity_league_ids")
  @SequenceGenerator(
      name = "identity_league_ids",
      sequenceName = "player_identity_league_discoveries_id_seq",
      allocationSize = 1)
  private Long id;

  @Column(nullable = false, updatable = false)
  private Long runId;

  @Column(nullable = false, updatable = false)
  private Long leagueId;

  @Column(nullable = false, updatable = false)
  private String leagueName;

  @Column(nullable = false, updatable = false)
  private int tournamentId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private PlayerIdentityLeagueStatus status;

  private Instant startedAt;
  private Instant finishedAt;
  private long pagesRead;
  private Integer lastPage;
  private Integer totalPages;
  private Long totalResults;
  private long rowsRead;
  private long discardedRows;
  private String errorCode;
  private String errorMessage;
  private Integer failedPage;
  private Integer httpStatus;

  protected PlayerIdentityLeagueDiscovery() {}

  public PlayerIdentityLeagueDiscovery(
      Long runId, Long leagueId, String leagueName, int tournamentId) {
    if (runId == null || leagueId == null || leagueName == null || tournamentId < 1) {
      throw new InvalidPlayerException("El ámbito requiere un binding válido");
    }
    this.runId = runId;
    this.leagueId = leagueId;
    this.leagueName = leagueName;
    this.tournamentId = tournamentId;
    this.status = PlayerIdentityLeagueStatus.NOT_STARTED;
  }

  public void start(Instant at) {
    if (this.status != PlayerIdentityLeagueStatus.NOT_STARTED || at == null) {
      throw new InvalidPlayerException("El ámbito ya fue iniciado");
    }
    this.startedAt = at;
    this.status = PlayerIdentityLeagueStatus.DISCOVERING;
  }

  /** Registra filas originales, incluidos los descartes, sin contar identidades deduplicadas. */
  public void recordPage(int page, int totalPages, long totalResults, long rows, long discarded) {
    if (this.status != PlayerIdentityLeagueStatus.DISCOVERING
        || page != this.pagesRead + 1
        || totalPages < 0
        || totalResults < 0
        || rows < 0
        || discarded < 0
        || discarded > rows
        || (this.totalPages != null
            && (this.totalPages != totalPages || this.totalResults != totalResults))) {
      throw new InvalidPlayerException("Los metadatos del ámbito son inconsistentes");
    }
    this.pagesRead++;
    this.lastPage = page;
    this.totalPages = totalPages;
    this.totalResults = totalResults;
    this.rowsRead += rows;
    this.discardedRows += discarded;
  }

  public void complete(Instant at) {
    if (this.status != PlayerIdentityLeagueStatus.DISCOVERING
        || this.totalResults == null
        || this.rowsRead != this.totalResults
        || (this.totalResults > 0 && !this.lastPage.equals(this.totalPages))) {
      throw new InvalidPlayerException("No se puede completar un ámbito incompleto");
    }
    this.finish(PlayerIdentityLeagueStatus.COMPLETED, at);
  }

  public void fail(String code, String message, Integer page, Integer httpStatus, Instant at) {
    if (this.status != PlayerIdentityLeagueStatus.NOT_STARTED
        && this.status != PlayerIdentityLeagueStatus.DISCOVERING) {
      throw new InvalidPlayerException("El ámbito ya tiene resultado final");
    }
    this.errorCode = code;
    this.errorMessage = message;
    this.failedPage = page;
    this.httpStatus = httpStatus;
    this.finish(PlayerIdentityLeagueStatus.FAILED, at);
  }

  public void skip(Instant at) {
    if (this.status != PlayerIdentityLeagueStatus.NOT_STARTED) {
      throw new InvalidPlayerException("El ámbito iniciado no puede omitirse");
    }
    this.finish(PlayerIdentityLeagueStatus.SKIPPED, at);
  }

  private void finish(PlayerIdentityLeagueStatus status, Instant at) {
    if (at == null || (this.startedAt != null && at.isBefore(this.startedAt))) {
      throw new InvalidPlayerException("El fin no puede preceder al inicio");
    }
    this.status = status;
    this.finishedAt = at;
  }
}
