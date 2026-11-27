package footballmarket.features.catalog.presentation;

import footballmarket.common.presentation.ErrorResponses;
import footballmarket.common.presentation.dtos.ErrorResponseDTO;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Especialización de validación de argumentos para los endpoints del catálogo de jugadores. */
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(assignableTypes = PlayerController.class)
public class PlayerCatalogValidationExceptionHandler {

  /**
   * Traduce los errores estructurales y de paginación del catálogo al contrato HTTP específico.
   *
   * @param exception excepción de validación de argumentos
   * @param request petición HTTP en curso
   * @return respuesta HTTP tipada
   */
  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ErrorResponseDTO> handleValidation(
      MethodArgumentNotValidException exception, HttpServletRequest request) {
    FieldError field = exception.getBindingResult().getFieldError();
    String code =
        field != null && field.isBindingFailure()
            ? "INVALID_PARAMETER_TYPE"
            : field != null && ("page".equals(field.getField()) || "size".equals(field.getField()))
                ? "INVALID_PLAYER_PAGE"
                : "INVALID_REQUEST";
    return ErrorResponses.build(
        "Los criterios del catálogo no tienen un formato válido",
        code,
        HttpStatus.BAD_REQUEST,
        request);
  }
}
