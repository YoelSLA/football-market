package footballmarket.features.valuation.presentation;

import footballmarket.common.presentation.ErrorResponses;
import footballmarket.common.presentation.dtos.ErrorResponseDTO;
import footballmarket.features.valuation.application.exceptions.ValuationEvaluationException;
import footballmarket.features.valuation.application.exceptions.ValuationSnapshotUnavailableException;
import footballmarket.features.valuation.presentation.exceptions.InvalidValuationRequestException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Manejador de errores HTTP propio de la feature de valoraciones estadísticas de jugadores. */
@Order(100)
@RestControllerAdvice
public class ValuationExceptionHandler {

  /**
   * Traduce solicitudes inválidas en evaluaciones de valoración.
   *
   * @param exception error de solicitud
   * @param request petición HTTP en curso
   * @return respuesta HTTP formateada
   */
  @ExceptionHandler(InvalidValuationRequestException.class)
  public ResponseEntity<ErrorResponseDTO> handleInvalidValuationRequest(
      InvalidValuationRequestException exception, HttpServletRequest request) {
    return ErrorResponses.build(
        exception.getMessage(), "INVALID_REQUEST", HttpStatus.BAD_REQUEST, request);
  }

  /**
   * Traduce la indisponibilidad de un snapshot confirmado para evaluar valoraciones.
   *
   * @param exception error de snapshot no disponible
   * @param request petición HTTP en curso
   * @return respuesta HTTP formateada
   */
  @ExceptionHandler(ValuationSnapshotUnavailableException.class)
  public ResponseEntity<ErrorResponseDTO> handleValuationSnapshotUnavailable(
      ValuationSnapshotUnavailableException exception, HttpServletRequest request) {
    return ErrorResponses.build(
        exception.getMessage(), "VALUATION_SNAPSHOT_UNAVAILABLE", HttpStatus.CONFLICT, request);
  }

  /**
   * Traduce fallos técnicos durante la corrida de evaluación de valoraciones.
   *
   * @param exception error de evaluación
   * @param request petición HTTP en curso
   * @return respuesta HTTP formateada
   */
  @ExceptionHandler(ValuationEvaluationException.class)
  public ResponseEntity<ErrorResponseDTO> handleValuationEvaluation(
      ValuationEvaluationException exception, HttpServletRequest request) {
    return ErrorResponses.build(
        exception.getMessage(),
        "VALUATION_EVALUATION_FAILED",
        HttpStatus.INTERNAL_SERVER_ERROR,
        request);
  }
}
