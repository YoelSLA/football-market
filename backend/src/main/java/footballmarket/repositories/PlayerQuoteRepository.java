package footballmarket.repositories;

import footballmarket.models.PlayerQuote;
import footballmarket.models.enums.QuoteOrigin;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Historial de cotizaciones: la misma tabla es el historial y el estado vigente del jugador.
 *
 * <p>El orden de lectura vigente es {@code effective_date DESC, calculated_at DESC} y, a igualdad
 * de fecha de vigencia, prevalece la fila {@code RECOMPUTED} porque fue escrita después que la
 * arrastrada. El identificador de la fila cierra el orden como último criterio, de modo que la
 * lectura sea total y una fila nunca dependa del orden arbitrario de PostgreSQL.
 */
public interface PlayerQuoteRepository extends JpaRepository<PlayerQuote, Long> {

  /**
   * Última fila persistida del jugador: su cotización vigente.
   *
   * @param playerId identificador interno del jugador
   * @return fila vigente del jugador, o vacío si nunca fue cotizado
   */
  @Query(
      "select q from PlayerQuote q where q.player.id = :playerId"
          + " order by q.effectiveDate desc,"
          + " case when q.origin = footballmarket.models.enums.QuoteOrigin.RECOMPUTED then 0 else 1 end,"
          + " q.calculatedAt desc, q.id desc")
  List<PlayerQuote> findLatestByPlayerId(@Param("playerId") Long playerId);

  /**
   * Historial completo del jugador, de la fila más reciente a la más antigua.
   *
   * @param playerId identificador interno del jugador
   * @return filas del historial ordenadas por fecha de vigencia descendente
   */
  @Query(
      "select q from PlayerQuote q where q.player.id = :playerId"
          + " order by q.effectiveDate desc,"
          + " case when q.origin = footballmarket.models.enums.QuoteOrigin.RECOMPUTED then 0 else 1 end,"
          + " q.calculatedAt desc, q.id desc")
  List<PlayerQuote> findHistoryByPlayerId(@Param("playerId") Long playerId);

  /**
   * Última fila cuya fecha de vigencia no supera la fecha consultada.
   *
   * @param playerId identificador interno del jugador
   * @param date fecha consultada
   * @return fila vigente a esa fecha, o vacío si el jugador no tenía cotización
   */
  @Query(
      "select q from PlayerQuote q where q.player.id = :playerId and q.effectiveDate <= :date"
          + " order by q.effectiveDate desc,"
          + " case when q.origin = footballmarket.models.enums.QuoteOrigin.RECOMPUTED then 0 else 1 end,"
          + " q.calculatedAt desc, q.id desc")
  List<PlayerQuote> findLatestByPlayerIdOnOrBefore(
      @Param("playerId") Long playerId, @Param("date") LocalDate date);

  /**
   * Ranking de jugadores por su cotización vigente, de mayor a menor precio.
   *
   * <p>Cada jugador aparece una sola vez, con la fila vigente según el mismo orden de lectura del
   * historial: fecha de vigencia descendente, prevalencia de la fila {@code RECOMPUTED} a igualdad
   * de fecha, instante de cálculo descendente y, en último término, identificador de la fila
   * descendente. El desempate por identificador cierra el orden y garantiza una sola entrada por
   * jugador incluso si dos filas coincidieran en fecha de vigencia e instante de cálculo. El
   * ranking no recalcula precios.
   *
   * @param playerIds jugadores cotizables cuya cotización vigente se ordena
   * @return filas vigentes de los jugadores indicados, ordenadas por precio descendente
   */
  @Query(
      "select q from PlayerQuote q where q.player.id in :playerIds and not exists ("
          + "select q2 from PlayerQuote q2 where q2.player.id = q.player.id and ("
          + "q2.effectiveDate > q.effectiveDate or ("
          + "q2.effectiveDate = q.effectiveDate and ("
          + "(q2.origin = footballmarket.models.enums.QuoteOrigin.RECOMPUTED"
          + " and q.origin in (footballmarket.models.enums.QuoteOrigin.INITIAL,"
          + " footballmarket.models.enums.QuoteOrigin.CARRIED_FORWARD)) or ("
          + "((q2.origin = footballmarket.models.enums.QuoteOrigin.RECOMPUTED"
          + " and q.origin = footballmarket.models.enums.QuoteOrigin.RECOMPUTED)"
          + " or (q2.origin in (footballmarket.models.enums.QuoteOrigin.INITIAL,"
          + " footballmarket.models.enums.QuoteOrigin.CARRIED_FORWARD)"
          + " and q.origin in (footballmarket.models.enums.QuoteOrigin.INITIAL,"
          + " footballmarket.models.enums.QuoteOrigin.CARRIED_FORWARD)))"
          + " and (q2.calculatedAt > q.calculatedAt"
          + " or (q2.calculatedAt = q.calculatedAt and q2.id > q.id)))))))"
          + " order by q.price desc, q.effectiveDate desc, q.player.id asc")
  List<PlayerQuote> findRanking(@Param("playerIds") List<Long> playerIds);

  /**
   * Fecha de vigencia de referencia del jugador: la de su última fila inicial o recalculada.
   *
   * <p>Las filas arrastradas no participan, porque no producen el precio que se repite y no deben
   * impedir un reprecio posterior.
   *
   * @param playerId identificador interno del jugador
   * @return fecha de vigencia de referencia, o vacío si el jugador no tiene fila inicial
   */
  @Query(
      "select q.effectiveDate from PlayerQuote q where q.player.id = :playerId"
          + " and q.origin in (footballmarket.models.enums.QuoteOrigin.INITIAL,"
          + " footballmarket.models.enums.QuoteOrigin.RECOMPUTED)"
          + " order by q.effectiveDate desc, q.calculatedAt desc, q.id desc")
  List<LocalDate> findReferenceDatesByPlayerId(@Param("playerId") Long playerId);

  /**
   * Fechas de mercado de las filas iniciales existentes, de la más reciente a la más antigua.
   *
   * @return fechas de mercado vigentes
   */
  @Query(
      "select distinct q.effectiveDate from PlayerQuote q"
          + " where q.origin = footballmarket.models.enums.QuoteOrigin.INITIAL"
          + " order by q.effectiveDate desc")
  List<LocalDate> findMarketDates();

  /**
   * Indica si existe alguna fila del origen indicado.
   *
   * @param origin origen de la fila buscada
   * @return {@code true} si existe al menos una fila de ese origen
   */
  boolean existsByOrigin(QuoteOrigin origin);

  /**
   * Cantidad de filas del origen indicado.
   *
   * @param origin origen de las filas contadas
   * @return cantidad de filas de ese origen
   */
  long countByOrigin(QuoteOrigin origin);
}
