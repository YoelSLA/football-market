package footballmarket.integrations.exceptions;

import footballmarket.exceptions.IntegrationException;

public class HeadedBrowserUnavailableException extends IntegrationException {
  public HeadedBrowserUnavailableException() {
    this(null);
  }

  public HeadedBrowserUnavailableException(String detail) {
    super(
        "No hay un navegador visible disponible para extraer estadísticas"
            + (detail == null || detail.isBlank() ? "" : ": " + detail));
  }
}
