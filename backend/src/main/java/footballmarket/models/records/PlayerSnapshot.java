package footballmarket.models.records;

import footballmarket.models.exceptions.InvalidPlayerSnapshotException;
import java.util.List;

/** Foto inmutable: candidatos válidos y contadores anteriores a consolidar la identidad externa. */
public record PlayerSnapshot(List<PlayerCandidate> players, int obtained, int discardedInvalid) {
  public PlayerSnapshot {
    players = List.copyOf(players);
    if (obtained < 0
        || discardedInvalid < 0
        || discardedInvalid > obtained
        || players.size() > obtained - discardedInvalid) {
      throw new InvalidPlayerSnapshotException();
    }
  }
}
