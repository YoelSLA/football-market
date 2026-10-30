package footballmarket.features.images.domain.enums;

/** Resultado de la evaluación individual de un jugador. */
public enum PlayerImageSyncRunItemResult {
  FOUND,
  NOT_FOUND,
  RETRYABLE_ERROR,
  FAILED,
  SKIPPED,
  INTERRUPTED
}
