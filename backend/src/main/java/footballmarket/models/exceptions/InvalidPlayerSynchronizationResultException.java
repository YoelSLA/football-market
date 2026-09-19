package footballmarket.models.exceptions;

import footballmarket.exceptions.DomainException;

/** Indica que un resultado de sincronización contiene contadores inconsistentes. */
public class InvalidPlayerSynchronizationResultException extends DomainException {

  /** Crea el error de consistencia de un resultado de sincronización. */
  public InvalidPlayerSynchronizationResultException() {
    super("Los contadores de sincronización son inválidos");
  }
}
