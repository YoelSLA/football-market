package footballmarket.features.identity.presentation;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import footballmarket.features.identity.application.PlayerIdentitySynchronizationService;
import footballmarket.features.identity.application.exceptions.PlayerIdentitySynchronizationException;
import footballmarket.features.identity.domain.enums.PlayerIdentityRunStatus;
import footballmarket.features.identity.domain.records.PlayerIdentitySyncSummary;
import footballmarket.security.PersistedIdentityValidator;
import footballmarket.security.SecurityConfig;
import io.jsonwebtoken.Jwts;
import java.time.Instant;
import java.util.Date;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
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
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@WebMvcTest(PlayerIdentityController.class)
@Import(SecurityConfig.class)
@ActiveProfiles("test")
@Tag("lightweight")
class PlayerIdentityControllerTest {
  private static final String PATH = "/api/players/identities/whoscored/sync";
  @Autowired private WebApplicationContext context;
  @Autowired private FilterChainProxy security;
  @Autowired private SecretKey key;
  @MockitoBean private PlayerIdentitySynchronizationService service;
  @MockitoBean private PersistedIdentityValidator identityValidator;
  private MockMvc mvc;
  private String authorization;

  @BeforeEach
  void setUp() {
    when(identityValidator.exists("identity@test.com")).thenReturn(true);
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
  @DisplayName("Contrato único de sincronización")
  class Synchronization {
    @ParameterizedTest
    @EnumSource(PlayerIdentityRunStatus.class)
    @DisplayName("Devuelve estado global y solo cuatro totales locales sin datos de auditoría")
    void returnsExactSummary(PlayerIdentityRunStatus runStatus) throws Exception {
      // Arrange
      PlayerIdentitySyncSummary summary = new PlayerIdentitySyncSummary(runStatus, 10, 7, 2, 1);
      when(service.synchronize()).thenReturn(summary);
      // Act / Assert
      mvc.perform(post(PATH).header("Authorization", authorization))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.*", hasSize(5)))
          .andExpect(jsonPath("$.status").value(runStatus.name()))
          .andExpect(jsonPath("$.selectedPlayers").value(10))
          .andExpect(jsonPath("$.referencedPlayers").value(7))
          .andExpect(jsonPath("$.noMatchPlayers").value(2))
          .andExpect(jsonPath("$.unresolvedPlayers").value(1));
    }

    @Test
    @DisplayName("El catálogo vacío devuelve COMPLETED y todos los totales en cero")
    void returnsEmptySummary() throws Exception {
      // Arrange
      PlayerIdentitySyncSummary summary =
          new PlayerIdentitySyncSummary(PlayerIdentityRunStatus.COMPLETED, 0, 0, 0, 0);
      when(service.synchronize()).thenReturn(summary);
      // Act / Assert
      mvc.perform(post(PATH).header("Authorization", authorization))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.*", hasSize(5)))
          .andExpect(jsonPath("$.status").value("COMPLETED"))
          .andExpect(jsonPath("$.selectedPlayers").value(0))
          .andExpect(jsonPath("$.referencedPlayers").value(0))
          .andExpect(jsonPath("$.noMatchPlayers").value(0))
          .andExpect(jsonPath("$.unresolvedPlayers").value(0));
    }

    @Test
    @DisplayName("Los fallos globales controlados mantienen el contrato de error sanitizado")
    void returnsControlledError() throws Exception {
      // Arrange
      PlayerIdentitySynchronizationException failure = new PlayerIdentitySynchronizationException();
      when(service.synchronize()).thenThrow(failure);
      // Act / Assert
      mvc.perform(post(PATH).header("Authorization", authorization))
          .andExpect(status().isInternalServerError())
          .andExpect(jsonPath("$.*", hasSize(6)))
          .andExpect(jsonPath("$.status").value(500))
          .andExpect(jsonPath("$.error").value("Internal Server Error"))
          .andExpect(jsonPath("$.code").value("PLAYER_IDENTITY_SYNC_FAILED"))
          .andExpect(jsonPath("$.path").value(PATH))
          .andExpect(jsonPath("$.timestamp").isNotEmpty())
          .andExpect(jsonPath("$.message").value(failure.getMessage()));
    }

    @Test
    @DisplayName("La ruta histórica retirada no ejecuta el Service")
    void removesHistoricalEndpoint() throws Exception {
      // Act / Assert
      mvc.perform(
              post("/api/players/identities/whoscored/sync-runs/1/historical")
                  .header("Authorization", authorization))
          .andExpect(status().isNotFound());
      // Verify
      verifyNoInteractions(service);
    }
  }

  @Nested
  @DisplayName("Autenticación de la operación única")
  class Authentication {
    @ParameterizedTest
    @ValueSource(strings = {"missing", "expired", "invalid-signature"})
    @DisplayName("Rechaza JWT ausente, expirado o de firma inválida antes del Service")
    void rejectsInvalidJwt(String kind) throws Exception {
      // Arrange
      SecretKey signingKey = kind.equals("invalid-signature") ? Jwts.SIG.HS256.key().build() : key;
      String token =
          Jwts.builder()
              .subject("identity@test.com")
              .expiration(Date.from(Instant.now().plusSeconds(kind.equals("expired") ? -300 : 300)))
              .signWith(signingKey)
              .compact();
      MockHttpServletRequestBuilder request = post(PATH);
      if (!kind.equals("missing")) {
        request.header("Authorization", "Bearer " + token);
      }
      // Act / Assert
      mvc.perform(request).andExpect(status().isUnauthorized());
      // Verify
      verifyNoInteractions(service);
    }
  }
}
