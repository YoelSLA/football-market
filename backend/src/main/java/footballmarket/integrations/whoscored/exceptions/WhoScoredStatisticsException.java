package footballmarket.integrations.whoscored.exceptions;

import footballmarket.common.exceptions.IntegrationException;
import lombok.Getter;

@Getter
public class WhoScoredStatisticsException extends IntegrationException {
  private final String code;
  private final Integer providerStatus;

  /**
   * Sección del recorrido en la que se produjo el fallo.
   *
   * <p>El recorrido visita las secciones de una en una y abandona al jugador en la primera que
   * falla. Sin la sección, la incidencia solo dice qué código se registró y no dónde se rompió el
   * recorrido.
   */
  private final String section;

  public WhoScoredStatisticsException(String code, Integer providerStatus) {
    this(code, providerStatus, null);
  }

  public WhoScoredStatisticsException(String code, Integer providerStatus, String section) {
    super("No se pudo completar la extracción de estadísticas del proveedor");
    this.code = code;
    this.providerStatus = providerStatus;
    this.section = section;
  }
}
