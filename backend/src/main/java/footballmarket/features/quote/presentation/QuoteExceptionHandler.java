package footballmarket.features.quote.presentation;

import footballmarket.common.presentation.ErrorResponses;
import footballmarket.common.presentation.dtos.ErrorResponseDTO;
import footballmarket.features.quote.application.exceptions.InvalidAdjustmentFactorException;
import footballmarket.features.quote.application.exceptions.InvalidMinPriceFloorException;
import footballmarket.features.quote.application.exceptions.InvalidQuotePriceException;
import footballmarket.features.quote.application.exceptions.InvalidVariationCapException;
import footballmarket.features.quote.application.exceptions.QuoteBatchUnavailableException;
import footballmarket.features.quote.application.exceptions.QuoteNotFoundException;
import footballmarket.features.quote.application.exceptions.QuoteUnavailableException;
import footballmarket.features.quote.application.exceptions.UnknownQuoteStrategyException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Manejador de errores HTTP propio de la feature de cotizaciones de mercado. */
@Order(100)
@RestControllerAdvice
public class QuoteExceptionHandler {

  /**
   * Traduce parámetros inválidos de configuración de recálculo de cotizaciones.
   *
   * @param exception error de factor de ajuste
   * @param request petición HTTP en curso
   * @return respuesta HTTP formateada
   */
  @ExceptionHandler(InvalidAdjustmentFactorException.class)
  public ResponseEntity<ErrorResponseDTO> handleInvalidAdjustmentFactor(
      InvalidAdjustmentFactorException exception, HttpServletRequest request) {
    return ErrorResponses.build(
        exception.getMessage(), "INVALID_ADJUSTMENT_FACTOR", HttpStatus.BAD_REQUEST, request);
  }

  /**
   * Traduce topes de variación inválidos.
   *
   * @param exception error de tope de variación
   * @param request petición HTTP en curso
   * @return respuesta HTTP formateada
   */
  @ExceptionHandler(InvalidVariationCapException.class)
  public ResponseEntity<ErrorResponseDTO> handleInvalidVariationCap(
      InvalidVariationCapException exception, HttpServletRequest request) {
    return ErrorResponses.build(
        exception.getMessage(), "INVALID_VARIATION_CAP", HttpStatus.BAD_REQUEST, request);
  }

  /**
   * Traduce pisos mínimos de precio inválidos.
   *
   * @param exception error de piso de precio
   * @param request petición HTTP en curso
   * @return respuesta HTTP formateada
   */
  @ExceptionHandler(InvalidMinPriceFloorException.class)
  public ResponseEntity<ErrorResponseDTO> handleInvalidMinPriceFloor(
      InvalidMinPriceFloorException exception, HttpServletRequest request) {
    return ErrorResponses.build(
        exception.getMessage(), "INVALID_MIN_PRICE_FLOOR", HttpStatus.BAD_REQUEST, request);
  }

  /**
   * Traduce estrategias de cotización no reconocidas.
   *
   * @param exception error de estrategia desconocida
   * @param request petición HTTP en curso
   * @return respuesta HTTP formateada
   */
  @ExceptionHandler(UnknownQuoteStrategyException.class)
  public ResponseEntity<ErrorResponseDTO> handleUnknownQuoteStrategy(
      UnknownQuoteStrategyException exception, HttpServletRequest request) {
    return ErrorResponses.build(
        exception.getMessage(), "UNKNOWN_QUOTE_STRATEGY", HttpStatus.BAD_REQUEST, request);
  }

  /**
   * Traduce la inexistencia de cotizaciones para un jugador.
   *
   * @param exception error de cotización no encontrada
   * @param request petición HTTP en curso
   * @return respuesta HTTP formateada
   */
  @ExceptionHandler(QuoteNotFoundException.class)
  public ResponseEntity<ErrorResponseDTO> handleQuoteNotFound(
      QuoteNotFoundException exception, HttpServletRequest request) {
    return ErrorResponses.build(
        exception.getMessage(), "QUOTE_NOT_FOUND", HttpStatus.NOT_FOUND, request);
  }

  /**
   * Traduce precios de cotización inválidos o fuera de coherencia.
   *
   * @param exception error de precio de cotización
   * @param request petición HTTP en curso
   * @return respuesta HTTP formateada
   */
  @ExceptionHandler(InvalidQuotePriceException.class)
  public ResponseEntity<ErrorResponseDTO> handleInvalidQuotePrice(
      InvalidQuotePriceException exception, HttpServletRequest request) {
    return ErrorResponses.build(
        exception.getMessage(), "INVALID_QUOTE_PRICE", HttpStatus.CONFLICT, request);
  }

  /**
   * Traduce la indisponibilidad de cotizaciones para un jugador particular.
   *
   * @param exception error de cotización no disponible
   * @param request petición HTTP en curso
   * @return respuesta HTTP formateada
   */
  @ExceptionHandler(QuoteUnavailableException.class)
  public ResponseEntity<ErrorResponseDTO> handleQuoteUnavailable(
      QuoteUnavailableException exception, HttpServletRequest request) {
    return ErrorResponses.build(
        exception.getMessage(), "QUOTE_UNAVAILABLE", HttpStatus.CONFLICT, request);
  }

  /**
   * Traduce la indisponibilidad técnica de la fuente masiva de cotizaciones.
   *
   * @param request petición HTTP en curso
   * @return respuesta HTTP formateada
   */
  @ExceptionHandler(QuoteBatchUnavailableException.class)
  public ResponseEntity<ErrorResponseDTO> handleQuoteBatchUnavailable(HttpServletRequest request) {
    return ErrorResponses.build(
        "La fuente de cotizaciones no está disponible en este momento.",
        "QUOTE_SOURCE_UNAVAILABLE",
        HttpStatus.SERVICE_UNAVAILABLE,
        request);
  }
}
