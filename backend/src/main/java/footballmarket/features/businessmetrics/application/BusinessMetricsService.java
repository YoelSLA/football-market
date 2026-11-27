package footballmarket.features.businessmetrics.application;

import footballmarket.features.businessmetrics.domain.records.BusinessMetricsTotals;

/** Caso de uso para consultar agregados de negocio. */
public interface BusinessMetricsService {
  /** Calcula los totales históricos hasta el instante de consulta. */
  BusinessMetricsTotals getHistoricalMetrics();

  /** Lee los acumulados Micrometer desde el arranque actual. */
  BusinessMetricsTotals getCurrentMetrics();
}
