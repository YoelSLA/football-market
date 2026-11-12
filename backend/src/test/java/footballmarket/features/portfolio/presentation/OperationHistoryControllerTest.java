package footballmarket.features.portfolio.presentation;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import footballmarket.features.portfolio.application.OperationHistoryService;
import footballmarket.features.portfolio.domain.records.OperationHistoryEntry;
import footballmarket.features.portfolio.domain.records.OperationHistoryEntry.OperationType;
import footballmarket.features.portfolio.domain.records.OperationHistoryEntry.PlayerData;
import footballmarket.features.portfolio.domain.records.OperationHistoryEntry.RelatedData;
import footballmarket.features.portfolio.domain.records.OperationHistoryView;
import footballmarket.security.PersistedIdentityValidator;
import footballmarket.security.SecurityConfig;
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
  private static final OffsetDateTime EXECUTED_AT = OffsetDateTime.parse("2026-11-10T14:30:00Z");

  @Autowired private WebApplicationContext context;
  @Autowired private FilterChainProxy security;
  @Autowired private SecretKey key;
  @MockitoBean private OperationHistoryService historyService;
  @MockitoBean private PersistedIdentityValidator identityValidator;

  private MockMvc mvc;
  private String authorization;

  @BeforeEach
  void arrange() {
    mvc = MockMvcBuilders.webAppContextSetup(context).addFilters(security).build();
    authorization = bearer(SUBJECT, Instant.now().plusSeconds(300));
    when(identityValidator.exists(anyString())).thenReturn(true);
  }

  @Nested
  @DisplayName("Consulta paginada")
  class ConsultaPaginada {

    @Test
    @DisplayName("Devuelve resumen, perfil actual, transacciones y paginación")
    void devuelveContratoCompleto() throws Exception {
      // Arrange
      OperationHistoryEntry entry =
          new OperationHistoryEntry(
              92L,
              OperationType.SELL,
              new PlayerData(
                  41L,
                  "Joel Robles",
                  "https://image.test/player.png",
                  null,
                  new RelatedData(7L, "Equipo"),
                  new RelatedData(3L, "Liga")),
              5,
              125L,
              625L,
              EXECUTED_AT);
      when(historyService.history(SUBJECT, 0, 20, null, null, null, null, "NEWEST"))
          .thenReturn(
              new OperationHistoryView(
                  new OperationHistoryView.Summary(1, 0, 1, 0, 5, 0, 625),
                  List.of(entry),
                  new OperationHistoryView.Pagination(0, 20, 1, 1)));

      // Act / Assert
      mvc.perform(get("/api/users/me/transactions").header("Authorization", authorization))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.length()").value(3))
          .andExpect(jsonPath("$.summary.totalTransactions").value(1))
          .andExpect(jsonPath("$.summary.soldVolume").value(625))
          .andExpect(jsonPath("$.transactions[0].transactionId").value(92))
          .andExpect(jsonPath("$.transactions[0].type").value("SELL"))
          .andExpect(jsonPath("$.transactions[0].player.id").value(41))
          .andExpect(jsonPath("$.transactions[0].player.name").value("Joel Robles"))
          .andExpect(jsonPath("$.transactions[0].player.team.name").value("Equipo"))
          .andExpect(jsonPath("$.transactions[0].player.league.name").value("Liga"))
          .andExpect(jsonPath("$.transactions[0].quantity").value(5))
          .andExpect(jsonPath("$.transactions[0].unitPrice").value(125))
          .andExpect(jsonPath("$.transactions[0].totalAmount").value(625))
          .andExpect(jsonPath("$.pagination.totalElements").value(1));

      // Verify
      verify(historyService).history(SUBJECT, 0, 20, null, null, null, null, "NEWEST");
    }

    @Test
    @DisplayName("Propaga todos los criterios al caso de uso")
    void propagaCriterios() throws Exception {
      // Arrange
      OffsetDateTime from = OffsetDateTime.parse("2026-11-01T00:00:00Z");
      OffsetDateTime before = OffsetDateTime.parse("2026-12-01T00:00:00Z");
      when(historyService.history(SUBJECT, 2, 10, "messi", "BUY", from, before, "AMOUNT_DESC"))
          .thenReturn(emptyView(2, 10));

      // Act / Assert
      mvc.perform(
              get("/api/users/me/transactions")
                  .header("Authorization", authorization)
                  .queryParam("page", "2")
                  .queryParam("size", "10")
                  .queryParam("search", "messi")
                  .queryParam("type", "BUY")
                  .queryParam("executedFrom", from.toString())
                  .queryParam("executedBefore", before.toString())
                  .queryParam("sort", "AMOUNT_DESC"))
          .andExpect(status().isOk());

      // Verify
      verify(historyService).history(SUBJECT, 2, 10, "messi", "BUY", from, before, "AMOUNT_DESC");
    }

    @Test
    @DisplayName("Rechaza formatos inválidos antes de invocar el caso de uso")
    void rechazaFormatoInvalido() throws Exception {
      // Act / Assert
      mvc.perform(
              get("/api/users/me/transactions")
                  .header("Authorization", authorization)
                  .queryParam("page", "invalid"))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.code").value("INVALID_PARAMETER_TYPE"));

      // Verify
      verifyNoInteractions(historyService);
    }
  }

  @Nested
  @DisplayName("Seguridad")
  class Seguridad {

    @Test
    @DisplayName("Rechaza una solicitud sin JWT antes de invocar el caso de uso")
    void rechazaSinJwt() throws Exception {
      // Act / Assert
      mvc.perform(get("/api/users/me/transactions")).andExpect(status().isUnauthorized());
      // Verify
      verifyNoInteractions(historyService);
    }

    @Test
    @DisplayName("Cada sesión propaga únicamente su propio subject")
    void aislaIdentidad() throws Exception {
      // Arrange
      String otherSubject = "other@example.com";
      when(historyService.history(
              eq(SUBJECT), anyInt(), anyInt(), isNull(), isNull(), isNull(), isNull(), anyString()))
          .thenReturn(emptyView(0, 20));
      when(historyService.history(
              eq(otherSubject),
              anyInt(),
              anyInt(),
              isNull(),
              isNull(),
              isNull(),
              isNull(),
              anyString()))
          .thenReturn(emptyView(0, 20));

      // Act / Assert
      mvc.perform(get("/api/users/me/transactions").header("Authorization", authorization))
          .andExpect(status().isOk());
      mvc.perform(
              get("/api/users/me/transactions")
                  .header("Authorization", bearer(otherSubject, Instant.now().plusSeconds(300))))
          .andExpect(status().isOk());
    }
  }

  private static OperationHistoryView emptyView(int page, int size) {
    return new OperationHistoryView(
        new OperationHistoryView.Summary(0, 0, 0, 0, 0, 0, 0),
        List.of(),
        new OperationHistoryView.Pagination(page, size, 0, 0));
  }

  private String bearer(String subject, Instant expiration) {
    return "Bearer "
        + Jwts.builder().subject(subject).expiration(Date.from(expiration)).signWith(key).compact();
  }
}
