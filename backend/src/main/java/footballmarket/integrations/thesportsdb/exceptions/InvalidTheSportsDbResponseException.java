package footballmarket.integrations.thesportsdb.exceptions;

import footballmarket.common.exceptions.IntegrationException;

/** Respuesta permanentemente inválida sin incluir URL, credenciales ni contenido remoto. */
public class InvalidTheSportsDbResponseException extends IntegrationException {
  public InvalidTheSportsDbResponseException() {
    super("El proveedor de imágenes devolvió datos inválidos");
  }
}
