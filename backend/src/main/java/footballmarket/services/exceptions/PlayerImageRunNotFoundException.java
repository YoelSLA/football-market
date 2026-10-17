package footballmarket.services.exceptions;

import footballmarket.exceptions.ApplicationException;

/** El identificador solicitado no corresponde a una ejecución existente. */
public class PlayerImageRunNotFoundException extends ApplicationException {
  public PlayerImageRunNotFoundException() {
    super("No existe la ejecución de imágenes solicitada");
  }
}
