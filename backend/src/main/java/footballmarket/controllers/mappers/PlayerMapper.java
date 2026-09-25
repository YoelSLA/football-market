package footballmarket.controllers.mappers;

import footballmarket.controllers.dtos.responses.PlayerResponseDTO;
import footballmarket.controllers.dtos.responses.PlayerSyncResponseDTO;
import footballmarket.controllers.dtos.responses.PlayersPageResponseDTO;
import footballmarket.models.Player;
import footballmarket.models.records.PlayerSynchronizationResult;
import org.springframework.data.domain.Page;

public final class PlayerMapper {
  private PlayerMapper() {}

  public static PlayerSyncResponseDTO toResponse(PlayerSynchronizationResult result) {
    return new PlayerSyncResponseDTO(
        result.obtained(),
        result.created(),
        result.updated(),
        result.markedInactive(),
        result.discardedInvalid());
  }

  public static PlayerResponseDTO toResponse(Player player) {
    return new PlayerResponseDTO(
        player.getId(),
        player.getName(),
        player.getTeam(),
        player.getLeague(),
        player.getPosition());
  }

  public static PlayersPageResponseDTO toResponse(Page<Player> page) {
    return new PlayersPageResponseDTO(
        page.getContent().stream().map(PlayerMapper::toResponse).toList(),
        page.getNumber(),
        page.getSize(),
        page.getTotalElements(),
        page.getTotalPages());
  }
}
