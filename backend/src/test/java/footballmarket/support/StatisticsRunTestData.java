package footballmarket.support;

import footballmarket.models.PlayerStatisticsExtractionRun;
import footballmarket.models.enums.MetricSource;
import footballmarket.repositories.PlayerStatisticsExtractionRunRepository;
import java.time.Instant;
import java.time.LocalDate;
import org.springframework.transaction.annotation.Transactional;

/** Precondiciones persistentes exclusivas de Arrange para tests de Service. Sin assertions. */
public class StatisticsRunTestData {
  private final PlayerStatisticsExtractionRunRepository runs;

  public StatisticsRunTestData(PlayerStatisticsExtractionRunRepository repository) {
    runs = repository;
  }

  @Transactional
  public Long orphanScrapingRun(LocalDate date) {
    PlayerStatisticsExtractionRun orphan =
        new PlayerStatisticsExtractionRun(date, MetricSource.SCRAPING, Instant.now());
    return runs.saveAndFlush(orphan).getId();
  }
}
