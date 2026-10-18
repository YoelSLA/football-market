package footballmarket.services.impl;

import footballmarket.models.enums.MetricSource;
import footballmarket.models.records.PlayerStatisticsCoverage;
import footballmarket.services.PlayerStatisticsCoverageService;
import footballmarket.services.internal.PlayerStatisticsCoverageQuery;
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
