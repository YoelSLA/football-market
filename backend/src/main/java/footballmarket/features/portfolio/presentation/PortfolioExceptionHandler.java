package footballmarket.features.portfolio.presentation;

import footballmarket.common.presentation.ErrorResponses;
import footballmarket.common.presentation.dtos.ErrorResponseDTO;
import footballmarket.features.portfolio.domain.exceptions.FinancialPositionIntegrityException;
import footballmarket.features.portfolio.domain.exceptions.PortfolioIntegrityException;
import footballmarket.features.portfolio.presentation.exceptions.InvalidPortfolioPageException;
import footballmarket.features.portfolio.presentation.exceptions.InvalidPortfolioStatusException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Manejador de errores HTTP propio de la feature de cartera y posiciones financieras. */
@Order(100)
@RestControllerAdvice
public class PortfolioExceptionHandler {

  /**
   * Traduce parámetros de paginación inválidos en consultas de cartera.
   *
   * @param exception error de paginación
   * @param request petición HTTP en curso
   * @return respuesta HTTP formateada
   */
  @ExceptionHandler(InvalidPortfolioPageException.class)
  public ResponseEntity<ErrorResponseDTO> handleInvalidPortfolioPage(
      InvalidPortfolioPageException exception, HttpServletRequest request) {
    return ErrorResponses.build(
        exception.getMessage(), "INVALID_PARAMETER_TYPE", HttpStatus.BAD_REQUEST, request);
  }

  /**
   * Traduce parámetros de estado inválidos en consultas de cartera.
   *
   * @param exception error de estado
   * @param request petición HTTP en curso
   * @return respuesta HTTP formateada
   */
  @ExceptionHandler(InvalidPortfolioStatusException.class)
  public ResponseEntity<ErrorResponseDTO> handleInvalidPortfolioStatus(
      InvalidPortfolioStatusException exception, HttpServletRequest request) {
    return ErrorResponses.build(
        exception.getMessage(), "INVALID_PARAMETER_TYPE", HttpStatus.BAD_REQUEST, request);
  }

  /**
   * Traduce inconsistencias en la reconstrucción de la cartera a mensaje sanitizado.
   *
   * @param request petición HTTP en curso
   * @return respuesta HTTP formateada
   */
  @ExceptionHandler({PortfolioIntegrityException.class, FinancialPositionIntegrityException.class})
  public ResponseEntity<ErrorResponseDTO> handlePortfolioIntegrityFailure(
      HttpServletRequest request) {
    return ErrorResponses.build(
        "No se pudo procesar la consulta del portfolio. Intentá nuevamente más tarde.",
        "INTERNAL_ERROR",
        HttpStatus.INTERNAL_SERVER_ERROR,
        request);
  }
}
