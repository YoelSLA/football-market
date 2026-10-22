package footballmarket.repositories;

import footballmarket.models.PlayerStatistics;
import footballmarket.models.enums.MetricSource;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Fila ancha de métricas por jugador, campaña y canal. */
public interface PlayerStatisticsRepository extends JpaRepository<PlayerStatistics, Long> {
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
          + " and i.result = footballmarket.models.enums.PlayerExtractionResult.COMPLETED)")
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
