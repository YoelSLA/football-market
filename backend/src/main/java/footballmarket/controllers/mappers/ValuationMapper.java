package footballmarket.controllers.mappers;

import footballmarket.controllers.dtos.responses.ValuationEvaluationResponseDTO;
import footballmarket.models.records.ValuationEvaluationSummary;
import java.util.LinkedHashMap;
import java.util.Map;

/** Transformación estructural del resumen de valoración al contrato HTTP. */
public final class ValuationMapper {
  private ValuationMapper() {}

  public static ValuationEvaluationResponseDTO toResponse(ValuationEvaluationSummary summary) {
    Map<String, Long> exclusions = new LinkedHashMap<>();
    for (footballmarket.models.enums.ValuationExclusionReason reason :
        footballmarket.models.enums.ValuationExclusionReason.values()) {
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
