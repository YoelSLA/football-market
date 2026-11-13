package footballmarket.features.statistics.presentation;

import footballmarket.common.presentation.ErrorResponses;
import footballmarket.common.presentation.dtos.ErrorResponseDTO;
import footballmarket.features.statistics.presentation.exceptions.InvalidStatisticsRequestException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import tools.jackson.databind.exc.MismatchedInputException;

/** Especialización de lectura y validación de peticiones para sincronización de estadísticas. */
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(assignableTypes = PlayerStatisticsController.class)
public class PlayerStatisticsRequestExceptionHandler {

  /**
   * Traduce errores de deserialización y formato JSON del canal de estadísticas.
   *
   * @param exception error de lectura de cuerpo HTTP
   * @param request petición HTTP en curso
   * @return respuesta HTTP formateada
   */
  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<ErrorResponseDTO> handleUnreadableBody(
      HttpMessageNotReadableException exception, HttpServletRequest request) {
    String code = "INVALID_REQUEST";
    String message = "El cuerpo de la solicitud está ausente o no es JSON válido";
    for (Throwable cause = exception; cause != null; cause = cause.getCause()) {
      if (cause instanceof InvalidStatisticsRequestException invalid) {
        return ErrorResponses.build(
            invalid.getMessage(), invalid.getCode(), HttpStatus.BAD_REQUEST, request);
      }
      if (cause instanceof MismatchedInputException mismatch) {
        String field =
            mismatch.getPath().isEmpty() ? null : mismatch.getPath().getFirst().getPropertyName();
        if ("extractionDate".equals(field)) {
          code = "INVALID_STATISTICS_EXTRACTION_DATE";
          message = "La fecha de extracción debe tener formato ISO y no ser futura";
        }
      }
    }
    return ErrorResponses.build(message, code, HttpStatus.BAD_REQUEST, request);
  }

  /**
   * Traduce errores de validación Jakarta del canal de estadísticas.
   *
   * @param exception error de validación de argumentos
   * @param request petición HTTP en curso
   * @return respuesta HTTP formateada
   */
  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ErrorResponseDTO> handleValidation(
      MethodArgumentNotValidException exception, HttpServletRequest request) {
    FieldError field = exception.getBindingResult().getFieldError();
    if (field != null) {
      String code =
          "extractionDate".equals(field.getField())
              ? "INVALID_STATISTICS_EXTRACTION_DATE"
              : "INVALID_REQUEST";
      return ErrorResponses.build(field.getDefaultMessage(), code, HttpStatus.BAD_REQUEST, request);
    }
    return ErrorResponses.build(
        "Solicitud inválida", "INVALID_REQUEST", HttpStatus.BAD_REQUEST, request);
  }
}
