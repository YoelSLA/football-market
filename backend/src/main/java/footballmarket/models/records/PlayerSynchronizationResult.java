package footballmarket.models.records;

import footballmarket.models.exceptions.InvalidPlayerSynchronizationResultException;

public record PlayerSynchronizationResult(
    int obtained, int created, int updated, int markedInactive, int discardedInvalid) {
  public PlayerSynchronizationResult {
    if (obtained < 0
        || created < 0
        || updated < 0
        || markedInactive < 0
        || discardedInvalid < 0
        || (long) created + updated + discardedInvalid > obtained) {
      throw new InvalidPlayerSynchronizationResultException();
    }
  }
}
