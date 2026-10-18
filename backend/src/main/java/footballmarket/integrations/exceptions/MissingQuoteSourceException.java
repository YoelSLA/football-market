package footballmarket.integrations.exceptions;

import footballmarket.exceptions.IntegrationException;

/**
 * No hay ninguna fuente de cotización disponible para arrancar.
 *
 * <p>Sin fuente real y sin simulador habilitado no existe precio válido, de modo que la compra no
 * podría confirmarse. Se prefiere este fallo de configuración explícito a un precio inventado.
 */
public class MissingQuoteSourceException extends IntegrationException {
  public MissingQuoteSourceException() {
    super(
        "No hay ninguna fuente de cotización disponible. Habilite el simulador con"
            + " market.quote.simulator.enabled=true para desarrollo y pruebas, o registre la"
            + " implementación real de REQ-S2-03.");
  }
}
