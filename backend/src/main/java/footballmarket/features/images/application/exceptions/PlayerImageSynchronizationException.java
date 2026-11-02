package footballmarket.features.images.application.exceptions;

import footballmarket.common.exceptions.ApplicationException;

/** Fallo global que impide finalizar la sincronización de imágenes. */
public class PlayerImageSynchronizationException extends ApplicationException {
  public PlayerImageSynchronizationException() {
    super("No se pudo completar la sincronización de imágenes");
  }
}
