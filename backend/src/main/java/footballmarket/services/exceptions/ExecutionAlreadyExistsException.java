package footballmarket.services.exceptions;

import footballmarket.exceptions.DomainException;

/** Indica que ya existe una ejecución de cotización para la fecha de cotización. */
public class ExecutionAlreadyExistsException extends DomainException {
  public ExecutionAlreadyExistsException() {
    super("Ya existe una ejecución de cotización para la fecha de cotización");
  }
}
