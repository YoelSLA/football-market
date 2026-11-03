package footballmarket.features.market.application.exceptions;

import footballmarket.common.exceptions.ApplicationException;

/**
 * Indica que el SUPERUSER no pudo aprovisionarse porque el estado persistido no es compatible con
 * la configuración de arranque.
 */
public class SuperuserProvisioningException extends ApplicationException {

  /**
   * Crea el error con el conflicto de aprovisionamiento.
   *
   * @param message mensaje que describe el conflicto
   */
  public SuperuserProvisioningException(String message) {
    super(message);
  }
}
