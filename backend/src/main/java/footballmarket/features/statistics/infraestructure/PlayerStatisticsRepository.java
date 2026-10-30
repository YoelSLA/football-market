package footballmarket.features.statistics.infraestructure;

import footballmarket.features.statistics.domain.enums.MetricSource;
import footballmarket.features.statistics.domain.models.PlayerStatistics;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Fila ancha de métricas por jugador, campaña y canal. */
public interface PlayerStatisticsRepository extends JpaRepository<PlayerStatistics, Long> {
  /** Un solo lote: la fecha del ancla fija qué filas y contexto pueden contribuir. */
  @Query(
      value =
          """
      with confirmed as (
        select s.* from player_statistics s
        join player_statistics_run_items i on i.id = s.run_item_id
        where s.player_id in (:playerIds) and s.context_status = 'RESOLVED'
          and s.season_key is not null and s.provider_competition_id is not null
          and i.result = 'COMPLETED' and i.player_id = s.player_id
          and i.extraction_date = s.extraction_date and i.source = s.source
      ), ranked as (
        select c.*, row_number() over (partition by player_id order by extraction_date desc,
          case when source = 'JSON' then 0 else 1 end, observed_at desc, id desc) as rank
        from confirmed c
      )
      select c.* from confirmed c join ranked a on a.player_id = c.player_id and a.rank = 1
        and a.extraction_date = c.extraction_date and a.season_key = c.season_key
        and a.provider_competition_id = c.provider_competition_id
      order by c.player_id, case when c.source = 'JSON' then 0 else 1 end, c.observed_at desc, c.id desc
      """,
      nativeQuery = true)
  List<PlayerStatistics> findCatalogStatistics(@Param("playerIds") List<Long> playerIds);

  /** Selección individual confirmada, sin exigir resultado global de la campaña. */
  @Query(
      "select s from PlayerStatistics s join s.runItem i"
          + " where s.player.id = :playerId and (:seasonKey is null or s.seasonKey = :seasonKey)"
          + " and s.contextStatus = 'RESOLVED' and s.seasonKey is not null"
          + " and s.providerCompetitionId is not null"
          + " and i.result = footballmarket.features.statistics.domain.enums.PlayerExtractionResult.COMPLETED"
          + " and i.player = s.player and i.extractionDate = s.extractionDate and i.source = s.source"
          + " order by s.extractionDate desc,"
          + " case when s.source = footballmarket.features.statistics.domain.enums.MetricSource.JSON then 0 else 1 end,"
          + " s.observedAt desc, s.id desc")
  List<PlayerStatistics> findConfirmedForPlayer(
      @Param("playerId") Long playerId, @Param("seasonKey") String seasonKey, Pageable pageable);

  /** Filas compañeras del contexto exacto del ancla, como máximo una por canal. */
  @Query(
      "select s from PlayerStatistics s join s.runItem i"
          + " where s.player.id = :playerId and s.extractionDate = :date"
          + " and s.seasonKey = :seasonKey and s.providerCompetitionId = :competitionKey"
          + " and s.contextStatus = 'RESOLVED'"
          + " and i.result = footballmarket.features.statistics.domain.enums.PlayerExtractionResult.COMPLETED"
          + " and i.player = s.player and i.extractionDate = s.extractionDate and i.source = s.source"
          + " order by s.observedAt desc, s.id desc")
  List<PlayerStatistics> findConfirmedCompanions(
      @Param("playerId") Long playerId,
      @Param("date") LocalDate date,
      @Param("seasonKey") String seasonKey,
      @Param("competitionKey") String competitionKey);

  /**
   * Fila ya presente de un jugador en la campaña, si existe.
   *
   * <p>Identifica por el jugador sin cargar su entidad, porque la fila observada es la que una
   * reconfirmación debe reemplazar y no por su referencia a un item anterior.
   */
  @Query(
      "select s from PlayerStatistics s"
          + " where s.extractionDate = :date and s.source = :source and s.player.id = :playerId")
  Optional<PlayerStatistics> findCampaignRow(
      @Param("playerId") Long playerId,
      @Param("date") LocalDate date,
      @Param("source") MetricSource source);

  /** Solo métricas y contexto: evita cargar el jugador para el cálculo de cobertura. */
  @Query("select s from PlayerStatistics s where s.extractionDate = :date and s.source = :source")
  List<PlayerStatistics> findMetricsByCampaign(LocalDate date, MetricSource source);

  @Query(
      "select s from PlayerStatistics s join fetch s.player p join fetch p.team t join fetch t.league where s.extractionDate = :date and s.source = :source order by p.id")
  List<PlayerStatistics> findValuationCampaign(LocalDate date, MetricSource source);

  /** Filas de la fecha cuyos jugadores tienen item COMPLETED en esa campaña. */
  @Query(
      "select count(distinct s.player.id) from PlayerStatistics s"
          + " where s.extractionDate = :date and s.source = :source"
          + " and exists (select i.id from PlayerStatisticsRunItem i where i.player = s.player"
          + " and i.extractionDate = :date and i.source = :source"
          + " and i.result = footballmarket.features.statistics.domain.enums.PlayerExtractionResult.COMPLETED)")
  long countValuationRowsWithCompletedItem(LocalDate date, MetricSource source);

  /**
   * Identificadores de los jugadores cotizables: los que tienen al menos una métrica informada en
   * su último registro de métricas persistidas.
   *
   * <p>Un jugador sin métricas, o con un último registro sin ninguna métrica informada, no aparece:
   * su condición de cotizable depende de las métricas persistidas y no de la cobertura de las
   * valuaciones.
   *
   * @return identificadores de los jugadores cotizables
   */
  @Query(
      "select distinct s.player.id from PlayerStatistics s where s.presentMetrics > 0"
          + " and s.extractionDate = (select max(s2.extractionDate) from PlayerStatistics s2"
          + " where s2.player.id = s.player.id)")
  List<Long> findPricablePlayerIds();
}
