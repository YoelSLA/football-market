package footballmarket.models;

import java.util.List;

/** Foto completa: candidatos válidos y contadores anteriores a la consolidación por ID. */
public record PlayerSnapshot(List<Player> players, int obtained, int discardedInvalid) {
  public PlayerSnapshot {
    players = List.copyOf(players);
    if (obtained < 0
        || discardedInvalid < 0
        || discardedInvalid > obtained
        || players.size() > obtained - discardedInvalid) {
      throw new IllegalArgumentException("Los contadores de la foto son inválidos");
    }
  }
}
