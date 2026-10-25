package footballmarket.controllers;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import footballmarket.config.SecurityConfig;
import footballmarket.models.User;
import footballmarket.models.records.OperationHistoryEntry;
import footballmarket.models.records.OperationHistoryEntry.OperationType;
import footballmarket.services.AuthenticationService;
import footballmarket.services.OperationHistoryService;
import footballmarket.services.exceptions.CurrentUserNotFoundException;
import io.jsonwebtoken.Jwts;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Date;
import java.util.List;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/** Contrato HTTP y aislamiento JWT del historial personal. */
@WebMvcTest(OperationHistoryController.class)
@Import(SecurityConfig.class)
@ActiveProfiles("test")
@org.junit.jupiter.api.Tag("lightweight")
class OperationHistoryControllerTest {

  private static final String SUBJECT = "owner@example.com";
  private static final OffsetDateTime EXECUTED_AT = OffsetDateTime.parse("2026-10-24T14:30:00Z");

  @Autowired private WebApplicationContext context;
  @Autowired private FilterChainProxy security;
  @Autowired private SecretKey key;

  @MockitoBean private OperationHistoryService historyService;
  @MockitoBean private AuthenticationService authenticationService;

  private MockMvc mvc;
  private String authorization;

  @BeforeEach
  void arrange() {
    mvc = MockMvcBuilders.webAppContextSetup(context).addFilters(security).build();
    authorization = bearer(SUBJECT, Instant.now().plusSeconds(300));
    when(authenticationService.getCurrentUser(anyString()))
        .thenAnswer(
            invocation -> new User("session-user", invocation.getArgument(0), "password123"));
  }

  @Nested
  @DisplayName("Consulta HTTP del historial")
  class Consulta {

    @Test
    @DisplayName("Devuelve el array con los ocho campos contractuales y propaga el subject")
    void devuelveHistorial() throws Exception {
      // Arrange
      when(historyService.history(SUBJECT))
          .thenReturn(
              List.of(
                  new OperationHistoryEntry(
                      92L, OperationType.SELL, 41L, "Joel Robles", 5, 125L, 625L, EXECUTED_AT)));
      // Act / Assert
      mvc.perform(get("/api/users/me/transactions").header("Authorization", authorization))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$", hasSize(1)))
          .andExpect(jsonPath("$[0].length()").value(8))
          .andExpect(jsonPath("$[0].transactionId").value(92))
          .andExpect(jsonPath("$[0].type").value("SELL"))
          .andExpect(jsonPath("$[0].playerId").value(41))
          .andExpect(jsonPath("$[0].playerName").value("Joel Robles"))
          .andExpect(jsonPath("$[0].quantity").value(5))
          .andExpect(jsonPath("$[0].unitPrice").value(125))
          .andExpect(jsonPath("$[0].totalAmount").value(625))
          .andExpect(jsonPath("$[0].executedAt").value(sameInstant(EXECUTED_AT)));
      // Verify
      verify(historyService).history(SUBJECT);
    }

    @Test
    @DisplayName("Devuelve 200 con un array vacío cuando el usuario no tiene operaciones")
    void devuelveHistorialVacio() throws Exception {
      // Arrange
      when(historyService.history(SUBJECT)).thenReturn(List.of());

      // Act / Assert
      mvc.perform(get("/api/users/me/transactions").header("Authorization", authorization))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$", hasSize(0)));

      // Verify
      verify(historyService).history(SUBJECT);
    }

    @Test
    @DisplayName("Cada sesión consulta únicamente el historial de su propio subject")
    void aíslaLaIdentidadDeSesion() throws Exception {
      // Arrange
      String otherSubject = "other@example.com";
      when(historyService.history(SUBJECT)).thenReturn(List.of());
      when(historyService.history(otherSubject)).thenReturn(List.of());
      // Act / Assert
      mvc.perform(get("/api/users/me/transactions").header("Authorization", authorization))
          .andExpect(status().isOk());
      mvc.perform(
              get("/api/users/me/transactions")
                  .header("Authorization", bearer(otherSubject, Instant.now().plusSeconds(300))))
          .andExpect(status().isOk());
      // Verify
      verify(historyService).history(SUBJECT);
      verify(historyService).history(otherSubject);
    }

    @Test
    @DisplayName("Rechaza una solicitud sin JWT antes de invocar el caso de uso")
    void rechazaSinJwt() throws Exception {
      // Act / Assert
      mvc.perform(get("/api/users/me/transactions")).andExpect(status().isUnauthorized());
      // Verify
      verifyNoInteractions(historyService);
    }

    @Test
    @DisplayName("Rechaza una credencial inválida antes de invocar el caso de uso")
    void rechazaJwtInvalido() throws Exception {
      // Act / Assert
      mvc.perform(get("/api/users/me/transactions").header("Authorization", "Bearer invalid-token"))
          .andExpect(status().isUnauthorized());
      // Verify
      verifyNoInteractions(historyService);
    }

    @Test
    @DisplayName("Rechaza una identidad autenticada que no corresponde a un usuario persistido")
    void rechazaSubjectSinUsuario() throws Exception {
      // Arrange
      when(authenticationService.getCurrentUser("missing@example.com"))
          .thenThrow(new CurrentUserNotFoundException());
      // Act / Assert
      mvc.perform(
              get("/api/users/me/transactions")
                  .header(
                      "Authorization",
                      bearer("missing@example.com", Instant.now().plusSeconds(300))))
          .andExpect(status().isUnauthorized());
      // Verify
      verifyNoInteractions(historyService);
    }

    @Test
    @DisplayName("No existe una ruta que permita seleccionar otro usuario")
    void rechazaRutaConOtraIdentidad() throws Exception {
      // Act / Assert
      mvc.perform(get("/api/users/123/transactions").header("Authorization", authorization))
          .andExpect(status().isNotFound());
      // Verify
      verifyNoInteractions(historyService);
    }

    @Test
    @DisplayName("Un fallo de persistencia responde 500 con un error interno sanitizado")
    void traduceFalloDePersistencia() throws Exception {
      // Arrange
      when(historyService.history(SUBJECT))
          .thenThrow(new DataAccessResourceFailureException("database unavailable"));

      // Act / Assert
      mvc.perform(get("/api/users/me/transactions").header("Authorization", authorization))
          .andExpect(status().isInternalServerError())
          .andExpect(jsonPath("$.length()").value(6))
          .andExpect(jsonPath("$.timestamp").exists())
          .andExpect(jsonPath("$.status").value(500))
          .andExpect(jsonPath("$.error").value("Internal Server Error"))
          .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"))
          .andExpect(
              jsonPath("$.message")
                  .value("No se pudo procesar la operación. Intentá nuevamente más tarde."))
          .andExpect(jsonPath("$.path").value("/api/users/me/transactions"));

      // Verify
      verify(historyService).history(SUBJECT);
    }
  }

  /** Exige ISO-8601 con offset y compara el instante exacto, sin depender de la zona local. */
  private static org.hamcrest.Matcher<String> sameInstant(OffsetDateTime expected) {
    return new org.hamcrest.FeatureMatcher<String, Instant>(
        org.hamcrest.Matchers.equalTo(expected.toInstant()),
        "instante ISO-8601 con offset",
        "instante") {
      @Override
      protected Instant featureValueOf(String actual) {
        return OffsetDateTime.parse(actual).toInstant();
      }
    };
  }

  private String bearer(String subject, Instant expiration) {
    return "Bearer "
        + Jwts.builder().subject(subject).expiration(Date.from(expiration)).signWith(key).compact();
  }
}
