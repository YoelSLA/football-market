package footballmarket.features.identity.presentation;

import footballmarket.common.presentation.ErrorResponses;
import footballmarket.common.presentation.dtos.ErrorResponseDTO;
import footballmarket.features.identity.application.exceptions.PlayerIdentitySynchronizationException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Manejador de errores HTTP propio de la feature de identidad de jugadores. */
@Order(100)
@RestControllerAdvice
public class PlayerIdentityExceptionHandler {

  /**
   * Traduce fallos en la sincronización de identidades de jugadores con WhoScored.
   *
   * @param request petición HTTP en curso
   * @return respuesta HTTP formateada
   */
  @ExceptionHandler(PlayerIdentitySynchronizationException.class)
  public ResponseEntity<ErrorResponseDTO> handleIdentitySynchronization(
      HttpServletRequest request) {
    return ErrorResponses.build(
        "No se pudo completar la sincronización de identidad WhoScored",
        "PLAYER_IDENTITY_SYNC_FAILED",
        HttpStatus.INTERNAL_SERVER_ERROR,
        request);
  }
}
