package footballmarket.features.statistics.presentation;

import footballmarket.common.exceptions.FootballMarketException;
import footballmarket.common.presentation.ErrorResponses;
import footballmarket.common.presentation.dtos.ErrorResponseDTO;
import footballmarket.features.statistics.application.exceptions.PlayerStatisticsConfirmationException;
import footballmarket.features.statistics.application.exceptions.PlayerStatisticsSyncInProgressException;
import footballmarket.features.statistics.application.exceptions.PlayerStatisticsSynchronizationException;
import footballmarket.features.statistics.presentation.exceptions.InvalidStatisticsRequestException;
import footballmarket.features.statistics.presentation.exceptions.StatisticsMappingException;
import footballmarket.integrations.whoscored.exceptions.HeadedBrowserUnavailableException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Manejador de errores HTTP propio de la feature de estadísticas de rendimiento de jugadores. */
@Order(100)
@RestControllerAdvice
public class PlayerStatisticsExceptionHandler {

  /**
   * Traduce rechazos de estructura o validación funcional de estadísticas.
   *
   * @param exception error de solicitud
   * @param request petición HTTP en curso
   * @return respuesta HTTP formateada
   */
  @ExceptionHandler(InvalidStatisticsRequestException.class)
  public ResponseEntity<ErrorResponseDTO> handleStatisticsRequest(
      InvalidStatisticsRequestException exception, HttpServletRequest request) {
    return ErrorResponses.build(
        exception.getMessage(), exception.getCode(), HttpStatus.BAD_REQUEST, request);
  }

  /**
   * Traduce el conflicto de sincronización de estadísticas activa en el canal.
   *
   * @param exception error de sincronización concurrente
   * @param request petición HTTP en curso
   * @return respuesta HTTP formateada
   */
  @ExceptionHandler(PlayerStatisticsSyncInProgressException.class)
  public ResponseEntity<ErrorResponseDTO> handleStatisticsSyncInProgress(
      PlayerStatisticsSyncInProgressException exception, HttpServletRequest request) {
    return ErrorResponses.build(
        exception.getMessage(), "PLAYER_STATISTICS_SYNC_IN_PROGRESS", HttpStatus.CONFLICT, request);
  }

  /**
   * Traduce fallos técnicos en la sincronización, confirmación o mapeo de estadísticas.
   *
   * @param exception error de sincronización
   * @param request petición HTTP en curso
   * @return respuesta HTTP formateada
   */
  @ExceptionHandler({
    PlayerStatisticsSynchronizationException.class,
    StatisticsMappingException.class,
    PlayerStatisticsConfirmationException.class
  })
  public ResponseEntity<ErrorResponseDTO> handleStatisticsSynchronization(
      FootballMarketException exception, HttpServletRequest request) {
    return ErrorResponses.build(
        exception.getMessage(),
        "PLAYER_STATISTICS_SYNC_FAILED",
        HttpStatus.INTERNAL_SERVER_ERROR,
        request);
  }

  /**
   * Traduce la indisponibilidad del navegador gráfico headed para scraping.
   *
   * @param exception error de navegador no disponible
   * @param request petición HTTP en curso
   * @return respuesta HTTP formateada
   */
  @ExceptionHandler(HeadedBrowserUnavailableException.class)
  public ResponseEntity<ErrorResponseDTO> handleHeadedBrowserUnavailable(
      HeadedBrowserUnavailableException exception, HttpServletRequest request) {
    return ErrorResponses.build(
        exception.getMessage(),
        "HEADED_BROWSER_UNAVAILABLE",
        HttpStatus.SERVICE_UNAVAILABLE,
        request);
  }
}
