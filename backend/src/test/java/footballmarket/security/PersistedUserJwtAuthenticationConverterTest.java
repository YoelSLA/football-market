package footballmarket.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;

@ExtendWith(MockitoExtension.class)
@org.junit.jupiter.api.Tag("lightweight")
class PersistedUserJwtAuthenticationConverterTest {

  @Mock private PersistedIdentityValidator identityValidator;
  @InjectMocks private PersistedUserJwtAuthenticationConverter converter;

  private Jwt buildJwt(String subject) {
    return new Jwt(
        "token-value",
        Instant.now(),
        Instant.now().plusSeconds(300),
        Map.of("alg", "HS256"),
        Map.of("sub", subject != null ? subject : "", "scope", "read write"));
  }

  @Nested
  @DisplayName("Conversión de token JWT")
  class TokenConversion {

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t"})
    @DisplayName("Lanza InvalidBearerTokenException cuando el sujeto es nulo o vacío")
    void lanzaInvalidBearerTokenExceptionParaSujetoInvalido(String subject) {
      Jwt jwt =
          new Jwt(
              "token-value",
              Instant.now(),
              Instant.now().plusSeconds(300),
              Map.of("alg", "HS256"),
              subject != null ? Map.of("sub", subject) : Map.of());

      assertThatThrownBy(() -> converter.convert(jwt))
          .isInstanceOf(InvalidBearerTokenException.class)
          .hasMessage("La identidad de la sesión no es válida.");

      verifyNoInteractions(identityValidator);
    }

    @Test
    @DisplayName("Lanza InvalidBearerTokenException cuando la identidad no existe")
    void lanzaInvalidBearerTokenExceptionParaUsuarioInexistente() {
      Jwt jwt = buildJwt("missing@example.com");
      when(identityValidator.exists("missing@example.com")).thenReturn(false);

      assertThatThrownBy(() -> converter.convert(jwt))
          .isInstanceOf(InvalidBearerTokenException.class)
          .hasMessage("La identidad de la sesión no es válida.");

      verify(identityValidator).exists("missing@example.com");
    }

    @Test
    @DisplayName("Propaga fallos de infraestructura sin enmascararlos como token inválido")
    void propagaFalloDeInfraestructuraSinEnmascarar() {
      Jwt jwt = buildJwt("user@example.com");
      when(identityValidator.exists("user@example.com"))
          .thenThrow(new DataAccessResourceFailureException("Conexión perdida"));

      assertThatThrownBy(() -> converter.convert(jwt))
          .isInstanceOf(DataAccessResourceFailureException.class);
    }

    @Test
    @DisplayName("Convierte exitosamente y delega autoridades para identidad existente")
    void convierteYConservaAutoridadesParaUsuarioExistente() {
      Jwt jwt = buildJwt("valid@example.com");
      when(identityValidator.exists("valid@example.com")).thenReturn(true);

      AbstractAuthenticationToken token = converter.convert(jwt);

      assertThat(token).isNotNull();
      assertThat(token.getName()).isEqualTo("valid@example.com");
      assertThat(token.getAuthorities())
          .extracting("authority")
          .containsExactlyInAnyOrder("SCOPE_read", "SCOPE_write");
    }
  }
}
