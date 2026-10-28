package footballmarket.features.statistics.application;

import footballmarket.features.statistics.domain.enums.MetricSource;
import footballmarket.features.statistics.domain.records.PlayerStatisticsCoverage;
import java.time.LocalDate;

/** Cobertura consultable de una campaña, sin acceso al proveedor. */
public interface PlayerStatisticsCoverageService {
  PlayerStatisticsCoverage coverage(LocalDate extractionDate, MetricSource source);
}
