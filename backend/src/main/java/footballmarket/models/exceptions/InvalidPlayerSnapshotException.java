package footballmarket.models.exceptions;

import footballmarket.exceptions.DomainException;

/** Indica que una foto de jugadores contiene contadores o datos inconsistentes. */
public class InvalidPlayerSnapshotException extends DomainException {

  /** Crea el error de consistencia de una foto de jugadores. */
  public InvalidPlayerSnapshotException() {
    super("Los contadores de la foto son inválidos");
  }
}
