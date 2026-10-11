package footballmarket.services.exceptions;

import footballmarket.exceptions.ApplicationException;

/**
 * La foto recibida no permite resolver una identidad requerida y no se aplica.
 *
 * <p>No es un fallo técnico ni un registro descartable: la abstención revierte por completo el
 * intento, de modo que no se desactiva ningún jugador ni queda ningún cambio parcial.
 */
public class IncompletePhotoException extends ApplicationException {
  public IncompletePhotoException() {
    super("La foto no permite resolver la identidad requerida y no se aplica");
  }
}
