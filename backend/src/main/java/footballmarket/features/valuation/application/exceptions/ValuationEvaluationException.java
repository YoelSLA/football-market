package footballmarket.features.valuation.application.exceptions;

import footballmarket.common.exceptions.ApplicationException;

/** Fallo global controlado que impide completar el recorrido poblacional. */
public class ValuationEvaluationException extends ApplicationException {
  public ValuationEvaluationException() {
    super("No se pudo completar la evaluación poblacional");
  }
}
