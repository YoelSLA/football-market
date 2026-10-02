package footballmarket.controllers.dtos.responses;

import io.swagger.v3.oas.annotations.media.Schema;

/** Resultado auditable de un jugador, sin datos crudos del proveedor. */
public record PlayerImageSyncRunItemResponseDTO(
    Long playerId,
    String previousState,
    @Schema(nullable = true) String finalState,
    @Schema(nullable = true) String result,
    boolean identityResolved,
    boolean imageFoundOrUpdated,
    boolean skipped,
    @Schema(nullable = true) String skipReason,
    boolean conflict,
    boolean errorOccurred,
    @Schema(nullable = true) String outcomeDetail) {}
