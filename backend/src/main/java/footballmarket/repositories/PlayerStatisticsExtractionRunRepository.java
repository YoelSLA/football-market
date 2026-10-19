package footballmarket.repositories;

import footballmarket.models.PlayerStatisticsExtractionRun;
import footballmarket.models.enums.ExtractionRunStatus;
import footballmarket.models.enums.MetricSource;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Auditoría de campañas y detección de ejecuciones activas por canal. */
public interface PlayerStatisticsExtractionRunRepository
    extends JpaRepository<PlayerStatisticsExtractionRun, Long> {
  Optional<PlayerStatisticsExtractionRun> findBySourceAndStatus(
      MetricSource source, ExtractionRunStatus status);

  List<PlayerStatisticsExtractionRun> findByExtractionDateAndSourceOrderByStartedAtDescIdDesc(
      LocalDate extractionDate, MetricSource source);
}
