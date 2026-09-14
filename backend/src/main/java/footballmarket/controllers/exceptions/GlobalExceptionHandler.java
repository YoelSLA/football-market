package footballmarket.controllers.exceptions;

import footballmarket.services.exceptions.EmailAlreadyRegisteredException;
import footballmarket.services.exceptions.InvalidCredentialsException;
import footballmarket.services.exceptions.UserNotFoundException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class GlobalExceptionHandler {

  private ResponseEntity<Object> buildResponse(String message, HttpStatus status) {
    Map<String, Object> body = new HashMap<>();
    body.put("timestamp", LocalDateTime.now());
    body.put("message", message);
    return new ResponseEntity<>(body, status);
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<Object> handleUserAlreadyExists(MethodArgumentNotValidException ex) {
    return buildResponse(ex.getMessage(), HttpStatus.BAD_REQUEST);
  }

  @ExceptionHandler(EmailAlreadyRegisteredException.class)
  public ResponseEntity<Object> EmailAlreadyRegisteredException(
      EmailAlreadyRegisteredException ex) {
    return buildResponse(ex.getMessage(), HttpStatus.CONFLICT);
  }

  @ExceptionHandler(InvalidCredentialsException.class)
  public ResponseEntity<Object> InvalidCredentialsException(InvalidCredentialsException ex) {
    return buildResponse(
        ex.getMessage(), HttpStatus.UNAUTHORIZED); // fijarse cual codigo queda mejor.
  }

  @ExceptionHandler(UserNotFoundException.class)
  public ResponseEntity<Object> UserNotFoundException(UserNotFoundException ex) {
    return buildResponse(ex.getMessage(), HttpStatus.NOT_FOUND);
  }
}
