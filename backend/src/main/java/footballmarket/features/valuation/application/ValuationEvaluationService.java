package footballmarket.features.valuation.application;

import footballmarket.features.valuation.domain.records.ValuationEvaluationSummary;

/** Fachada pública de la evaluación estadística poblacional. */
public interface ValuationEvaluationService {
  ValuationEvaluationSummary evaluate();
}
