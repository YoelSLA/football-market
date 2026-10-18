package footballmarket.services.exceptions;

import footballmarket.exceptions.ApplicationException;

/** Publicación inexistente o no visible, sin distinción pública entre ambos casos. */
public class ListingNotFoundException extends ApplicationException {
  public ListingNotFoundException() {
    super("No encontramos la publicación solicitada.");
  }
}
