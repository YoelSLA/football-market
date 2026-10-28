package footballmarket.features.images.domain.records;

import footballmarket.features.images.domain.enums.PlayerImageResolutionStatus;
import footballmarket.features.images.domain.enums.PlayerImageSkipReason;
import footballmarket.features.images.domain.enums.PlayerImageSyncRunItemResult;

/** Proyección pública del resultado auditado de un jugador. */
public record PlayerImageSyncRunItemRecord(
    Long playerId,
    PlayerImageResolutionStatus previousState,
    PlayerImageResolutionStatus finalState,
    PlayerImageSyncRunItemResult result,
    boolean identityResolved,
    boolean imageFoundOrUpdated,
    boolean skipped,
    PlayerImageSkipReason skipReason,
    boolean conflict,
    boolean errorOccurred,
    String outcomeDetail) {}
