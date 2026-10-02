package footballmarket.models.records;

import footballmarket.models.enums.PlayerImageResolutionStatus;
import footballmarket.models.enums.PlayerImageSkipReason;
import footballmarket.models.enums.PlayerImageSyncRunItemResult;

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
