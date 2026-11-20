package footballmarket.features.market.presentation;

import footballmarket.common.presentation.ErrorResponses;
import footballmarket.common.presentation.dtos.ErrorResponseDTO;
import footballmarket.features.market.application.exceptions.ArithmeticOverflowException;
import footballmarket.features.market.application.exceptions.CannotBuyOwnListingException;
import footballmarket.features.market.application.exceptions.InsufficientCreditsException;
import footballmarket.features.market.application.exceptions.InsufficientStockException;
import footballmarket.features.market.application.exceptions.InvalidListingStatusException;
import footballmarket.features.market.application.exceptions.InvalidOrderRequestException;
import footballmarket.features.market.application.exceptions.InvalidQuantityException;
import footballmarket.features.market.application.exceptions.ListingNotAvailableException;
import footballmarket.features.market.application.exceptions.ListingNotFoundException;
import footballmarket.features.market.application.exceptions.MarketAlreadyInitializedException;
import footballmarket.features.market.application.exceptions.MarketNotInitializedException;
import footballmarket.features.market.application.exceptions.SellerUnavailableException;
import footballmarket.features.market.application.exceptions.SuperuserCannotBuyException;
import footballmarket.features.market.application.exceptions.SuperuserCannotSellException;
import footballmarket.features.market.domain.exceptions.InsufficientPlayerTokensException;
import footballmarket.features.market.domain.exceptions.InsufficientUserCreditsException;
import footballmarket.features.market.domain.exceptions.InvalidTokenListingException;
import footballmarket.features.quote.application.exceptions.QuoteContractViolationException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Manejador de errores HTTP propio de la feature de mercado de tokens. */
@Order(100)
@RestControllerAdvice
public class MarketExceptionHandler {

  /**
   * Traduce solicitudes inválidas en órdenes de compra.
   *
   * @param exception error de solicitud
   * @param request petición HTTP en curso
   * @return respuesta HTTP formateada
   */
  @ExceptionHandler(InvalidOrderRequestException.class)
  public ResponseEntity<ErrorResponseDTO> handleInvalidOrder(
      InvalidOrderRequestException exception, HttpServletRequest request) {
    return ErrorResponses.build(
        exception.getMessage(), "INVALID_REQUEST", HttpStatus.BAD_REQUEST, request);
  }

  /**
   * Traduce cantidades fuera de rango en operaciones de mercado.
   *
   * @param exception error de cantidad
   * @param request petición HTTP en curso
   * @return respuesta HTTP formateada
   */
  @ExceptionHandler(InvalidQuantityException.class)
  public ResponseEntity<ErrorResponseDTO> handleInvalidQuantity(
      InvalidQuantityException exception, HttpServletRequest request) {
    return ErrorResponses.build(
        exception.getMessage(), "INVALID_QUANTITY", HttpStatus.BAD_REQUEST, request);
  }

  /**
   * Traduce parámetros de estado de publicación malformados.
   *
   * @param request petición HTTP en curso
   * @return respuesta HTTP formateada
   */
  @ExceptionHandler(InvalidListingStatusException.class)
  public ResponseEntity<ErrorResponseDTO> handleInvalidListingStatus(HttpServletRequest request) {
    return ErrorResponses.build(
        "El parámetro de consulta no tiene un formato válido",
        "INVALID_PARAMETER_TYPE",
        HttpStatus.BAD_REQUEST,
        request);
  }

  /**
   * Traduce la restricción de que el superusuario no puede vender tokens.
   *
   * @param exception error de operación prohibida
   * @param request petición HTTP en curso
   * @return respuesta HTTP formateada
   */
  @ExceptionHandler(SuperuserCannotSellException.class)
  public ResponseEntity<ErrorResponseDTO> handleSuperuserCannotSell(
      SuperuserCannotSellException exception, HttpServletRequest request) {
    return ErrorResponses.build(
        exception.getMessage(), "SUPERUSER_CANNOT_SELL", HttpStatus.FORBIDDEN, request);
  }

  /**
   * Traduce la restricción de que el superusuario no puede comprar tokens.
   *
   * @param exception error de operación prohibida
   * @param request petición HTTP en curso
   * @return respuesta HTTP formateada
   */
  @ExceptionHandler(SuperuserCannotBuyException.class)
  public ResponseEntity<ErrorResponseDTO> handleSuperuserCannotBuy(
      SuperuserCannotBuyException exception, HttpServletRequest request) {
    return ErrorResponses.build(
        exception.getMessage(), "SUPERUSER_CANNOT_BUY", HttpStatus.FORBIDDEN, request);
  }

  /**
   * Traduce el intento de comprar una publicación propia.
   *
   * @param exception error de autopatrocinio o compra propia
   * @param request petición HTTP en curso
   * @return respuesta HTTP formateada
   */
  @ExceptionHandler(CannotBuyOwnListingException.class)
  public ResponseEntity<ErrorResponseDTO> handleOwnListing(
      CannotBuyOwnListingException exception, HttpServletRequest request) {
    return ErrorResponses.build(
        exception.getMessage(), "CANNOT_BUY_OWN_LISTING", HttpStatus.FORBIDDEN, request);
  }

  /**
   * Traduce la inexistencia de una publicación.
   *
   * @param exception error de publicación no encontrada
   * @param request petición HTTP en curso
   * @return respuesta HTTP formateada
   */
  @ExceptionHandler(ListingNotFoundException.class)
  public ResponseEntity<ErrorResponseDTO> handleListingNotFound(
      ListingNotFoundException exception, HttpServletRequest request) {
    return ErrorResponses.build(
        exception.getMessage(), "LISTING_NOT_FOUND", HttpStatus.NOT_FOUND, request);
  }

  /**
   * Traduce el intento de operar sobre una publicación no disponible.
   *
   * @param exception error de publicación no disponible
   * @param request petición HTTP en curso
   * @return respuesta HTTP formateada
   */
  @ExceptionHandler(ListingNotAvailableException.class)
  public ResponseEntity<ErrorResponseDTO> handleListingNotAvailable(
      ListingNotAvailableException exception, HttpServletRequest request) {
    return ErrorResponses.build(
        exception.getMessage(), "LISTING_NOT_AVAILABLE", HttpStatus.CONFLICT, request);
  }

  /**
   * Traduce operaciones ejecutadas antes de inicializar el mercado.
   *
   * @param exception error de mercado no inicializado
   * @param request petición HTTP en curso
   * @return respuesta HTTP formateada
   */
  @ExceptionHandler(MarketNotInitializedException.class)
  public ResponseEntity<ErrorResponseDTO> handleMarketNotInitialized(
      MarketNotInitializedException exception, HttpServletRequest request) {
    return ErrorResponses.build(
        exception.getMessage(), "MARKET_NOT_INITIALIZED", HttpStatus.CONFLICT, request);
  }

  /**
   * Traduce el conflicto de inicialización duplicada del mercado.
   *
   * @param exception error de mercado ya inicializado
   * @param request petición HTTP en curso
   * @return respuesta HTTP formateada
   */
  @ExceptionHandler(MarketAlreadyInitializedException.class)
  public ResponseEntity<ErrorResponseDTO> handleMarketAlreadyInitialized(
      MarketAlreadyInitializedException exception, HttpServletRequest request) {
    return ErrorResponses.build(
        exception.getMessage(), "MARKET_ALREADY_INITIALIZED", HttpStatus.CONFLICT, request);
  }

  /**
   * Traduce la falta de stock o tokens de jugador, tanto de dominio como de aplicación.
   *
   * @param exception error de existencias insuficientes
   * @param request petición HTTP en curso
   * @return respuesta HTTP formateada
   */
  @ExceptionHandler({InsufficientStockException.class, InsufficientPlayerTokensException.class})
  public ResponseEntity<ErrorResponseDTO> handleInsufficientStock(
      footballmarket.common.exceptions.FootballMarketException exception,
      HttpServletRequest request) {
    return ErrorResponses.build(
        exception.getMessage(), "INSUFFICIENT_STOCK", HttpStatus.CONFLICT, request);
  }

  /**
   * Traduce la falta de créditos del usuario, tanto de dominio como de aplicación.
   *
   * @param exception error de créditos insuficientes
   * @param request petición HTTP en curso
   * @return respuesta HTTP formateada
   */
  @ExceptionHandler({InsufficientCreditsException.class, InsufficientUserCreditsException.class})
  public ResponseEntity<ErrorResponseDTO> handleInsufficientCredits(
      footballmarket.common.exceptions.FootballMarketException exception,
      HttpServletRequest request) {
    return ErrorResponses.build(
        exception.getMessage(), "INSUFFICIENT_CREDITS", HttpStatus.CONFLICT, request);
  }

  /**
   * Traduce inconsistencias en publicaciones de tokens a error interno sanitizado.
   *
   * @param request petición HTTP en curso
   * @return respuesta HTTP formateada
   */
  @ExceptionHandler(InvalidTokenListingException.class)
  public ResponseEntity<ErrorResponseDTO> handleInvalidTokenListing(HttpServletRequest request) {
    return ErrorResponses.build(
        "No se pudo procesar la operación. Intentá nuevamente más tarde.",
        "INTERNAL_ERROR",
        HttpStatus.INTERNAL_SERVER_ERROR,
        request);
  }

  /**
   * Traduce fallos internos durante la compra de órdenes a mensaje contractual sanitizado.
   *
   * @param request petición HTTP en curso
   * @return respuesta HTTP formateada
   */
  @ExceptionHandler({
    QuoteContractViolationException.class,
    SellerUnavailableException.class,
    ArithmeticOverflowException.class
  })
  public ResponseEntity<ErrorResponseDTO> handleInternalOrderFailure(HttpServletRequest request) {
    return ErrorResponses.build(
        "No se pudo procesar la compra. Intentá nuevamente más tarde.",
        "INTERNAL_ERROR",
        HttpStatus.INTERNAL_SERVER_ERROR,
        request);
  }
}
