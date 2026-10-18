package footballmarket.models.exceptions;

import footballmarket.exceptions.DomainException;

/** Rechazo de una construcción o transición que viola las invariantes de publicación. */
public class InvalidTokenListingException extends DomainException {
  public InvalidTokenListingException() {
    super("La publicación no admite la operación solicitada.");
  }
}
