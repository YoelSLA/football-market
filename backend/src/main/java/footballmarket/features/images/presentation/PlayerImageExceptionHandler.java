package footballmarket.features.images.presentation;

import footballmarket.common.presentation.ErrorResponses;
import footballmarket.common.presentation.dtos.ErrorResponseDTO;
import footballmarket.features.images.application.exceptions.PlayerImageResolutionPersistenceException;
import footballmarket.features.images.application.exceptions.PlayerImageRunNotFoundException;
import footballmarket.features.images.application.exceptions.PlayerImageSyncInProgressException;
import footballmarket.features.images.application.exceptions.PlayerImageSynchronizationException;
import footballmarket.integrations.thesportsdb.exceptions.InvalidTheSportsDbConfigurationException;
import footballmarket.integrations.thesportsdb.exceptions.TheSportsDbRateLimitException;
import footballmarket.integrations.thesportsdb.exceptions.TheSportsDbUnavailableException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Manejador de errores HTTP propio de la feature de enriquecimiento de imágenes de jugadores. */
@Order(100)
@RestControllerAdvice
public class PlayerImageExceptionHandler {

  /**
   * Traduce la inexistencia de una ejecución de sincronización de imágenes.
   *
   * @param exception error de ejecución no encontrada
   * @param request petición HTTP en curso
   * @return respuesta HTTP formateada
   */
  @ExceptionHandler(PlayerImageRunNotFoundException.class)
  public ResponseEntity<ErrorResponseDTO> handleImageRunNotFound(
      PlayerImageRunNotFoundException exception, HttpServletRequest request) {
    return ErrorResponses.build(
        exception.getMessage(), "PLAYER_IMAGE_SYNC_RUN_NOT_FOUND", HttpStatus.NOT_FOUND, request);
  }

  /**
   * Traduce configuración inválida del proveedor TheSportsDB.
   *
   * @param request petición HTTP en curso
   * @return respuesta HTTP formateada
   */
  @ExceptionHandler(InvalidTheSportsDbConfigurationException.class)
  public ResponseEntity<ErrorResponseDTO> handleInvalidImageConfiguration(
      HttpServletRequest request) {
    return ErrorResponses.build(
        "No se pudo iniciar la sincronización de imágenes",
        "PLAYER_IMAGE_SYNC_FAILED",
        HttpStatus.BAD_GATEWAY,
        request);
  }

  /**
   * Traduce fallos en la resolución o persistencia de sincronización de imágenes.
   *
   * @param request petición HTTP en curso
   * @return respuesta HTTP formateada
   */
  @ExceptionHandler({
    PlayerImageSynchronizationException.class,
    PlayerImageResolutionPersistenceException.class
  })
  public ResponseEntity<ErrorResponseDTO> handlePlayerImageSyncFailure(HttpServletRequest request) {
    return ErrorResponses.build(
        "No se pudo completar la sincronización de imágenes",
        "PLAYER_IMAGE_SYNC_FAILED",
        HttpStatus.BAD_GATEWAY,
        request);
  }

  /**
   * Traduce indisponibilidad o límite de tasa de TheSportsDB.
   *
   * @param request petición HTTP en curso
   * @return respuesta HTTP formateada
   */
  @ExceptionHandler({TheSportsDbUnavailableException.class, TheSportsDbRateLimitException.class})
  public ResponseEntity<ErrorResponseDTO> handleImageProviderUnavailable(
      HttpServletRequest request) {
    return ErrorResponses.build(
        "No se pudo completar la lectura del proveedor de imágenes",
        "PLAYER_IMAGE_SYNC_FAILED",
        HttpStatus.BAD_GATEWAY,
        request);
  }

  /**
   * Traduce el conflicto de sincronización de imágenes ya en progreso.
   *
   * @param exception error de sincronización concurrente
   * @param request petición HTTP en curso
   * @return respuesta HTTP formateada
   */
  @ExceptionHandler(PlayerImageSyncInProgressException.class)
  public ResponseEntity<ErrorResponseDTO> handlePlayerImageSyncInProgress(
      PlayerImageSyncInProgressException exception, HttpServletRequest request) {
    return ErrorResponses.build(
        exception.getMessage(), "PLAYER_IMAGE_SYNC_IN_PROGRESS", HttpStatus.CONFLICT, request);
  }
}
