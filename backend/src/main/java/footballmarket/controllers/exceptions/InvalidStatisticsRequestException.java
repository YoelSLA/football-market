package footballmarket.controllers.exceptions;

import footballmarket.exceptions.ApplicationException;

/** Rechazo estructural estable del contrato HTTP de estadísticas. */
public class InvalidStatisticsRequestException extends ApplicationException {
  private final String code;

  public InvalidStatisticsRequestException(String code, String message) {
    super(message);
    this.code = code;
  }

  public String getCode() {
    return this.code;
  }
}
