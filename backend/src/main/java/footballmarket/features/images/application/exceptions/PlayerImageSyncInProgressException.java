package footballmarket.features.images.application.exceptions;

import footballmarket.common.exceptions.ApplicationException;

/** Otra sincronización de imágenes ya ocupa la única ejecución permitida por instancia. */
public class PlayerImageSyncInProgressException extends ApplicationException {
  public PlayerImageSyncInProgressException() {
    super("Ya hay una sincronización de imágenes en curso");
  }
}
