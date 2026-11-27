package footballmarket.features.quote.presentation;

import footballmarket.common.presentation.ErrorResponses;
import footballmarket.common.presentation.dtos.ErrorResponseDTO;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/** Especialización de parámetros de consulta para los endpoints de ejecución de cotizaciones. */
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(assignableTypes = QuoteController.class)
public class QuoteExecutionRequestExceptionHandler {

  /**
   * Traduce la omisión de parámetros requeridos en ejecuciones de cotización.
   *
   * @param request petición HTTP en curso
   * @return respuesta HTTP formateada
   */
  @ExceptionHandler(MissingServletRequestParameterException.class)
  public ResponseEntity<ErrorResponseDTO> handleMissingParameter(HttpServletRequest request) {
    return ErrorResponses.build(
        "Falta la fecha de cotización", "INVALID_REQUEST", HttpStatus.BAD_REQUEST, request);
  }

  /**
   * Traduce parámetros de consulta malformados en ejecuciones de cotización.
   *
   * @param request petición HTTP en curso
   * @return respuesta HTTP formateada
   */
  @ExceptionHandler(MethodArgumentTypeMismatchException.class)
  public ResponseEntity<ErrorResponseDTO> handleInvalidParameter(HttpServletRequest request) {
    return ErrorResponses.build(
        "La fecha de cotización debe tener formato ISO",
        "INVALID_REQUEST",
        HttpStatus.BAD_REQUEST,
        request);
  }
}
