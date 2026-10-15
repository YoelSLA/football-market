package footballmarket.integrations.exceptions;

import footballmarket.exceptions.IntegrationException;
import lombok.Getter;

/** Diagnóstico limitado a códigos y metadatos; no conserva cuerpos ni causas sensibles. */
@Getter
public class WhoScoredIntegrationException extends IntegrationException {
  private final String code;
  private final Integer httpStatus;
  private final boolean interrupted;
  private final Integer competitionId;
  private final Integer page;

  public WhoScoredIntegrationException(String code, Integer httpStatus, boolean interrupted) {
    this(code, httpStatus, interrupted, null, null);
  }

  public WhoScoredIntegrationException(
      String code, Integer httpStatus, boolean interrupted, Integer competitionId, Integer page) {
    super("No se pudo completar el descubrimiento de identidad del proveedor");
    this.code = code;
    this.httpStatus = httpStatus;
    this.interrupted = interrupted;
    this.competitionId = competitionId;
    this.page = page;
  }

  /** Añade competition y página conservando el código, el estado HTTP y la interrupción. */
  public WhoScoredIntegrationException withContext(int competition, int requestedPage) {
    return this.competitionId != null && this.page != null
        ? this
        : new WhoScoredIntegrationException(
            this.code,
            this.httpStatus,
            this.interrupted,
            this.competitionId != null ? this.competitionId : competition,
            this.page != null ? this.page : requestedPage);
  }
}
