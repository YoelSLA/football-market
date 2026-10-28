package footballmarket.features.catalog.domain.exceptions;

import footballmarket.common.exceptions.DomainException;

/** Datos incompatibles con las invariantes de equipos, ligas o sus referencias. */
public class InvalidCatalogEntityException extends DomainException {
  public InvalidCatalogEntityException(String message) {
    super(message);
  }
}
