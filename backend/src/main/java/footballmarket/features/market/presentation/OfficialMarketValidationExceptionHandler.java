package footballmarket.features.market.presentation;

import footballmarket.common.presentation.ErrorResponses;
import footballmarket.common.presentation.dtos.ErrorResponseDTO;
import footballmarket.features.market.application.exceptions.InvalidPageSizeException;
import footballmarket.features.market.application.exceptions.InvalidPaginationException;
import footballmarket.features.market.application.exceptions.InvalidPriceRangeException;
import footballmarket.features.market.application.exceptions.InvalidSortParameterException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/** Manejador especializado de validación y errores HTTP para el controlador del Mercado Oficial. */
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(assignableTypes = OfficialMarketController.class)
public class OfficialMarketValidationExceptionHandler {

  /** Traduce errores de validación Jakarta (@Valid) a los códigos específicos del contrato. */
  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ErrorResponseDTO> handleValidation(
      MethodArgumentNotValidException exception, HttpServletRequest request) {
    FieldError field = exception.getBindingResult().getFieldError();
    if (field != null) {
      if (field.isBindingFailure()) {
        return ErrorResponses.build(
            "El parámetro de consulta no tiene un formato válido",
            "INVALID_PARAMETER_TYPE",
            HttpStatus.BAD_REQUEST,
            request);
      }
      String fieldName = field.getField();
      if ("page".equals(fieldName)) {
        return ErrorResponses.build(
            "El número de página no puede ser negativo",
            "INVALID_PAGINATION",
            HttpStatus.BAD_REQUEST,
            request);
      }
      if ("size".equals(fieldName)) {
        return ErrorResponses.build(
            "El tamaño de página debe estar entre 1 y 100",
            "INVALID_PAGE_SIZE",
            HttpStatus.BAD_REQUEST,
            request);
      }
      if ("minPrice".equals(fieldName) || "maxPrice".equals(fieldName)) {
        return ErrorResponses.build(
            "El rango de precios solicitado no es válido",
            "INVALID_PRICE_RANGE",
            HttpStatus.BAD_REQUEST,
            request);
      }
      if ("sort".equals(fieldName)) {
        return ErrorResponses.build(
            "El criterio de ordenamiento no es válido",
            "INVALID_SORT_PARAMETER",
            HttpStatus.BAD_REQUEST,
            request);
      }
    }
    return ErrorResponses.build(
        "Los parámetros de consulta no tienen un formato válido",
        "INVALID_REQUEST",
        HttpStatus.BAD_REQUEST,
        request);
  }

  /** Traduce discrepancias de tipos en parámetros numéricos o booleanos. */
  @ExceptionHandler(MethodArgumentTypeMismatchException.class)
  public ResponseEntity<ErrorResponseDTO> handleTypeMismatch(
      MethodArgumentTypeMismatchException exception, HttpServletRequest request) {
    return ErrorResponses.build(
        "El parámetro de consulta no tiene un formato válido",
        "INVALID_PARAMETER_TYPE",
        HttpStatus.BAD_REQUEST,
        request);
  }

  @ExceptionHandler(InvalidPriceRangeException.class)
  public ResponseEntity<ErrorResponseDTO> handleInvalidPriceRange(
      InvalidPriceRangeException exception, HttpServletRequest request) {
    return ErrorResponses.build(
        exception.getMessage(), "INVALID_PRICE_RANGE", HttpStatus.BAD_REQUEST, request);
  }

  @ExceptionHandler(InvalidSortParameterException.class)
  public ResponseEntity<ErrorResponseDTO> handleInvalidSort(
      InvalidSortParameterException exception, HttpServletRequest request) {
    return ErrorResponses.build(
        exception.getMessage(), "INVALID_SORT_PARAMETER", HttpStatus.BAD_REQUEST, request);
  }

  @ExceptionHandler(InvalidPaginationException.class)
  public ResponseEntity<ErrorResponseDTO> handleInvalidPagination(
      InvalidPaginationException exception, HttpServletRequest request) {
    return ErrorResponses.build(
        exception.getMessage(), "INVALID_PAGINATION", HttpStatus.BAD_REQUEST, request);
  }

  @ExceptionHandler(InvalidPageSizeException.class)
  public ResponseEntity<ErrorResponseDTO> handleInvalidPageSize(
      InvalidPageSizeException exception, HttpServletRequest request) {
    return ErrorResponses.build(
        exception.getMessage(), "INVALID_PAGE_SIZE", HttpStatus.BAD_REQUEST, request);
  }
}
