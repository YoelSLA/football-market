package footballmarket.models.records;

import footballmarket.models.exceptions.InvalidPlayerSnapshotException;
import java.util.List;

/** Foto inmutable: candidatos válidos y contadores anteriores a consolidar la identidad externa. */
public record PlayerSnapshot(
    List<PlayerCandidate> players,
    int obtained,
    int discardedInvalid,
    List<LeagueCandidate> leagues,
    List<TeamCandidate> teams,
    List<InvalidPlayerObservation> invalidPlayers) {
  /** Mantiene lectura de fotos del contrato anterior, sin inferir identidades de textos. */
  public PlayerSnapshot(List<PlayerCandidate> players, int obtained, int discardedInvalid) {
    this(players, obtained, discardedInvalid, List.of(), List.of(), List.of());
  }

  public PlayerSnapshot {
    players = List.copyOf(players);
    leagues = List.copyOf(leagues);
    teams = List.copyOf(teams);
    invalidPlayers = List.copyOf(invalidPlayers);
    if (obtained < 0
        || discardedInvalid < 0
        || discardedInvalid > obtained
        || players.size() > obtained - discardedInvalid) {
      throw new InvalidPlayerSnapshotException();
    }
  }
}
