package footballmarket.common.presentation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import footballmarket.common.presentation.dtos.ErrorResponseDTO;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataRetrievalFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;

@Tag("unit")
@DisplayName("Manejador global de excepciones transversales")
class GlobalExceptionHandlerTest {

  private GlobalExceptionHandler handler;
  private HttpServletRequest request;

  @BeforeEach
  void setUp() {
    handler = new GlobalExceptionHandler();
    request = mock(HttpServletRequest.class);
    when(request.getRequestURI()).thenReturn("/api/test");
  }

  @Nested
  @DisplayName("Errores de seguridad transversal")
  class SecurityErrors {

    @Test
    @DisplayName("Traduce AccessDeniedException a 403 ACCESS_DENIED")
    void handleAccessDenied() {
      // Arrange
      AccessDeniedException exception = new AccessDeniedException("Denegado");

      // Act
      ResponseEntity<ErrorResponseDTO> response = handler.handleAccessDenied(request);

      // Assert
      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
      assertThat(response.getBody()).isNotNull();
      assertThat(response.getBody().status()).isEqualTo(403);
      assertThat(response.getBody().code()).isEqualTo("ACCESS_DENIED");
      assertThat(response.getBody().message())
          .isEqualTo("No tenés permiso para realizar esta operación.");
      assertThat(response.getBody().path()).isEqualTo("/api/test");
    }
  }

  @Nested
  @DisplayName("Errores de persistencia y transacciones transversales")
  class PersistenceErrors {

    @Test
    @DisplayName("Traduce DataAccessException a 500 INTERNAL_ERROR sanitizado")
    void handlePersistenceFailure() {
      // Arrange
      DataRetrievalFailureException exception = new DataRetrievalFailureException("DB down");

      // Act
      ResponseEntity<ErrorResponseDTO> response =
          handler.handlePersistenceFailure(exception, request);

      // Assert
      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
      assertThat(response.getBody()).isNotNull();
      assertThat(response.getBody().status()).isEqualTo(500);
      assertThat(response.getBody().code()).isEqualTo("INTERNAL_ERROR");
      assertThat(response.getBody().message())
          .isEqualTo("No se pudo procesar la operación. Intentá nuevamente más tarde.");
      assertThat(response.getBody().path()).isEqualTo("/api/test");
    }
  }
}
