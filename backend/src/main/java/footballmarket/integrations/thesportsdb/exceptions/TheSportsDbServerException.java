package footballmarket.integrations.thesportsdb.exceptions;

import footballmarket.common.exceptions.IntegrationException;

/** Error de servidor en una consulta de identidad de equipo. */
public class TheSportsDbServerException extends IntegrationException {
  public TheSportsDbServerException() {
    super("TheSportsDB no pudo completar la consulta de equipo");
  }
}
