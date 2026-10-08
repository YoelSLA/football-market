package footballmarket.models.enums;

/** Fallos técnicos que no consumen una oportunidad de evaluación válida. */
public enum TeamResolutionFailure {
  TIMEOUT_OR_TRANSPORT,
  RATE_LIMITED,
  SERVER_ERROR,
  INVALID_RESPONSE
}
