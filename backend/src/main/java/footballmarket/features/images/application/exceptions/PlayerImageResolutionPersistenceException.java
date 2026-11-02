package footballmarket.features.images.application.exceptions;

import footballmarket.common.exceptions.PersistenceException;

/** Error crítico al persistir el estado o la auditoría de imágenes. */
public class PlayerImageResolutionPersistenceException extends PersistenceException {
  public PlayerImageResolutionPersistenceException() {
    super("No se pudo persistir la resolución de imágenes");
  }
}
