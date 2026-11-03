package footballmarket.features.valuation.presentation;

import footballmarket.features.valuation.domain.records.ValuationEvaluationSummary;
import footballmarket.features.valuation.presentation.dtos.response.ValuationEvaluationResponseDTO;
import java.util.LinkedHashMap;
import java.util.Map;

/** Transformación estructural del resumen de valoración al contrato HTTP. */
public final class ValuationMapper {
  private ValuationMapper() {}

  public static ValuationEvaluationResponseDTO toResponse(ValuationEvaluationSummary summary) {
    Map<String, Long> exclusions = new LinkedHashMap<>();
    for (footballmarket.features.valuation.domain.enums.ValuationExclusionReason reason :
        footballmarket.features.valuation.domain.enums.ValuationExclusionReason.values()) {
      exclusions.put(reason.name(), summary.excludedPlayers().getOrDefault(reason, 0L));
    }
    return new ValuationEvaluationResponseDTO(
        summary.valuationDate(),
        summary.evaluatedPlayers(),
        summary.failedPlayers(),
        Map.copyOf(exclusions),
        summary.imputedMetricCounts());
  }
}
