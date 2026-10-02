package footballmarket.integrations.exceptions;

import footballmarket.exceptions.IntegrationException;

/** Configuración inválida para el proveedor de imágenes. */
public class InvalidTheSportsDbConfigurationException extends IntegrationException {
  public InvalidTheSportsDbConfigurationException() {
    super("La configuración del proveedor de imágenes es inválida");
  }
}
