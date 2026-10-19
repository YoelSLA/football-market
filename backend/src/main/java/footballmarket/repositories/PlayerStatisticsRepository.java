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
}
