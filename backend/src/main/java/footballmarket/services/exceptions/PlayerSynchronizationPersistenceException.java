package footballmarket.services.exceptions;

import footballmarket.exceptions.PersistenceException;

/** Fallo técnico al aplicar una foto; no representa un registro descartable. */
public class PlayerSynchronizationPersistenceException extends PersistenceException {
  public PlayerSynchronizationPersistenceException() {
    super("No se pudo aplicar la sincronización del catálogo");
  }
}
