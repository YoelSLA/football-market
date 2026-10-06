package footballmarket.integrations.exceptions;

import footballmarket.exceptions.IntegrationException;
import java.time.Instant;

/** Respuesta de cuota agotada y momento seguro para intentar otra solicitud. */
public class TheSportsDbRateLimitException extends IntegrationException {
  private final Instant retryAt;

  public TheSportsDbRateLimitException(Instant retryAt) {
    super("El proveedor de imágenes limitó las peticiones");
    this.retryAt = retryAt;
  }

  public Instant getRetryAt() {
    return this.retryAt;
  }
}
