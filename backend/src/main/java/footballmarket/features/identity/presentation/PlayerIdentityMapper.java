package footballmarket.features.identity.presentation;

import footballmarket.features.identity.domain.records.PlayerIdentitySyncSummary;
import footballmarket.features.identity.presentation.dtos.response.PlayerIdentitySyncRunResponseDTO;

/** Transformación estructural del resultado global al contrato HTTP. */
public final class PlayerIdentityMapper {
  private PlayerIdentityMapper() {}

  public static PlayerIdentitySyncRunResponseDTO toResponse(PlayerIdentitySyncSummary summary) {
    return new PlayerIdentitySyncRunResponseDTO(
        summary.status().name(),
        summary.selectedPlayers(),
        summary.referencedPlayers(),
        summary.noMatchPlayers(),
        summary.unresolvedPlayers());
  }
}
