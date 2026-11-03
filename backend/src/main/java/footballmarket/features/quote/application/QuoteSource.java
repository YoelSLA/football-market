package footballmarket.features.quote.application;

import footballmarket.features.quote.application.exceptions.QuoteBatchUnavailableException;
import footballmarket.features.quote.domain.records.CurrentQuote;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Puerto de lectura de la cotización vigente de un jugador.
 *
 * <p>Es una Integration y no un Service: no expone un caso de uso propio y solo lo consume la capa
 * Service. Desacopla el mercado y la contabilidad del repositorio canónico que entrega REQ-S2-03,
 * cuyo esquema no forma parte de esta SPEC.
 */
public interface QuoteSource {

  /**
   * Obtiene la cotización vigente de un jugador.
   *
   * @param playerId identificador interno del jugador
   * @return instantánea de la cotización vigente, o vacío si el jugador no tiene cotización
   */
  Optional<CurrentQuote> currentQuote(Long playerId);

  /**
   * Obtiene un lote real, sin consultas individuales por jugador. Las claves omitidas representan
   * ausencia de cotización; los fallos técnicos abortan la adquisición completa.
   *
   * <p>El default mantiene la compatibilidad con fuentes individuales, pero no simula soporte
   * masivo mediante N llamadas. Cada fuente que atienda portfolios debe implementar este método.
   *
   * @param playerIds conjunto de jugadores con posición abierta
   * @return mapa de snapshots correspondientes a las claves solicitadas
   * @throws QuoteBatchUnavailableException si la fuente no soporta la adquisición masiva
   */
  default Map<Long, CurrentQuote> currentQuotes(Set<Long> playerIds) {
    throw new QuoteBatchUnavailableException();
  }
}
