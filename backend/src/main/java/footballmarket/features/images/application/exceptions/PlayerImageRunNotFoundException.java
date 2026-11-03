package footballmarket.features.images.application.exceptions;

import footballmarket.common.exceptions.ApplicationException;

/** El identificador solicitado no corresponde a una ejecución existente. */
public class PlayerImageRunNotFoundException extends ApplicationException {
  public PlayerImageRunNotFoundException() {
    super("No existe la ejecución de imágenes solicitada");
  }
}
