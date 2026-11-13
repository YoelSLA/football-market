package footballmarket.features.catalog.presentation;

import footballmarket.common.presentation.ErrorResponses;
import footballmarket.common.presentation.dtos.ErrorResponseDTO;
import footballmarket.features.catalog.application.exceptions.PlayerNotFoundException;
import footballmarket.features.catalog.application.exceptions.PlayerNotVisibleException;
import footballmarket.features.catalog.application.exceptions.PlayerSynchronizationPersistenceException;
import footballmarket.features.catalog.presentation.exceptions.InvalidPlayerIdException;
import footballmarket.features.catalog.presentation.exceptions.InvalidPlayerPageException;
import footballmarket.integrations.footballdata.exceptions.FootballDataUnavailableException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Manejador de errores HTTP propio de la feature del catálogo de jugadores. */
@Order(100)
@RestControllerAdvice
public class CatalogExceptionHandler {

  /**
   * Traduce identificadores de jugador con formato o rango inválido.
   *
   * @param exception error de identificador inválido
   * @param request petición HTTP en curso
   * @return respuesta HTTP formateada
   */
  @ExceptionHandler(InvalidPlayerIdException.class)
  public ResponseEntity<ErrorResponseDTO> handleInvalidPlayerId(
      InvalidPlayerIdException exception, HttpServletRequest request) {
    return ErrorResponses.build(
        exception.getMessage(), "INVALID_PARAMETER_TYPE", HttpStatus.BAD_REQUEST, request);
  }

  /**
   * Traduce parámetros de paginación inválidos en consultas del catálogo.
   *
   * @param exception error de paginación inválida
   * @param request petición HTTP en curso
   * @return respuesta HTTP formateada
   */
  @ExceptionHandler(InvalidPlayerPageException.class)
  public ResponseEntity<ErrorResponseDTO> handleInvalidPlayerPage(
      InvalidPlayerPageException exception, HttpServletRequest request) {
    return ErrorResponses.build(
        exception.getMessage(), "INVALID_PLAYER_PAGE", HttpStatus.BAD_REQUEST, request);
  }

  /**
   * Traduce la inexistencia de un jugador en el catálogo.
   *
   * @param exception error de jugador no encontrado
   * @param request petición HTTP en curso
   * @return respuesta HTTP formateada
   */
  @ExceptionHandler(PlayerNotFoundException.class)
  public ResponseEntity<ErrorResponseDTO> handlePlayerNotFound(
      PlayerNotFoundException exception, HttpServletRequest request) {
    return ErrorResponses.build(
        exception.getMessage(), "PLAYER_NOT_FOUND", HttpStatus.NOT_FOUND, request);
  }

  /**
   * Traduce la no visibilidad de un jugador en el catálogo.
   *
   * @param exception error de jugador no visible
   * @param request petición HTTP en curso
   * @return respuesta HTTP formateada
   */
  @ExceptionHandler(PlayerNotVisibleException.class)
  public ResponseEntity<ErrorResponseDTO> handlePlayerNotVisible(
      PlayerNotVisibleException exception, HttpServletRequest request) {
    return ErrorResponses.build(
        exception.getMessage(), "PLAYER_NOT_VISIBLE", HttpStatus.NOT_FOUND, request);
  }

  /**
   * Traduce fallos de persistencia durante la sincronización del catálogo.
   *
   * @param request petición HTTP en curso
   * @return respuesta HTTP formateada
   */
  @ExceptionHandler(PlayerSynchronizationPersistenceException.class)
  public ResponseEntity<ErrorResponseDTO> handlePlayerPersistence(HttpServletRequest request) {
    return ErrorResponses.build(
        "No se pudo aplicar la sincronización del catálogo",
        "FOOTBALL_DATA_UNAVAILABLE",
        HttpStatus.BAD_GATEWAY,
        request);
  }

  /**
   * Traduce la indisponibilidad del proveedor externo Football-Data.org.
   *
   * @param request petición HTTP en curso
   * @return respuesta HTTP formateada
   */
  @ExceptionHandler(FootballDataUnavailableException.class)
  public ResponseEntity<ErrorResponseDTO> handleFootballDataUnavailable(
      HttpServletRequest request) {
    return ErrorResponses.build(
        "No se pudo completar la lectura del proveedor de jugadores",
        "FOOTBALL_DATA_UNAVAILABLE",
        HttpStatus.BAD_GATEWAY,
        request);
  }
}
