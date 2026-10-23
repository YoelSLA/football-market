package footballmarket.services;

import footballmarket.models.enums.MetricSource;
import footballmarket.models.records.PlayerStatisticsCoverage;
import java.time.LocalDate;

/** Cobertura consultable de una campaña, sin acceso al proveedor. */
public interface PlayerStatisticsCoverageService {
  PlayerStatisticsCoverage coverage(LocalDate extractionDate, MetricSource source);
}
