package footballmarket.features.images.domain.enums;

/** Estado operativo de la búsqueda de imagen de un jugador. */
public enum PlayerImageResolutionStatus {
  PENDING,
  FOUND,
  NOT_FOUND,
  RETRYABLE_ERROR,
  FAILED
}
