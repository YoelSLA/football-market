package footballmarket.controllers.exceptions;

import footballmarket.controllers.dtos.responses.ErrorResponseDTO;
import footballmarket.exceptions.FootballMarketException;
import footballmarket.integrations.exceptions.FootballDataUnavailableException;
import footballmarket.integrations.exceptions.InvalidTheSportsDbConfigurationException;
import footballmarket.integrations.exceptions.TheSportsDbRateLimitException;
import footballmarket.integrations.exceptions.TheSportsDbUnavailableException;
import footballmarket.models.exceptions.InsufficientPlayerTokensException;
import footballmarket.models.exceptions.InsufficientUserCreditsException;
import footballmarket.services.exceptions.ArithmeticOverflowException;
import footballmarket.services.exceptions.EmailAlreadyRegisteredException;
import footballmarket.services.exceptions.InsufficientCreditsException;
import footballmarket.services.exceptions.InsufficientStockException;
import footballmarket.services.exceptions.InvalidCredentialsException;
import footballmarket.services.exceptions.InvalidQuantityException;
import footballmarket.services.exceptions.InvalidQuotePriceException;
import footballmarket.services.exceptions.PlayerImageResolutionPersistenceException;
import footballmarket.services.exceptions.PlayerImageRunNotFoundException;
import footballmarket.services.exceptions.PlayerImageSyncInProgressException;
import footballmarket.services.exceptions.PlayerImageSynchronizationException;
import footballmarket.services.exceptions.PlayerNotFoundException;
import footballmarket.services.exceptions.PlayerNotVisibleException;
import footballmarket.services.exceptions.PlayerSynchronizationPersistenceException;
import footballmarket.services.exceptions.QuoteContractViolationException;
import footballmarket.services.exceptions.QuoteUnavailableException;
import footballmarket.services.exceptions.SellerUnavailableException;
import footballmarket.services.exceptions.SuperuserCannotBuyException;
import footballmarket.services.exceptions.UserNotFoundException;
import footballmarket.services.exceptions.UsernameAlreadyRegisteredException;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(PlayerImageRunNotFoundException.class)
  public ResponseEntity<ErrorResponseDTO> handleImageRunNotFound(
      PlayerImageRunNotFoundException exception, HttpServletRequest request) {
    return buildResponse(
        exception.getMessage(), "PLAYER_IMAGE_SYNC_RUN_NOT_FOUND", HttpStatus.NOT_FOUND, request);
  }

  @ExceptionHandler(InvalidTheSportsDbConfigurationException.class)
  public ResponseEntity<ErrorResponseDTO> handleInvalidImageConfiguration(
      HttpServletRequest request) {
    return buildResponse(
        "No se pudo iniciar la sincronización de imágenes",
        "PLAYER_IMAGE_SYNC_FAILED",
        HttpStatus.BAD_GATEWAY,
        request);
  }

  @ExceptionHandler({
    PlayerImageSynchronizationException.class,
    PlayerImageResolutionPersistenceException.class
  })
  public ResponseEntity<ErrorResponseDTO> handlePlayerImageSyncFailure(HttpServletRequest request) {
    return buildResponse(
        "No se pudo completar la sincronización de imágenes",
        "PLAYER_IMAGE_SYNC_FAILED",
        HttpStatus.BAD_GATEWAY,
        request);
  }

  @ExceptionHandler({TheSportsDbUnavailableException.class, TheSportsDbRateLimitException.class})
  public ResponseEntity<ErrorResponseDTO> handleImageProviderUnavailable(
      HttpServletRequest request) {
    return buildResponse(
        "No se pudo completar la lectura del proveedor de imágenes",
        "PLAYER_IMAGE_SYNC_FAILED",
        HttpStatus.BAD_GATEWAY,
        request);
  }

  @ExceptionHandler(PlayerImageSyncInProgressException.class)
  public ResponseEntity<ErrorResponseDTO> handlePlayerImageSyncInProgress(
      PlayerImageSyncInProgressException exception, HttpServletRequest request) {
    return buildResponse(
        exception.getMessage(), "PLAYER_IMAGE_SYNC_IN_PROGRESS", HttpStatus.CONFLICT, request);
  }

  @ExceptionHandler(PlayerSynchronizationPersistenceException.class)
  public ResponseEntity<ErrorResponseDTO> handlePlayerPersistence(HttpServletRequest request) {
    return buildResponse(
        "No se pudo aplicar la sincronización del catálogo",
        "FOOTBALL_DATA_UNAVAILABLE",
        HttpStatus.BAD_GATEWAY,
        request);
  }

  @ExceptionHandler(FootballDataUnavailableException.class)
  public ResponseEntity<ErrorResponseDTO> handleFootballDataUnavailable(
      HttpServletRequest request) {
    return buildResponse(
        "No se pudo completar la lectura del proveedor de jugadores",
        "FOOTBALL_DATA_UNAVAILABLE",
        HttpStatus.BAD_GATEWAY,
        request);
  }

  @ExceptionHandler(InvalidPlayerPageException.class)
  public ResponseEntity<ErrorResponseDTO> handleInvalidPlayerPage(
      InvalidPlayerPageException ex, HttpServletRequest request) {
    return buildResponse(ex.getMessage(), "INVALID_PLAYER_PAGE", HttpStatus.BAD_REQUEST, request);
  }

  @ExceptionHandler(
      org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class)
  public ResponseEntity<ErrorResponseDTO> handleInvalidParameter(HttpServletRequest request) {
    return buildResponse(
        "El parámetro de consulta no tiene un formato válido",
        "INVALID_PARAMETER_TYPE",
        HttpStatus.BAD_REQUEST,
        request);
  }

  private ResponseEntity<ErrorResponseDTO> buildResponse(
      String message, String code, HttpStatus status, HttpServletRequest request) {
    ErrorResponseDTO body =
        new ErrorResponseDTO(
            LocalDateTime.now(),
            status.value(),
            status.getReasonPhrase(),
            code,
            message,
            request.getRequestURI());
    return new ResponseEntity<>(body, status);
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ErrorResponseDTO> handleValidation(
      MethodArgumentNotValidException ex, HttpServletRequest request) {
    String message =
        ex.getBindingResult().getAllErrors().stream()
            .map(error -> error.getDefaultMessage())
            .filter(value -> value != null && !value.isBlank())
            .findFirst()
            .orElse("Solicitud inválida");
    return buildResponse(message, "INVALID_REQUEST", HttpStatus.BAD_REQUEST, request);
  }

  @ExceptionHandler(EmailAlreadyRegisteredException.class)
  public ResponseEntity<ErrorResponseDTO> handleEmailAlreadyRegistered(
      EmailAlreadyRegisteredException ex, HttpServletRequest request) {
    return buildResponse(ex.getMessage(), "EMAIL_ALREADY_REGISTERED", HttpStatus.CONFLICT, request);
  }

  @ExceptionHandler(UsernameAlreadyRegisteredException.class)
  public ResponseEntity<ErrorResponseDTO> handleUsernameAlreadyRegistered(
      UsernameAlreadyRegisteredException ex, HttpServletRequest request) {
    return buildResponse(ex.getMessage(), "USERNAME_ALREADY_TAKEN", HttpStatus.CONFLICT, request);
  }

  @ExceptionHandler(InvalidCredentialsException.class)
  public ResponseEntity<ErrorResponseDTO> handleInvalidCredentials(
      InvalidCredentialsException ex, HttpServletRequest request) {
    return buildResponse(ex.getMessage(), "INVALID_CREDENTIALS", HttpStatus.UNAUTHORIZED, request);
  }

  @ExceptionHandler(PlayerNotFoundException.class)
  public ResponseEntity<ErrorResponseDTO> handlePlayerNotFound(
      PlayerNotFoundException ex, HttpServletRequest request) {
    return buildResponse(ex.getMessage(), "PLAYER_NOT_FOUND", HttpStatus.NOT_FOUND, request);
  }

  @ExceptionHandler(UserNotFoundException.class)
  public ResponseEntity<ErrorResponseDTO> handleUserNotFound(
      UserNotFoundException ex, HttpServletRequest request) {
    return buildResponse(ex.getMessage(), "USER_NOT_FOUND", HttpStatus.NOT_FOUND, request);
  }

  @ExceptionHandler(SuperuserCannotBuyException.class)
  public ResponseEntity<ErrorResponseDTO> handleSuperuserCannotBuy(
      SuperuserCannotBuyException ex, HttpServletRequest request) {
    return buildResponse(ex.getMessage(), "SUPERUSER_CANNOT_BUY", HttpStatus.FORBIDDEN, request);
  }

  @ExceptionHandler(PlayerNotVisibleException.class)
  public ResponseEntity<ErrorResponseDTO> handlePlayerNotVisible(
      PlayerNotVisibleException ex, HttpServletRequest request) {
    return buildResponse(ex.getMessage(), "PLAYER_NOT_VISIBLE", HttpStatus.NOT_FOUND, request);
  }

  @ExceptionHandler(InvalidQuantityException.class)
  public ResponseEntity<ErrorResponseDTO> handleInvalidQuantity(
      InvalidQuantityException ex, HttpServletRequest request) {
    return buildResponse(ex.getMessage(), "INVALID_QUANTITY", HttpStatus.BAD_REQUEST, request);
  }

  @ExceptionHandler(InvalidQuotePriceException.class)
  public ResponseEntity<ErrorResponseDTO> handleInvalidQuotePrice(
      InvalidQuotePriceException ex, HttpServletRequest request) {
    return buildResponse(ex.getMessage(), "INVALID_QUOTE_PRICE", HttpStatus.CONFLICT, request);
  }

  @ExceptionHandler(QuoteUnavailableException.class)
  public ResponseEntity<ErrorResponseDTO> handleQuoteUnavailable(
      QuoteUnavailableException ex, HttpServletRequest request) {
    return buildResponse(ex.getMessage(), "QUOTE_UNAVAILABLE", HttpStatus.CONFLICT, request);
  }

  @ExceptionHandler(InsufficientStockException.class)
  public ResponseEntity<ErrorResponseDTO> handleInsufficientStock(
      InsufficientStockException ex, HttpServletRequest request) {
    return buildResponse(ex.getMessage(), "INSUFFICIENT_STOCK", HttpStatus.CONFLICT, request);
  }

  @ExceptionHandler(InsufficientCreditsException.class)
  public ResponseEntity<ErrorResponseDTO> handleInsufficientCredits(
      InsufficientCreditsException ex, HttpServletRequest request) {
    return buildResponse(ex.getMessage(), "INSUFFICIENT_CREDITS", HttpStatus.CONFLICT, request);
  }

  /**
   * Las excepciones de dominio del mercado se traducen al mismo rechazo público que sus
   * equivalentes de aplicación, de modo que el contrato HTTP no dependa de qué capa produjo el
   * rechazo.
   */
  @ExceptionHandler(InsufficientUserCreditsException.class)
  public ResponseEntity<ErrorResponseDTO> handleDomainInsufficientCredits(
      InsufficientUserCreditsException ex, HttpServletRequest request) {
    return buildResponse(ex.getMessage(), "INSUFFICIENT_CREDITS", HttpStatus.CONFLICT, request);
  }

  @ExceptionHandler(InsufficientPlayerTokensException.class)
  public ResponseEntity<ErrorResponseDTO> handleDomainInsufficientStock(
      InsufficientPlayerTokensException ex, HttpServletRequest request) {
    return buildResponse(ex.getMessage(), "INSUFFICIENT_STOCK", HttpStatus.CONFLICT, request);
  }

  @ExceptionHandler({
    QuoteContractViolationException.class,
    SellerUnavailableException.class,
    ArithmeticOverflowException.class
  })
  public ResponseEntity<ErrorResponseDTO> handleInternalOrderFailure(
      FootballMarketException ex, HttpServletRequest request) {
    // El detalle técnico no se expone en la respuesta: la causa queda para los registros de la
    // aplicación y el análisis de fallos, no en este cuerpo. La respuesta pública es genérica y no
    // expone excepciones, trazas ni identificadores técnicos.
    return buildResponse(
        "No se pudo procesar la compra. Intentá nuevamente más tarde.",
        "INTERNAL_ERROR",
        HttpStatus.INTERNAL_SERVER_ERROR,
        request);
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<ErrorResponseDTO> handleUnreadableBody(
      HttpMessageNotReadableException ex, HttpServletRequest request) {
    return buildResponse("Solicitud inválida", "INVALID_REQUEST", HttpStatus.BAD_REQUEST, request);
  }
}
