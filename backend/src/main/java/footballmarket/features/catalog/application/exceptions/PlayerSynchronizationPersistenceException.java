package footballmarket.features.catalog.application.exceptions;

import footballmarket.common.exceptions.PersistenceException;

/** Fallo técnico al aplicar una foto; no representa un registro descartable. */
public class PlayerSynchronizationPersistenceException extends PersistenceException {
  public PlayerSynchronizationPersistenceException() {
    super("No se pudo aplicar la sincronización del catálogo");
  }
}
