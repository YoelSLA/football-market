package footballmarket.models.records;

import footballmarket.models.enums.ValuationExclusionReason;
import java.time.LocalDate;
import java.util.Map;

/** Resumen agregado de una evaluación poblacional. */
public record ValuationEvaluationSummary(
    LocalDate valuationDate,
    long evaluatedPlayers,
    long failedPlayers,
    Map<ValuationExclusionReason, Long> excludedPlayers,
    Map<String, Long> imputedMetricCounts) {}
