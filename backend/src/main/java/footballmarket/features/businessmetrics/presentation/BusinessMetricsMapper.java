package footballmarket.features.businessmetrics.presentation;

import footballmarket.features.businessmetrics.domain.records.BusinessMetricsTotals;
import footballmarket.features.businessmetrics.presentation.dtos.response.BusinessMetricsResponseDTO;

/** Convierte los agregados de negocio al contrato HTTP. */
public final class BusinessMetricsMapper {
  private BusinessMetricsMapper() {}

  /** Mapea los totales históricos a la respuesta pública. */
  public static BusinessMetricsResponseDTO toResponse(BusinessMetricsTotals totals) {
    return new BusinessMetricsResponseDTO(
        totals.purchases(),
        totals.sales(),
        totals.cancelledListings(),
        totals.tokensTransferred(),
        totals.creditsTradedInCents(),
        totals.queriedAt());
  }
}
