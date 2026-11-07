package footballmarket.common.presentation;

import footballmarket.common.presentation.dtos.ErrorResponseDTO;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.transaction.TransactionException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * Manejador transversal de excepciones de Spring MVC, seguridad y persistencia.
 *
 * <p>Actúa como fallback de menor precedencia para errores estándar del framework que no hayan sido
 * interceptados por manejadores especializados de feature.
 */
@SuppressWarnings("DefaultAnnotationParam")
@Order(Ordered.LOWEST_PRECEDENCE)
@ControllerAdvice
public class GlobalExceptionHandler {

  private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  /**
   * Traduce rechazos de autorización y control de acceso de Spring Security.
   *
   * @param request petición HTTP en curso
   * @return respuesta HTTP 403 formateada
   */
  @ExceptionHandler(AccessDeniedException.class)
  public ResponseEntity<ErrorResponseDTO> handleAccessDenied(HttpServletRequest request) {
    return ErrorResponses.build(
        "No tenés permiso para realizar esta operación.",
        "ACCESS_DENIED",
        HttpStatus.FORBIDDEN,
        request);
  }

  /**
   * Traduce fallos de acceso a datos, JDBC o transacciones a error interno sanitizado.
   *
   * @param exception error de persistencia o transacción
   * @param request petición HTTP en curso
   * @return respuesta HTTP 500 formateada
   */
  @ExceptionHandler({DataAccessException.class, TransactionException.class})
  public ResponseEntity<ErrorResponseDTO> handlePersistenceFailure(
      Exception exception, HttpServletRequest request) {
    log.error(
        "Fallo de acceso a datos o transacción en {}: {}",
        request.getRequestURI(),
        exception.getMessage(),
        exception);
    return ErrorResponses.build(
        "No se pudo procesar la operación. Intentá nuevamente más tarde.",
        "INTERNAL_ERROR",
        HttpStatus.INTERNAL_SERVER_ERROR,
        request);
  }

  /**
   * Fallback general para errores de validación de argumentos (@Valid / Jakarta Validation).
   *
   * @param exception error de validación
   * @param request petición HTTP en curso
   * @return respuesta HTTP 400 formateada
   */
  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ErrorResponseDTO> handleValidation(
      MethodArgumentNotValidException exception, HttpServletRequest request) {
    String message =
        exception.getBindingResult().getAllErrors().stream()
            .map(DefaultMessageSourceResolvable::getDefaultMessage)
            .filter(value -> value != null && !value.isBlank())
            .findFirst()
            .orElse("Solicitud inválida");
    return ErrorResponses.build(message, "INVALID_REQUEST", HttpStatus.BAD_REQUEST, request);
  }

  /**
   * Fallback general para discrepancia de tipos en parámetros de consulta o variables de ruta.
   *
   * @param request petición HTTP en curso
   * @return respuesta HTTP 400 formateada
   */
  @ExceptionHandler(MethodArgumentTypeMismatchException.class)
  public ResponseEntity<ErrorResponseDTO> handleTypeMismatch(HttpServletRequest request) {
    return ErrorResponses.build(
        "El parámetro de consulta no tiene un formato válido",
        "INVALID_PARAMETER_TYPE",
        HttpStatus.BAD_REQUEST,
        request);
  }

  /**
   * Fallback general para cuerpos de petición ilegibles o JSON malformado.
   *
   * @param request petición HTTP en curso
   * @return respuesta HTTP 400 formateada
   */
  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<ErrorResponseDTO> handleHttpMessageNotReadable(HttpServletRequest request) {
    return ErrorResponses.build(
        "Solicitud inválida", "INVALID_REQUEST", HttpStatus.BAD_REQUEST, request);
  }
}
