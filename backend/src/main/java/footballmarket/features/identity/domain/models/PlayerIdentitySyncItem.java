package footballmarket.features.identity.domain.models;

import footballmarket.features.catalog.domain.exceptions.InvalidPlayerException;
import java.util.List;
import lombok.Getter;

/** Datos locales capturados en memoria para resolver un jugador sin sobrescribir el catálogo. */
@Getter
public final class PlayerIdentitySyncItem {
  private final Long playerId;
  private final String playerName;
  private final String teamName;
  private final Long leagueId;
  private final List<String> initialExternalIds;

  public PlayerIdentitySyncItem(
      Long playerId,
      String playerName,
      String teamName,
      Long leagueId,
      List<String> initialExternalIds) {
    if (playerId == null || leagueId == null || initialExternalIds == null) {
      throw new InvalidPlayerException("La captura requiere jugador, liga y referencias");
    }
    this.playerId = playerId;
    this.playerName = playerName;
    this.teamName = teamName;
    this.leagueId = leagueId;
    this.initialExternalIds = initialExternalIds.stream().distinct().sorted().toList();
  }

  public String getInitialExternalId() {
    return this.initialExternalIds.size() == 1 ? this.initialExternalIds.getFirst() : null;
  }
}
