package footballmarket.features.quote.infraestructure;

import footballmarket.features.quote.domain.enums.QuoteOrigin;
import footballmarket.features.quote.domain.models.PlayerQuote;
import footballmarket.features.quote.domain.records.QuoteRankingPage.LeagueOption;
import footballmarket.features.quote.domain.records.QuoteRankingRow;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Historial de cotizaciones: la misma tabla es el historial y el estado vigente del jugador.
 *
 * <p>La unicidad diaria elimina la prevalencia entre orígenes. El orden vigente es {@code
 * effective_date DESC, calculated_at DESC, id DESC}.
 */
public interface PlayerQuoteRepository extends JpaRepository<PlayerQuote, Long> {

  String RANKED_QUOTES =
      """
      with latest as (
        select distinct on (q.player_id) q.* from player_quotes q
        order by q.player_id, q.effective_date desc, q.calculated_at desc, q.id desc
      ), ranked as (
        select row_number() over (order by q.price desc, q.player_id asc) as position,
          q.player_id as "playerId", q.price, q.effective_date as "effectiveDate",
          q.origin, q.calculated_at as "calculatedAt", p.name,
          p.position as "playerPosition", p.image_url as "imageUrl",
          t.id as "teamId", t.name as "teamName", l.id as "leagueId", l.name as "leagueName"
        from latest q join players p on p.id = q.player_id
        left join teams t on t.id = p.team_id left join leagues l on l.id = t.league_id
        where exists (select 1 from player_statistics s where s.player_id = p.id
          and s.present_metrics > 0 and s.extraction_date =
            (select max(s2.extraction_date) from player_statistics s2 where s2.player_id = p.id))
      )
      """;

  String RANKING_FILTERS =
      """
      where (cast(:name as text) is null or lower(name) like
        '%' || replace(replace(replace(lower(cast(:name as text)), '!', '!!'), '%', '!%'), '_', '!_') || '%' escape '!')
      and (cast(:leagueId as bigint) is null or "leagueId" = :leagueId)
      and (cast(:position as text) is null or lower("playerPosition") = lower(cast(:position as text)))
      """;

  /** Filtra y pagina en SQL conservando las posiciones del universo sin filtros. */
  @Query(
      value =
          RANKED_QUOTES
              + "select * from ranked "
              + RANKING_FILTERS
              + " order by position limit :size offset :offset",
      nativeQuery = true)
  List<QuoteRankingRow> findRankingPage(
      String name, Long leagueId, String position, int size, long offset);

  /** Conteo del mismo conjunto filtrado de la página, incluso si el offset está fuera de rango. */
  @Query(
      value = RANKED_QUOTES + "select count(*) from ranked " + RANKING_FILTERS,
      nativeQuery = true)
  long countRanking(String name, Long leagueId, String position);

  /** Opciones globales, nunca limitadas a los filtros o página solicitados. */
  @Query(
      value =
          RANKED_QUOTES
              + "select distinct \"leagueId\" as id, \"leagueName\" as name from ranked where \"leagueId\" is not null order by name, id",
      nativeQuery = true)
  List<LeagueOption> findRankingLeagues();

  /** Posiciones presentes en el universo elegible. */
  @Query(
      value =
          RANKED_QUOTES
              + "select distinct \"playerPosition\" from ranked where \"playerPosition\" is not null order by \"playerPosition\"",
      nativeQuery = true)
  List<String> findRankingPositions();

  /** Comprobación diaria independiente del origen, bajo el bloqueo del jugador. */
  boolean existsByPlayerIdAndEffectiveDate(Long playerId, LocalDate effectiveDate);

  /**
   * Última fila persistida del jugador: su cotización vigente.
   *
   * @param playerId identificador interno del jugador
   * @return fila vigente del jugador, o vacío si nunca fue cotizado
   */
  @Query(
      "select q from PlayerQuote q where q.player.id = :playerId"
          + " order by q.effectiveDate desc,"
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
          + " q.calculatedAt desc, q.id desc")
  List<PlayerQuote> findLatestByPlayerIdOnOrBefore(
      @Param("playerId") Long playerId, @Param("date") LocalDate date);

  /**
   * Ranking de jugadores por su cotización vigente, de mayor a menor precio.
   *
   * <p>Lectura de conjunto usada también por QuoteSource: una fila diaria vigente por jugador,
   * ordenada por precio descendente e ID de jugador ascendente. No recalcula precios.
   *
   * @param playerIds jugadores cotizables cuya cotización vigente se ordena
   * @return filas vigentes de los jugadores indicados, ordenadas por precio descendente
   */
  @Query(
      "select q from PlayerQuote q where q.player.id in :playerIds and not exists ("
          + "select q2 from PlayerQuote q2 where q2.player.id = q.player.id and ("
          + "q2.effectiveDate > q.effectiveDate or ("
          + "q2.effectiveDate = q.effectiveDate and (q2.calculatedAt > q.calculatedAt"
          + " or (q2.calculatedAt = q.calculatedAt and q2.id > q.id)))))"
          + " order by q.price desc, q.player.id asc")
  List<PlayerQuote> findRanking(@Param("playerIds") List<Long> playerIds);

  /**
   * Fechas de mercado de las filas iniciales existentes, de la más reciente a la más antigua.
   *
   * @return fechas de mercado vigentes
   */
  @Query(
      "select distinct q.effectiveDate from PlayerQuote q"
          + " where q.origin = footballmarket.features.quote.domain.enums.QuoteOrigin.INITIAL"
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
