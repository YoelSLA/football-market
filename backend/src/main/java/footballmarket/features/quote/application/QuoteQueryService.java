package footballmarket.features.quote.application;

import footballmarket.features.quote.domain.models.PlayerQuote;
import footballmarket.features.quote.domain.records.PlayerRankingEntry;
import java.time.LocalDate;
import java.util.List;

/**
 * Caso de uso de consulta del historial y de la cotización vigente de un jugador.
 *
 * <p>Las consultas se resuelven por acceso directo al historial persistido: no recalculan precios
 * ni aplican la fórmula de re-ajuste.
 */
public interface QuoteQueryService {

  /**
   * Historial completo del jugador, de la fila más reciente a la más antigua.
   *
   * @param playerId identificador interno del jugador
   * @return filas del historial; vacío cuando el jugador nunca fue cotizado
   */
  List<PlayerQuote> findHistory(Long playerId);

  /**
   * Cotización vigente del jugador: su última cotización persistida.
   *
   * @param playerId identificador interno del jugador
   * @return cotización vigente del jugador
   * @throws footballmarket.features.quote.application.exceptions.QuoteNotFoundException si el
   *     jugador no tiene cotización
   */
  PlayerQuote findCurrent(Long playerId);

  /**
   * Cotización del jugador a una fecha dada: la última cuya fecha de vigencia no supera la
   * consultada.
   *
   * @param playerId identificador interno del jugador
   * @param date fecha consultada
   * @return cotización vigente a esa fecha
   * @throws footballmarket.features.quote.application.exceptions.QuoteNotFoundException si el
   *     jugador no tenía cotización anterior o igual a la fecha consultada
   */
  PlayerQuote findOnDate(Long playerId, LocalDate date);

  /**
   * Ranking de jugadores por su cotización vigente, de mayor a menor precio.
   *
   * <p>Cada jugador cotizado aparece una sola vez, con la fila vigente según el orden de lectura
   * del historial. La consulta no recalcula precios.
   *
   * @return entradas del ranking, ordenadas por precio descendente; vacío si el mercado todavía no
   *     tiene cotizaciones
   */
  List<PlayerRankingEntry> findRanking();
}
