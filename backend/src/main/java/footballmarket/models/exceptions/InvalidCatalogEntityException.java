package footballmarket.models.exceptions;

import footballmarket.exceptions.DomainException;

/** Datos incompatibles con las invariantes de equipos, ligas o sus referencias. */
public class InvalidCatalogEntityException extends DomainException {
  public InvalidCatalogEntityException(String message) {
    super(message);
  }
}
