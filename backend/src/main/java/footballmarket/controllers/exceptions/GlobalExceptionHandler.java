package footballmarket.controllers.exceptions;

import footballmarket.controllers.dtos.responses.ErrorResponseDTO;
import footballmarket.integrations.exceptions.FootballDataUnavailableException;
import footballmarket.services.exceptions.EmailAlreadyRegisteredException;
import footballmarket.services.exceptions.InvalidCredentialsException;
import footballmarket.services.exceptions.UserNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class GlobalExceptionHandler {

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

  @ExceptionHandler(InvalidCredentialsException.class)
  public ResponseEntity<ErrorResponseDTO> handleInvalidCredentials(
      InvalidCredentialsException ex, HttpServletRequest request) {
    return buildResponse(ex.getMessage(), "INVALID_CREDENTIALS", HttpStatus.UNAUTHORIZED, request);
  }

  @ExceptionHandler(UserNotFoundException.class)
  public ResponseEntity<ErrorResponseDTO> handleUserNotFound(
      UserNotFoundException ex, HttpServletRequest request) {
    return buildResponse(ex.getMessage(), "USER_NOT_FOUND", HttpStatus.NOT_FOUND, request);
  }
}
