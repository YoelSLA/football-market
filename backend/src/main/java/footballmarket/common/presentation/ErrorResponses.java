package footballmarket.common.presentation;

import footballmarket.common.presentation.dtos.ErrorResponseDTO;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/** Utilidad transversal para la construcción uniforme de respuestas HTTP de error. */
public final class ErrorResponses {

  private ErrorResponses() {}

  /**
   * Construye una respuesta HTTP estandarizada con la estructura de {@link ErrorResponseDTO}.
   *
   * @param message mensaje legible del error
   * @param code identificador programático estable
   * @param status estado HTTP asignado a la respuesta
   * @param request petición HTTP entrante
   * @return respuesta HTTP tipada con el cuerpo de error
   */
  public static ResponseEntity<ErrorResponseDTO> build(
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
}
