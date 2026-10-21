package footballmarket.models.exceptions;

import footballmarket.exceptions.DomainException;

/**
 * Rechaza una reconstrucción contable incoherente sin exponer datos financieros de participantes.
 */
public class FinancialPositionIntegrityException extends DomainException {
  public FinancialPositionIntegrityException() {
    super("Los datos no permiten reconstruir la posición financiera de forma consistente.");
  }
}
