package footballmarket.features.auth.presentation;

import footballmarket.common.exceptions.FootballMarketException;
import footballmarket.common.presentation.ErrorResponses;
import footballmarket.common.presentation.dtos.ErrorResponseDTO;
import footballmarket.features.auth.application.exceptions.CurrentUserNotFoundException;
import footballmarket.features.auth.application.exceptions.EmailAlreadyRegisteredException;
import footballmarket.features.auth.application.exceptions.InvalidCredentialsException;
import footballmarket.features.auth.application.exceptions.UserNotFoundException;
import footballmarket.features.auth.application.exceptions.UsernameAlreadyRegisteredException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Manejador de errores HTTP propio de la feature de autenticación y usuarios. */
@Order(100)
@RestControllerAdvice
public class AuthenticationExceptionHandler {

  /**
   * Traduce el conflicto de correo electrónico ya registrado.
   *
   * @param exception error de correo duplicado
   * @param request petición HTTP en curso
   * @return respuesta HTTP formateada
   */
  @ExceptionHandler(EmailAlreadyRegisteredException.class)
  public ResponseEntity<ErrorResponseDTO> handleEmailAlreadyRegistered(
      EmailAlreadyRegisteredException exception, HttpServletRequest request) {
    return ErrorResponses.build(
        exception.getMessage(), "EMAIL_ALREADY_REGISTERED", HttpStatus.CONFLICT, request);
  }

  /**
   * Traduce el conflicto de nombre de usuario ya registrado.
   *
   * @param exception error de nombre de usuario duplicado
   * @param request petición HTTP en curso
   * @return respuesta HTTP formateada
   */
  @ExceptionHandler(UsernameAlreadyRegisteredException.class)
  public ResponseEntity<ErrorResponseDTO> handleUsernameAlreadyRegistered(
      UsernameAlreadyRegisteredException exception, HttpServletRequest request) {
    return ErrorResponses.build(
        exception.getMessage(), "USERNAME_ALREADY_TAKEN", HttpStatus.CONFLICT, request);
  }

  /**
   * Traduce credenciales inválidas en el proceso de autenticación.
   *
   * @param exception error de credenciales
   * @param request petición HTTP en curso
   * @return respuesta HTTP formateada
   */
  @ExceptionHandler(InvalidCredentialsException.class)
  public ResponseEntity<ErrorResponseDTO> handleInvalidCredentials(
      InvalidCredentialsException exception, HttpServletRequest request) {
    return ErrorResponses.build(
        exception.getMessage(), "INVALID_CREDENTIALS", HttpStatus.UNAUTHORIZED, request);
  }

  /**
   * Traduce la inexistencia de un usuario solicitado o del usuario en sesión.
   *
   * @param exception error de usuario no encontrado
   * @param request petición HTTP en curso
   * @return respuesta HTTP formateada
   */
  @ExceptionHandler({UserNotFoundException.class, CurrentUserNotFoundException.class})
  public ResponseEntity<ErrorResponseDTO> handleUserNotFound(
      FootballMarketException exception, HttpServletRequest request) {
    return ErrorResponses.build(
        exception.getMessage(), "USER_NOT_FOUND", HttpStatus.NOT_FOUND, request);
  }
}
