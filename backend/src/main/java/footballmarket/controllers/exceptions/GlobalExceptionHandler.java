package footballmarket.controllers.exceptions;

import footballmarket.controllers.dtos.responses.ErrorResponseDTO;
import footballmarket.exceptions.FootballMarketException;
import footballmarket.integrations.exceptions.FootballDataUnavailableException;
import footballmarket.integrations.exceptions.HeadedBrowserUnavailableException;
import footballmarket.integrations.exceptions.InvalidTheSportsDbConfigurationException;
import footballmarket.integrations.exceptions.TheSportsDbRateLimitException;
import footballmarket.integrations.exceptions.TheSportsDbUnavailableException;
import footballmarket.models.exceptions.InsufficientPlayerTokensException;
import footballmarket.models.exceptions.InsufficientUserCreditsException;
import footballmarket.services.exceptions.ArithmeticOverflowException;
import footballmarket.services.exceptions.CannotBuyOwnListingException;
import footballmarket.services.exceptions.EmailAlreadyRegisteredException;
import footballmarket.services.exceptions.InsufficientCreditsException;
import footballmarket.services.exceptions.InsufficientStockException;
import footballmarket.services.exceptions.InvalidCredentialsException;
import footballmarket.services.exceptions.InvalidListingStatusException;
import footballmarket.services.exceptions.InvalidOrderRequestException;
import footballmarket.services.exceptions.InvalidQuantityException;
import footballmarket.services.exceptions.InvalidQuotePriceException;
import footballmarket.services.exceptions.ListingNotAvailableException;
import footballmarket.services.exceptions.ListingNotFoundException;
import footballmarket.services.exceptions.PlayerIdentityRunNotFoundException;
import footballmarket.services.exceptions.PlayerIdentitySourceNotEligibleException;
import footballmarket.services.exceptions.PlayerIdentitySynchronizationException;
import footballmarket.services.exceptions.PlayerImageResolutionPersistenceException;
import footballmarket.services.exceptions.PlayerImageRunNotFoundException;
import footballmarket.services.exceptions.PlayerImageSyncInProgressException;
import footballmarket.services.exceptions.PlayerImageSynchronizationException;
import footballmarket.services.exceptions.PlayerNotFoundException;
import footballmarket.services.exceptions.PlayerNotVisibleException;
import footballmarket.services.exceptions.PlayerStatisticsSyncInProgressException;
import footballmarket.services.exceptions.PlayerStatisticsSynchronizationException;
import footballmarket.services.exceptions.PlayerSynchronizationPersistenceException;
import footballmarket.services.exceptions.QuoteContractViolationException;
import footballmarket.services.exceptions.QuoteUnavailableException;
import footballmarket.services.exceptions.SellerUnavailableException;
import footballmarket.services.exceptions.SuperuserCannotBuyException;
import footballmarket.services.exceptions.SuperuserCannotSellException;
import footballmarket.services.exceptions.UserNotFoundException;
import footballmarket.services.exceptions.UsernameAlreadyRegisteredException;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(org.springframework.web.bind.MissingServletRequestParameterException.class)
  public ResponseEntity<ErrorResponseDTO> handleMissingParameter(
      org.springframework.web.bind.MissingServletRequestParameterException exception,
      HttpServletRequest request)
      throws org.springframework.web.bind.MissingServletRequestParameterException {
    if (!request.getRequestURI().equals("/api/players/statistics/whoscored/coverage")) {
      throw exception;
    }
    boolean statisticsSource =
        request.getRequestURI().equals("/api/players/statistics/whoscored/coverage")
            && "source".equals(exception.getParameterName());
    return this.buildResponse(
        "Falta un parámetro obligatorio",
        statisticsSource ? "INVALID_STATISTICS_SOURCE" : "INVALID_PARAMETER_TYPE",
        HttpStatus.BAD_REQUEST,
        request);
  }

  @ExceptionHandler(InvalidStatisticsRequestException.class)
  public ResponseEntity<ErrorResponseDTO> handleStatisticsRequest(
      InvalidStatisticsRequestException exception, HttpServletRequest request) {
    return this.buildResponse(
        exception.getMessage(), exception.getCode(), HttpStatus.BAD_REQUEST, request);
  }

  @ExceptionHandler(org.springframework.http.converter.HttpMessageNotReadableException.class)
  public ResponseEntity<ErrorResponseDTO> handleUnreadableBody(
      org.springframework.http.converter.HttpMessageNotReadableException exception,
      HttpServletRequest request) {
    if (!request.getRequestURI().equals("/api/players/statistics/whoscored/sync/scraping")
        && !request.getRequestURI().equals("/api/players/statistics/whoscored/sync/json")) {
      return this.buildResponse(
          "Solicitud inválida", "INVALID_REQUEST", HttpStatus.BAD_REQUEST, request);
    }
    String code = "INVALID_REQUEST";
    String message = "El cuerpo de la solicitud está ausente o no es JSON válido";
    if (request.getRequestURI().equals("/api/players/statistics/whoscored/sync/scraping")) {
      for (Throwable cause = exception; cause != null; cause = cause.getCause()) {
        if (cause instanceof InvalidStatisticsRequestException invalid) {
          return this.handleStatisticsRequest(invalid, request);
        }
        if (cause instanceof tools.jackson.databind.exc.MismatchedInputException mismatch) {
          String field =
              mismatch.getPath().isEmpty() ? null : mismatch.getPath().getFirst().getPropertyName();
          if ("extractionDate".equals(field)) {
            code = "INVALID_STATISTICS_EXTRACTION_DATE";
            message = "La fecha de extracción debe tener formato ISO y no ser futura";
          } else if ("maxPlayers".equals(field)) {
            code = "INVALID_STATISTICS_MAX_PLAYERS";
            message = "El máximo de jugadores debe ser un entero positivo de 32 bits";
          }
        }
      }
    }
    return this.buildResponse(message, code, HttpStatus.BAD_REQUEST, request);
  }

  @ExceptionHandler(PlayerStatisticsSyncInProgressException.class)
  public ResponseEntity<ErrorResponseDTO> handleStatisticsSyncInProgress(
      PlayerStatisticsSyncInProgressException exception, HttpServletRequest request) {
    return this.buildResponse(
        exception.getMessage(), "PLAYER_STATISTICS_SYNC_IN_PROGRESS", HttpStatus.CONFLICT, request);
  }

  @ExceptionHandler({
    PlayerStatisticsSynchronizationException.class,
    StatisticsMappingException.class,
    footballmarket.services.exceptions.PlayerStatisticsConfirmationException.class
  })
  public ResponseEntity<ErrorResponseDTO> handleStatisticsSynchronization(
      footballmarket.exceptions.FootballMarketException exception, HttpServletRequest request) {
    return this.buildResponse(
        exception.getMessage(),
        "PLAYER_STATISTICS_SYNC_FAILED",
        HttpStatus.INTERNAL_SERVER_ERROR,
        request);
  }

  @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
  public ResponseEntity<ErrorResponseDTO> handleAccessDenied(HttpServletRequest request) {
    return this.buildResponse(
        "No tenés permiso para realizar esta operación.",
        "ACCESS_DENIED",
        HttpStatus.FORBIDDEN,
        request);
  }

  /**
   * Traduce fallos de persistencia o transacción sin exponer el detalle interno de la operación.
   */
  @ExceptionHandler({
    org.springframework.dao.DataAccessException.class,
    org.springframework.transaction.TransactionException.class,
    footballmarket.models.exceptions.InvalidTokenListingException.class
  })
  public ResponseEntity<ErrorResponseDTO> handleMarketPersistenceFailure(
      HttpServletRequest request) {
    return this.buildResponse(
        "No se pudo procesar la operación. Intentá nuevamente más tarde.",
        "INTERNAL_ERROR",
        HttpStatus.INTERNAL_SERVER_ERROR,
        request);
  }

  @ExceptionHandler(HeadedBrowserUnavailableException.class)
  public ResponseEntity<ErrorResponseDTO> handleHeadedBrowserUnavailable(
      HeadedBrowserUnavailableException exception, HttpServletRequest request) {
    return this.buildResponse(
        exception.getMessage(),
        "HEADED_BROWSER_UNAVAILABLE",
        HttpStatus.SERVICE_UNAVAILABLE,
        request);
  }

  @ExceptionHandler(InvalidOrderRequestException.class)
  public ResponseEntity<ErrorResponseDTO> handleInvalidOrder(
      InvalidOrderRequestException ex, HttpServletRequest request) {
    return this.buildResponse(ex.getMessage(), "INVALID_REQUEST", HttpStatus.BAD_REQUEST, request);
  }

  @ExceptionHandler(SuperuserCannotSellException.class)
  public ResponseEntity<ErrorResponseDTO> handleSuperuserCannotSell(
      SuperuserCannotSellException ex, HttpServletRequest request) {
    return this.buildResponse(
        ex.getMessage(), "SUPERUSER_CANNOT_SELL", HttpStatus.FORBIDDEN, request);
  }

  @ExceptionHandler(CannotBuyOwnListingException.class)
  public ResponseEntity<ErrorResponseDTO> handleOwnListing(
      CannotBuyOwnListingException ex, HttpServletRequest request) {
    return this.buildResponse(
        ex.getMessage(), "CANNOT_BUY_OWN_LISTING", HttpStatus.FORBIDDEN, request);
  }

  @ExceptionHandler(ListingNotFoundException.class)
  public ResponseEntity<ErrorResponseDTO> handleListingNotFound(
      ListingNotFoundException ex, HttpServletRequest request) {
    return this.buildResponse(ex.getMessage(), "LISTING_NOT_FOUND", HttpStatus.NOT_FOUND, request);
  }

  @ExceptionHandler(ListingNotAvailableException.class)
  public ResponseEntity<ErrorResponseDTO> handleListingNotAvailable(
      ListingNotAvailableException ex, HttpServletRequest request) {
    return this.buildResponse(
        ex.getMessage(), "LISTING_NOT_AVAILABLE", HttpStatus.CONFLICT, request);
  }

  @ExceptionHandler(PlayerIdentitySynchronizationException.class)
  public ResponseEntity<ErrorResponseDTO> handleIdentitySynchronization(
      HttpServletRequest request) {
    return this.buildResponse(
        "No se pudo completar la sincronización de identidad WhoScored",
        "PLAYER_IDENTITY_SYNC_FAILED",
        HttpStatus.INTERNAL_SERVER_ERROR,
        request);
  }

  @ExceptionHandler(InvalidPlayerIdentityIdException.class)
  public ResponseEntity<ErrorResponseDTO> handleInvalidIdentityId(
      InvalidPlayerIdentityIdException exception, HttpServletRequest request) {
    return this.buildResponse(
        exception.getMessage(), "INVALID_PLAYER_IDENTITY_ID", HttpStatus.BAD_REQUEST, request);
  }

  @ExceptionHandler(PlayerIdentityRunNotFoundException.class)
  public ResponseEntity<ErrorResponseDTO> handleIdentityRunNotFound(
      PlayerIdentityRunNotFoundException exception, HttpServletRequest request) {
    return this.buildResponse(
        exception.getMessage(), "PLAYER_IDENTITY_RUN_NOT_FOUND", HttpStatus.NOT_FOUND, request);
  }

  @ExceptionHandler(PlayerIdentitySourceNotEligibleException.class)
  public ResponseEntity<ErrorResponseDTO> handleIdentitySourceNotEligible(
      PlayerIdentitySourceNotEligibleException exception, HttpServletRequest request) {
    return this.buildResponse(
        exception.getMessage(),
        "PLAYER_IDENTITY_SOURCE_NOT_ELIGIBLE",
        HttpStatus.CONFLICT,
        request);
  }

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

  @ExceptionHandler(InvalidListingStatusException.class)
  public ResponseEntity<ErrorResponseDTO> handleInvalidListingStatus(HttpServletRequest request) {
    return this.buildResponse(
        "El parámetro de consulta no tiene un formato válido",
        "INVALID_PARAMETER_TYPE",
        HttpStatus.BAD_REQUEST,
        request);
  }

  @ExceptionHandler(
      org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class)
  public ResponseEntity<ErrorResponseDTO> handleInvalidParameter(
      org.springframework.web.method.annotation.MethodArgumentTypeMismatchException exception,
      HttpServletRequest request) {
    if (request.getRequestURI().equals("/api/players/statistics/whoscored/coverage")) {
      String code =
          "source".equals(exception.getName())
              ? "INVALID_STATISTICS_SOURCE"
              : "INVALID_STATISTICS_EXTRACTION_DATE";
      return this.buildResponse(
          "El filtro de cobertura no es válido", code, HttpStatus.BAD_REQUEST, request);
    }
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
    if (request.getRequestURI().equals("/api/players/statistics/whoscored/sync/scraping")) {
      org.springframework.validation.FieldError field = ex.getBindingResult().getFieldError();
      if (field != null) {
        String code =
            "extractionDate".equals(field.getField())
                ? "INVALID_STATISTICS_EXTRACTION_DATE"
                : "maxPlayers".equals(field.getField())
                    ? "INVALID_STATISTICS_MAX_PLAYERS"
                    : "INVALID_REQUEST";
        return this.buildResponse(field.getDefaultMessage(), code, HttpStatus.BAD_REQUEST, request);
      }
    }
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
}
