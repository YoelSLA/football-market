package footballmarket.integrations;

import footballmarket.models.records.CurrentQuote;
import java.util.Optional;

/**
 * Puerto de lectura de la cotización vigente de un jugador.
 *
 * <p>Es una Integration y no un Service: no expone un caso de uso propio y solo lo consume la
 * fachada de compra. Desacopla la compra del repositorio canónico de cotizaciones que entrega
 * REQ-S2-03, cuyo esquema no forma parte de esta SPEC.
 */
public interface QuoteSource {

  /**
   * Obtiene la cotización vigente de un jugador.
   *
   * @param playerId identificador interno del jugador
   * @return instantánea de la cotización vigente, o vacío si el jugador no tiene cotización
   */
  Optional<CurrentQuote> currentQuote(Long playerId);
}
