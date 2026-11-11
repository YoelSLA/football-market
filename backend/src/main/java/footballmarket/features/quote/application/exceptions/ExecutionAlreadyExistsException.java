package footballmarket.features.quote.application.exceptions;

import footballmarket.common.exceptions.ApplicationException;

/** Indica que ya se reservó la corrida automática de la fecha UTC. */
public class ExecutionAlreadyExistsException extends ApplicationException {
  public ExecutionAlreadyExistsException() {
    super("Ya existe una ejecución automática para la fecha de cotización");
  }
}
