package footballmarket.repositories;

import footballmarket.models.PlayerStatisticsRunItem;
import footballmarket.models.enums.MetricSource;
import footballmarket.models.enums.PlayerExtractionResult;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

/** Resultados de extracción y selección de pendientes sin cursor mutable. */
public interface PlayerStatisticsRunItemRepository
    extends JpaRepository<PlayerStatisticsRunItem, Long> {
  @Query(
      """
      select p.id from Player p
      where exists (select r.id from PlayerExternalReference r
        where r.player = p and r.provider = footballmarket.models.enums.ExternalProvider.WHOSCORED)
      and not exists (select i.id from PlayerStatisticsRunItem i
        where i.player = p and i.extractionDate = :date and i.source = :source
          and i.result = footballmarket.models.enums.PlayerExtractionResult.COMPLETED)
      order by p.id
      """)
  List<Long> findPendingPlayerIds(LocalDate date, MetricSource source, Pageable pageable);

  List<PlayerStatisticsRunItem> findByRunIdOrderByPlayerIdAsc(Long runId);

  boolean existsByPlayerIdAndExtractionDateAndSourceAndResult(
      Long playerId, LocalDate extractionDate, MetricSource source, PlayerExtractionResult result);

  List<PlayerStatisticsRunItem> findByExtractionDateAndSourceAndResultOrderByFinishedAtDescIdDesc(
      LocalDate extractionDate,
      MetricSource source,
      PlayerExtractionResult result,
      Pageable pageable);

  /** Jugadores distintos confirmados para la fecha y el canal de la campaña. */
  @Query(
      "select count(distinct i.player.id) from PlayerStatisticsRunItem i"
          + " where i.extractionDate = :date and i.source = :source and i.result = :result")
  long countDistinctPlayersByResult(
      LocalDate date, MetricSource source, PlayerExtractionResult result);

  /** Jugadores distintos pendientes: sin confirmación satisfactoria de esa campaña. */
  @Query(
      "select count(distinct i.player.id) from PlayerStatisticsRunItem i"
          + " where i.extractionDate = :date and i.source = :source and i.result <>"
          + " footballmarket.models.enums.PlayerExtractionResult.COMPLETED")
  long countDistinctPlayersWithoutConfirmation(LocalDate date, MetricSource source);

  /** Identificadores de la población elegible del catálogo, en orden estable. */
  @Query(
      "select p.id from Player p where exists (select r.id from PlayerExternalReference r"
          + " where r.player = p and r.provider = footballmarket.models.enums.ExternalProvider.WHOSCORED)"
          + " order by p.id")
  List<Long> findEligiblePlayerIds();

  /** Jugadores distintos cuyos items caen en cualquiera de los resultados indicados. */
  @Query(
      "select count(distinct i.player.id) from PlayerStatisticsRunItem i"
          + " where i.extractionDate = :date and i.source = :source and i.result in :results")
  long countDistinctPlayersByResults(
      LocalDate date, MetricSource source, Collection<PlayerExtractionResult> results);
}
