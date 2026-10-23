package footballmarket.models.records;

import footballmarket.models.enums.PlayerImageSkipReason;
import footballmarket.models.enums.PlayerImageSyncRunItemResult;
import footballmarket.models.exceptions.InvalidPlayerException;

/** Contadores de una ejecución; los conflictos son un subconjunto de los fallos individuales. */
public record PlayerImageSyncCounters(
    int evaluated,
    int processed,
    int found,
    int notFound,
    int retryableErrors,
    int failed,
    int conflicts,
    int skippedFound,
    int skippedRetryWindow,
    int skippedFailed,
    int interrupted) {

  public PlayerImageSyncCounters {
    if (evaluated < 0
        || processed < 0
        || found < 0
        || notFound < 0
        || retryableErrors < 0
        || failed < 0
        || conflicts < 0
        || skippedFound < 0
        || skippedRetryWindow < 0
        || skippedFailed < 0
        || interrupted < 0
        || conflicts > failed
        || processed > evaluated) {
      throw new InvalidPlayerException("Los contadores de imágenes son inconsistentes");
    }
  }

  public static PlayerImageSyncCounters empty() {
    return new PlayerImageSyncCounters(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
  }

  /** Incorpora la evaluación de un jugador activo y su resultado confirmado. */
  public PlayerImageSyncCounters add(
      PlayerImageSyncRunItemResult result,
      PlayerImageSkipReason reason,
      boolean attempted,
      boolean conflict) {
    if (result == null
        || (conflict && result != PlayerImageSyncRunItemResult.FAILED)
        || (result == PlayerImageSyncRunItemResult.SKIPPED) != (reason != null)
        || (attempted && result == PlayerImageSyncRunItemResult.SKIPPED)) {
      throw new InvalidPlayerException("Resultado individual inconsistente");
    }
    return new PlayerImageSyncCounters(
        this.evaluated + 1,
        this.processed + (attempted ? 1 : 0),
        this.found + (result == PlayerImageSyncRunItemResult.FOUND ? 1 : 0),
        this.notFound + (result == PlayerImageSyncRunItemResult.NOT_FOUND ? 1 : 0),
        this.retryableErrors + (result == PlayerImageSyncRunItemResult.RETRYABLE_ERROR ? 1 : 0),
        this.failed + (result == PlayerImageSyncRunItemResult.FAILED ? 1 : 0),
        this.conflicts + (conflict ? 1 : 0),
        this.skippedFound + (reason == PlayerImageSkipReason.FOUND ? 1 : 0),
        this.skippedRetryWindow + (reason == PlayerImageSkipReason.RETRY_WINDOW ? 1 : 0),
        this.skippedFailed + (reason == PlayerImageSkipReason.FAILED ? 1 : 0),
        this.interrupted + (result == PlayerImageSyncRunItemResult.INTERRUPTED ? 1 : 0));
  }

  /** Suma el resultado de un item cuyo jugador ya fue contado como evaluado. */
  public PlayerImageSyncCounters addOutcome(
      PlayerImageSyncRunItemResult result,
      PlayerImageSkipReason reason,
      boolean attempted,
      boolean conflict) {
    PlayerImageSyncCounters incremented = this.add(result, reason, attempted, conflict);
    return new PlayerImageSyncCounters(
        this.evaluated,
        incremented.processed,
        incremented.found,
        incremented.notFound,
        incremented.retryableErrors,
        incremented.failed,
        incremented.conflicts,
        incremented.skippedFound,
        incremented.skippedRetryWindow,
        incremented.skippedFailed,
        incremented.interrupted);
  }

  /** Marca una evaluación ya contabilizada como interrumpida, sin sumar fallos. */
  public PlayerImageSyncCounters interrupt() {
    return new PlayerImageSyncCounters(
        this.evaluated,
        this.processed,
        this.found,
        this.notFound,
        this.retryableErrors,
        this.failed,
        this.conflicts,
        this.skippedFound,
        this.skippedRetryWindow,
        this.skippedFailed,
        this.interrupted + 1);
  }
}
