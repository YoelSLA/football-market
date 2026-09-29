package footballmarket.services.exceptions;

import footballmarket.exceptions.DomainException;

/** Indica que el sujeto de la sesión no identifica a un usuario persistido actual. */
public class CurrentUserNotFoundException extends DomainException {

  public CurrentUserNotFoundException() {
    super("La identidad de la sesión no es válida.");
  }
}
