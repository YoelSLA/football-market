package footballmarket.features.statistics.application.impl;

import footballmarket.features.statistics.application.PlayerStatisticsCoverageService;
import footballmarket.features.statistics.application.internal.PlayerStatisticsCoverageQuery;
import footballmarket.features.statistics.domain.enums.MetricSource;
import footballmarket.features.statistics.domain.records.PlayerStatisticsCoverage;
import java.time.LocalDate;
import org.springframework.stereotype.Service;

/** Consulta de cobertura derivada de la persistencia, sin red ni navegador. */
@Service
public class PlayerStatisticsCoverageServiceImpl implements PlayerStatisticsCoverageService {
  private final PlayerStatisticsCoverageQuery coverage;

  public PlayerStatisticsCoverageServiceImpl(PlayerStatisticsCoverageQuery coverage) {
    this.coverage = coverage;
  }

  @Override
  public PlayerStatisticsCoverage coverage(LocalDate extractionDate, MetricSource source) {
    return this.coverage.coverage(extractionDate, source);
  }
}
