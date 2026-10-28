package footballmarket.controllers;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import footballmarket.features.auth.application.AuthenticationService;
import footballmarket.features.auth.domain.User;
import footballmarket.features.auth.domain.enums.UserRole;
import footballmarket.features.identity.application.PlayerIdentitySynchronizationService;
import footballmarket.features.identity.application.exceptions.PlayerIdentitySynchronizationException;
import footballmarket.features.identity.domain.enums.PlayerIdentityPassType;
import footballmarket.features.identity.domain.enums.PlayerIdentityRunStatus;
import footballmarket.features.identity.domain.records.PlayerIdentitySyncCounters;
import footballmarket.features.identity.domain.records.PlayerIdentitySyncSummary;
import footballmarket.features.identity.presentation.PlayerIdentityController;
import footballmarket.security.SecurityConfig;
import io.jsonwebtoken.Jwts;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@WebMvcTest(PlayerIdentityController.class)
@Import(SecurityConfig.class)
@ActiveProfiles("test")
@org.junit.jupiter.api.Tag("lightweight")
class PlayerIdentityControllerTest {
  @Autowired private WebApplicationContext context;
  @Autowired private FilterChainProxy security;
  @Autowired private SecretKey key;
  @MockitoBean private PlayerIdentitySynchronizationService service;
  @MockitoBean private AuthenticationService authentication;
  private MockMvc mvc;
  private String authorization;
  private static final String PATH = "/api/player/identities/whoscored/sync";

  @BeforeEach
  void setUp() {
    when(authentication.getCurrentUser("identity@test.com"))
        .thenReturn(
            new User(1L, "identity.user", "identity@test.com", "password123", UserRole.USER));
    mvc = MockMvcBuilders.webAppContextSetup(context).addFilters(security).build();
    authorization =
        "Bearer "
            + Jwts.builder()
                .subject("identity@test.com")
                .expiration(Date.from(Instant.now().plusSeconds(300)))
                .signWith(key)
                .compact();
  }

  @Nested
  @DisplayName("POST histórico autenticado")
  class Historical {
    private static final String HISTORICAL_PATH =
        "/api/player/identities/whoscored/sync-runs/1/historical";

    @ParameterizedTest
    @EnumSource(
        value = PlayerIdentityRunStatus.class,
        names = {"COMPLETED", "PARTIAL", "FAILED"})
    @DisplayName("Devuelve resumen histórico terminado y su origen con contrato exacto")
    void returnsHistoricalSummary(PlayerIdentityRunStatus runStatus) throws Exception {
      // Arrange
      Instant at = Instant.parse("2026-10-14T12:00:00Z");
      PlayerIdentitySyncCounters counters =
          new PlayerIdentitySyncCounters(45, 11, 1, 2, 3, 4, 5, 6, 7, 8, 9, 0, 12, 13);
      PlayerIdentitySyncSummary summary =
          new PlayerIdentitySyncSummary(
              2L,
              PlayerIdentityPassType.HISTORICAL,
              1L,
              at,
              at,
              runStatus,
              null,
              counters,
              List.of());
      when(service.synchronizeHistorical(1L)).thenReturn(summary);
      // Act / Assert
      mvc.perform(post(HISTORICAL_PATH).header("Authorization", authorization))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.*", hasSize(9)))
          .andExpect(jsonPath("$.id").value(2))
          .andExpect(jsonPath("$.sourceRunId").value(1))
          .andExpect(jsonPath("$.passType").value("HISTORICAL"))
          .andExpect(jsonPath("$.startedAt").value(at.toString()))
          .andExpect(jsonPath("$.finishedAt").value(at.toString()))
          .andExpect(jsonPath("$.status").value(runStatus.name()))
          .andExpect(jsonPath("$.failureReason").isEmpty())
          .andExpect(jsonPath("$.leagues", hasSize(0)))
          .andExpect(jsonPath("$.counters.*", hasSize(14)))
          .andExpect(jsonPath("$.counters.selectedPlayers").value(45))
          .andExpect(jsonPath("$.counters.discoveredUnique").value(11))
          .andExpect(jsonPath("$.counters.resolved").value(1))
          .andExpect(jsonPath("$.counters.alreadyReferenced").value(2))
          .andExpect(jsonPath("$.counters.conflicts").value(3))
          .andExpect(jsonPath("$.counters.noMatch").value(4))
          .andExpect(jsonPath("$.counters.ambiguous").value(5))
          .andExpect(jsonPath("$.counters.localDataErrors").value(6))
          .andExpect(jsonPath("$.counters.discoveryErrors").value(7))
          .andExpect(jsonPath("$.counters.persistenceErrors").value(8))
          .andExpect(jsonPath("$.counters.interruptedPlayers").value(9))
          .andExpect(jsonPath("$.counters.pendingPlayers").value(0))
          .andExpect(jsonPath("$.counters.providerErrors").value(12))
          .andExpect(jsonPath("$.counters.discardedRows").value(13));
    }

    @Test
    @DisplayName("Sin elegibles devuelve ejecución nueva completada y ámbitos omitidos")
    void returnsEmptyHistorical() throws Exception {
      // Arrange
      Instant at = Instant.parse("2026-10-14T12:00:00Z");
      footballmarket.features.identity.domain.models.PlayerIdentityLeagueDiscovery scope =
          new footballmarket.features.identity.domain.models.PlayerIdentityLeagueDiscovery(
              2L, 10L, "Liga", 2);
      scope.skip(at);
      PlayerIdentitySyncCounters counters =
          PlayerIdentitySyncCounters.from(java.util.Map.of(), 0, 0, 0);
      PlayerIdentitySyncSummary summary =
          new PlayerIdentitySyncSummary(
              2L,
              PlayerIdentityPassType.HISTORICAL,
              1L,
              at,
              at,
              PlayerIdentityRunStatus.COMPLETED,
              null,
              counters,
              List.of(scope));
      when(service.synchronizeHistorical(1L)).thenReturn(summary);
      // Act / Assert
      mvc.perform(post(HISTORICAL_PATH).header("Authorization", authorization))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.passType").value("HISTORICAL"))
          .andExpect(jsonPath("$.sourceRunId").value(1))
          .andExpect(jsonPath("$.status").value("COMPLETED"))
          .andExpect(
              jsonPath(
                  "$.counters.*", org.hamcrest.Matchers.everyItem(org.hamcrest.Matchers.is(0))))
          .andExpect(jsonPath("$.leagues[0].status").value("SKIPPED"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"abc", "9223372036854775808", "0", "-1"})
    @DisplayName("Rechaza tipo inválido e ID no positivo antes del Service")
    void rejectsInvalidId(String id) throws Exception {
      // Arrange
      boolean nonPositive = id.equals("0") || id.equals("-1");
      String path = "/api/player/identities/whoscored/sync-runs/" + id + "/historical";
      // Act / Assert
      mvc.perform(post(path).header("Authorization", authorization))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.status").value(400))
          .andExpect(
              jsonPath("$.code")
                  .value(nonPositive ? "INVALID_PLAYER_IDENTITY_ID" : "INVALID_PARAMETER_TYPE"))
          .andExpect(jsonPath("$.path").value(path))
          .andExpect(jsonPath("$.*", hasSize(6)))
          .andExpect(jsonPath("$.timestamp").isNotEmpty())
          .andExpect(jsonPath("$.error").value("Bad Request"))
          .andExpect(jsonPath("$.message").isNotEmpty());
      // Verify
      verifyNoInteractions(service);
    }

    @ParameterizedTest
    @ValueSource(strings = {"not-found", "ineligible", "sync-error"})
    @DisplayName("Documenta origen ausente, no elegible y errores técnicos sanitizados")
    void returnsHistoricalErrors(String kind) throws Exception {
      // Arrange
      RuntimeException failure =
          switch (kind) {
            case "not-found" ->
                new footballmarket.features.identity.application.exceptions
                    .PlayerIdentityRunNotFoundException();
            case "ineligible" ->
                new footballmarket.features.identity.application.exceptions
                    .PlayerIdentitySourceNotEligibleException();
            default -> new PlayerIdentitySynchronizationException();
          };
      int expected = kind.equals("not-found") ? 404 : kind.equals("ineligible") ? 409 : 500;
      String code =
          switch (kind) {
            case "not-found" -> "PLAYER_IDENTITY_RUN_NOT_FOUND";
            case "ineligible" -> "PLAYER_IDENTITY_SOURCE_NOT_ELIGIBLE";
            default -> "PLAYER_IDENTITY_SYNC_FAILED";
          };
      when(service.synchronizeHistorical(1L)).thenThrow(failure);
      // Act / Assert
      mvc.perform(post(HISTORICAL_PATH).header("Authorization", authorization))
          .andExpect(status().is(expected))
          .andExpect(jsonPath("$.*", hasSize(6)))
          .andExpect(jsonPath("$.status").value(expected))
          .andExpect(jsonPath("$.code").value(code))
          .andExpect(jsonPath("$.message").value(failure.getMessage()))
          .andExpect(
              jsonPath("$.error")
                  .value(org.springframework.http.HttpStatus.valueOf(expected).getReasonPhrase()))
          .andExpect(jsonPath("$.timestamp").isNotEmpty())
          .andExpect(jsonPath("$.path").value(HISTORICAL_PATH));
    }

    @ParameterizedTest
    @ValueSource(strings = {"missing", "expired", "invalid-signature"})
    @DisplayName("Rechaza JWT ausente, expirado o de firma inválida antes del caso de uso")
    void rejectsHistoricalJwt(String kind) throws Exception {
      // Arrange
      SecretKey signingKey = kind.equals("invalid-signature") ? Jwts.SIG.HS256.key().build() : key;
      String token =
          Jwts.builder()
              .subject("identity@test.com")
              .expiration(Date.from(Instant.now().plusSeconds(kind.equals("expired") ? -300 : 300)))
              .signWith(signingKey)
              .compact();
      org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder request =
          post(HISTORICAL_PATH);
      if (!kind.equals("missing")) {
        request.header("Authorization", "Bearer " + token);
      }
      // Act / Assert
      mvc.perform(request).andExpect(status().isUnauthorized());
      // Verify
      verifyNoInteractions(service);
    }
  }

  @Nested
  @DisplayName("POST actual autenticado")
  class Current {
    @Test
    @DisplayName("El pase vacío devuelve COMPLETED con fin y contadores en cero")
    void returnsEmptyFinishedRun() throws Exception {
      // Arrange
      Instant at = Instant.parse("2026-10-14T12:00:00Z");
      PlayerIdentitySyncCounters counters =
          PlayerIdentitySyncCounters.from(java.util.Map.of(), 0, 0, 0);
      PlayerIdentitySyncSummary summary =
          new PlayerIdentitySyncSummary(
              1L,
              PlayerIdentityPassType.CURRENT,
              null,
              at,
              at,
              PlayerIdentityRunStatus.COMPLETED,
              null,
              counters,
              List.of());
      when(service.synchronizeCurrent()).thenReturn(summary);
      // Act / Assert
      mvc.perform(post(PATH).header("Authorization", authorization))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.status").value("COMPLETED"))
          .andExpect(jsonPath("$.finishedAt").value(at.toString()))
          .andExpect(
              jsonPath(
                  "$.counters.*", org.hamcrest.Matchers.everyItem(org.hamcrest.Matchers.is(0))))
          .andExpect(jsonPath("$.leagues", hasSize(0)));
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    @DisplayName("Un JWT expirado o con firma ajena rechaza antes del Service")
    void rejectsInvalidJwt(boolean expired) throws Exception {
      // Arrange
      SecretKey signingKey = expired ? key : Jwts.SIG.HS256.key().build();
      String token =
          Jwts.builder()
              .subject("identity@test.com")
              .expiration(Date.from(Instant.now().plusSeconds(expired ? -300 : 300)))
              .signWith(signingKey)
              .compact();
      // Act / Assert
      mvc.perform(post(PATH).header("Authorization", "Bearer " + token))
          .andExpect(status().isUnauthorized());
      // Verify
      verifyNoInteractions(service);
    }

    @ParameterizedTest
    @EnumSource(
        value = PlayerIdentityRunStatus.class,
        names = {"COMPLETED", "PARTIAL", "FAILED"})
    @DisplayName("Devuelve el resumen terminado exacto sin exponer RUNNING ni auditoría individual")
    void returnsFinishedSummary(PlayerIdentityRunStatus runStatus) throws Exception {
      // Arrange
      Instant at = Instant.parse("2026-10-14T12:00:00Z");
      PlayerIdentitySyncCounters counters =
          new PlayerIdentitySyncCounters(45, 11, 1, 2, 3, 4, 5, 6, 7, 8, 9, 0, 12, 13);
      PlayerIdentitySyncSummary summary =
          new PlayerIdentitySyncSummary(
              1L,
              PlayerIdentityPassType.CURRENT,
              null,
              at,
              at,
              runStatus,
              null,
              counters,
              List.of());
      when(service.synchronizeCurrent()).thenReturn(summary);
      // Act / Assert
      mvc.perform(post(PATH).header("Authorization", authorization))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.*", hasSize(9)))
          .andExpect(jsonPath("$.id").value(1))
          .andExpect(jsonPath("$.passType").value("CURRENT"))
          .andExpect(jsonPath("$.sourceRunId").isEmpty())
          .andExpect(jsonPath("$.startedAt").value(at.toString()))
          .andExpect(jsonPath("$.finishedAt").value(at.toString()))
          .andExpect(jsonPath("$.status").value(runStatus.name()))
          .andExpect(jsonPath("$.failureReason").isEmpty())
          .andExpect(jsonPath("$.leagues", hasSize(0)))
          .andExpect(jsonPath("$.counters.*", hasSize(14)))
          .andExpect(jsonPath("$.counters.pendingPlayers").value(0))
          .andExpect(jsonPath("$.counters.selectedPlayers").value(45))
          .andExpect(jsonPath("$.counters.discoveredUnique").value(11))
          .andExpect(jsonPath("$.counters.resolved").value(1))
          .andExpect(jsonPath("$.counters.alreadyReferenced").value(2))
          .andExpect(jsonPath("$.counters.conflicts").value(3))
          .andExpect(jsonPath("$.counters.noMatch").value(4))
          .andExpect(jsonPath("$.counters.ambiguous").value(5))
          .andExpect(jsonPath("$.counters.localDataErrors").value(6))
          .andExpect(jsonPath("$.counters.discoveryErrors").value(7))
          .andExpect(jsonPath("$.counters.persistenceErrors").value(8))
          .andExpect(jsonPath("$.counters.interruptedPlayers").value(9))
          .andExpect(jsonPath("$.counters.providerErrors").value(12))
          .andExpect(jsonPath("$.counters.discardedRows").value(13));
    }

    @Test
    @DisplayName("Los fallos controlados devuelven ErrorResponseDTO sanitizado")
    void returnsControlledError() throws Exception {
      // Arrange
      PlayerIdentitySynchronizationException failure = new PlayerIdentitySynchronizationException();
      when(service.synchronizeCurrent()).thenThrow(failure);
      // Act / Assert
      mvc.perform(post(PATH).header("Authorization", authorization))
          .andExpect(status().isInternalServerError())
          .andExpect(jsonPath("$.*", hasSize(6)))
          .andExpect(jsonPath("$.status").value(500))
          .andExpect(jsonPath("$.code").value("PLAYER_IDENTITY_SYNC_FAILED"))
          .andExpect(jsonPath("$.path").value(PATH))
          .andExpect(jsonPath("$.timestamp").isNotEmpty())
          .andExpect(jsonPath("$.message").value(failure.getMessage()));
    }

    @Test
    @DisplayName("Sin JWT rechaza antes del Service sin asumir un DTO del filtro")
    void rejectsMissingJwt() throws Exception {
      // Act / Assert
      mvc.perform(post(PATH)).andExpect(status().isUnauthorized());
      // Verify
      verifyNoInteractions(service);
    }
  }
}
