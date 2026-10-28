package footballmarket.controllers;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import footballmarket.features.auth.application.AuthenticationService;
import footballmarket.features.auth.domain.User;
import footballmarket.features.auth.domain.enums.UserRole;
import footballmarket.features.images.application.PlayerImageAuditService;
import footballmarket.features.images.application.PlayerImageSynchronizationService;
import footballmarket.features.images.application.exceptions.PlayerImageRunNotFoundException;
import footballmarket.features.images.application.exceptions.PlayerImageSyncInProgressException;
import footballmarket.features.images.application.exceptions.PlayerImageSynchronizationException;
import footballmarket.features.images.domain.enums.PlayerImageResolutionStatus;
import footballmarket.features.images.domain.enums.PlayerImageSyncRunItemResult;
import footballmarket.features.images.domain.enums.PlayerImageSyncRunStatus;
import footballmarket.features.images.domain.records.PlayerImageSyncCounters;
import footballmarket.features.images.domain.records.PlayerImageSyncRunItemRecord;
import footballmarket.features.images.domain.records.PlayerImageSyncSummary;
import footballmarket.features.images.presentation.PlayerImageController;
import footballmarket.integrations.thesportsdb.exceptions.InvalidTheSportsDbConfigurationException;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@WebMvcTest(PlayerImageController.class)
@Import(SecurityConfig.class)
@ActiveProfiles("test")
@org.junit.jupiter.api.Tag("lightweight")
class PlayerImageControllerTest {
  @Autowired private WebApplicationContext context;
  @Autowired private FilterChainProxy security;
  @Autowired private SecretKey key;
  @MockitoBean private PlayerImageSynchronizationService synchronization;
  @MockitoBean private PlayerImageAuditService audit;
  @MockitoBean private AuthenticationService authenticationService;
  private MockMvc mvc;
  private String authorization;

  @BeforeEach
  void setUp() {
    when(authenticationService.getCurrentUser("images@example.com"))
        .thenReturn(
            new User(1L, "images.user", "images@example.com", "password123", UserRole.USER));
    mvc = MockMvcBuilders.webAppContextSetup(context).addFilters(security).build();
    authorization =
        "Bearer "
            + Jwts.builder()
                .subject("images@example.com")
                .expiration(Date.from(Instant.now().plusSeconds(300)))
                .signWith(key)
                .compact();
  }

  @Nested
  @DisplayName("Sincronización manual HTTP")
  class ManualSync {
    @Test
    @DisplayName("Un run sin errores responde 200 con resumen COMPLETED y contadores exactos")
    void completedSummary() throws Exception {
      PlayerImageSyncSummary summary = summary(PlayerImageSyncRunStatus.COMPLETED);
      when(synchronization.synchronize(false)).thenReturn(summary);
      mvc.perform(post("/api/players/images/sync").header("Authorization", authorization))
          .andExpect(status().isOk())
          .andExpect(
              content()
                  .json(
                      """
              {"id":43,"status":"COMPLETED","force":false,
               "startedAt":"2026-10-01T10:15:00Z","finishedAt":"2026-10-01T10:17:32Z",
               "durationMillis":152000,"failureReason":null,
               "counters":{"evaluated":1,"processed":1,"found":1,"notFound":0,
               "retryableErrors":0,"failed":0,"conflicts":0,"skippedFound":0,
               "skippedRetryWindow":0,"skippedFailed":0,"interrupted":0}}
              """,
                      org.springframework.test.json.JsonCompareMode.STRICT));
    }

    @Test
    @DisplayName("Los fallos individuales devuelven 200 y estado PARTIAL")
    void partialSummary() throws Exception {
      when(synchronization.synchronize(true)).thenReturn(summary(PlayerImageSyncRunStatus.PARTIAL));
      mvc.perform(
              post("/api/players/images/sync")
                  .param("force", "true")
                  .header("Authorization", authorization))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.status").value("PARTIAL"))
          .andExpect(jsonPath("$.counters.found").value(1));
    }

    @Test
    @DisplayName("La segunda ejecución responde 409 con el código estable")
    void rejectsConcurrentSync() throws Exception {
      when(synchronization.synchronize(false)).thenThrow(new PlayerImageSyncInProgressException());
      mvc.perform(post("/api/players/images/sync").header("Authorization", authorization))
          .andExpect(status().isConflict())
          .andExpect(jsonPath("$.code").value("PLAYER_IMAGE_SYNC_IN_PROGRESS"))
          .andExpect(jsonPath("$.path").value("/api/player/images/sync"));
    }

    @Test
    @DisplayName("Sin JWT no se invoca la sincronización")
    void rejectsAnonymousSync() throws Exception {
      mvc.perform(post("/api/players/images/sync"))
          .andExpect(status().isUnauthorized())
          .andExpect(content().string(""));
      verifyNoInteractions(synchronization);
    }

    @Test
    @DisplayName("Un fallo global devuelve un error seguro sin perder resultados previos")
    void globalFailure() throws Exception {
      when(synchronization.synchronize(false)).thenThrow(new PlayerImageSynchronizationException());
      mvc.perform(post("/api/players/images/sync").header("Authorization", authorization))
          .andExpect(status().isBadGateway())
          .andExpect(jsonPath("$.code").value("PLAYER_IMAGE_SYNC_FAILED"));
    }

    @Test
    @DisplayName("Configuración inválida impide admitir una ejecución y responde sin secretos")
    void invalidConfiguration() throws Exception {
      when(synchronization.synchronize(false))
          .thenThrow(new InvalidTheSportsDbConfigurationException());
      mvc.perform(post("/api/players/images/sync").header("Authorization", authorization))
          .andExpect(status().isBadGateway())
          .andExpect(jsonPath("$.code").value("PLAYER_IMAGE_SYNC_FAILED"))
          .andExpect(
              jsonPath("$.message").value("No se pudo iniciar la sincronización de imágenes"));
    }
  }

  @Nested
  @DisplayName("Lectura HTTP de la auditoría")
  class Audit {
    @Test
    @DisplayName("Historial paginado devuelve solo resúmenes con metadatos")
    void listsRuns() throws Exception {
      // Arrange
      PlayerImageSyncSummary run = summary(PlayerImageSyncRunStatus.COMPLETED);
      when(audit.getRuns(0, 20)).thenReturn(new PageImpl<>(List.of(run), PageRequest.of(0, 20), 1));

      // Act / Assert
      mvc.perform(get("/api/players/images/sync-runs").header("Authorization", authorization))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.content[0].id").value(43))
          .andExpect(jsonPath("$.content[0].counters.evaluated").value(1))
          .andExpect(jsonPath("$.page").value(0))
          .andExpect(jsonPath("$.size").value(20))
          .andExpect(jsonPath("$.totalElements").value(1))
          .andExpect(jsonPath("$.totalPages").value(1))
          .andExpect(jsonPath("$.content[0].items").doesNotExist());
    }

    @Test
    @DisplayName("Acepta el límite inferior del tamaño y conserva la página solicitada")
    void acceptsMinimumSize() throws Exception {
      when(audit.getRuns(1, 1)).thenReturn(new PageImpl<>(List.of(), PageRequest.of(1, 1), 2));
      mvc.perform(
              get("/api/players/images/sync-runs")
                  .param("page", "1")
                  .param("size", "1")
                  .header("Authorization", authorization))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.page").value(1))
          .andExpect(jsonPath("$.size").value(1));
    }

    @Test
    @DisplayName("El resumen individual excluye los items")
    void readsRun() throws Exception {
      when(audit.getRun(43L)).thenReturn(summary(PlayerImageSyncRunStatus.COMPLETED));
      mvc.perform(get("/api/players/images/sync-runs/43").header("Authorization", authorization))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.id").value(43))
          .andExpect(jsonPath("$.counters.found").value(1))
          .andExpect(jsonPath("$.items").doesNotExist());
    }

    @Test
    @DisplayName("Los items se entregan en una página independiente")
    void readsItems() throws Exception {
      PlayerImageSyncRunItemRecord item =
          new PlayerImageSyncRunItemRecord(
              7L,
              PlayerImageResolutionStatus.PENDING,
              PlayerImageResolutionStatus.FOUND,
              PlayerImageSyncRunItemResult.FOUND,
              true,
              true,
              false,
              null,
              false,
              false,
              "Imagen asociada");
      when(audit.getItems(43L, 0, 20))
          .thenReturn(new PageImpl<>(List.of(item), PageRequest.of(0, 20), 1));
      mvc.perform(
              get("/api/players/images/sync-runs/43/items").header("Authorization", authorization))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.content[0].playerId").value(7))
          .andExpect(jsonPath("$.content[0].previousState").value("PENDING"))
          .andExpect(jsonPath("$.content[0].finalState").value("FOUND"))
          .andExpect(jsonPath("$.content[0].result").value("FOUND"))
          .andExpect(jsonPath("$.content[0].skipReason").value(org.hamcrest.Matchers.nullValue()))
          .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    @DisplayName("Una página inválida se rechaza antes de invocar al Service")
    void rejectsInvalidPage() throws Exception {
      mvc.perform(
              get("/api/players/images/sync-runs")
                  .param("size", "101")
                  .header("Authorization", authorization))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.code").value("INVALID_PLAYER_PAGE"));
      verifyNoInteractions(audit);
    }

    @Test
    @DisplayName("Rechaza tamaño cero antes de consultar el historial")
    void rejectsZeroSize() throws Exception {
      mvc.perform(
              get("/api/players/images/sync-runs")
                  .param("size", "0")
                  .header("Authorization", authorization))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.code").value("INVALID_PLAYER_PAGE"));
      verifyNoInteractions(audit);
    }

    @Test
    @DisplayName("Una página de items inválida se rechaza antes del Service")
    void rejectsInvalidItemsPage() throws Exception {
      mvc.perform(
              get("/api/players/images/sync-runs/43/items")
                  .param("page", "-1")
                  .header("Authorization", authorization))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.code").value("INVALID_PLAYER_PAGE"));
      verifyNoInteractions(audit);
    }

    @Test
    @DisplayName("Un run inexistente responde 404 con el código estable")
    void missingRun() throws Exception {
      when(audit.getRun(999L)).thenThrow(new PlayerImageRunNotFoundException());
      mvc.perform(get("/api/players/images/sync-runs/999").header("Authorization", authorization))
          .andExpect(status().isNotFound())
          .andExpect(jsonPath("$.code").value("PLAYER_IMAGE_SYNC_RUN_NOT_FOUND"));
    }

    @Test
    @DisplayName("Los items de un run inexistente responden 404")
    void missingItems() throws Exception {
      when(audit.getItems(999L, 0, 20)).thenThrow(new PlayerImageRunNotFoundException());
      mvc.perform(
              get("/api/players/images/sync-runs/999/items").header("Authorization", authorization))
          .andExpect(status().isNotFound())
          .andExpect(jsonPath("$.code").value("PLAYER_IMAGE_SYNC_RUN_NOT_FOUND"));
    }

    @Test
    @DisplayName("Sin JWT ninguno de los tres GET consulta la auditoría")
    void requiresJwt() throws Exception {
      mvc.perform(get("/api/players/images/sync-runs"))
          .andExpect(status().isUnauthorized())
          .andExpect(content().string(""));
      mvc.perform(get("/api/players/images/sync-runs/43"))
          .andExpect(status().isUnauthorized())
          .andExpect(content().string(""));
      mvc.perform(get("/api/players/images/sync-runs/43/items"))
          .andExpect(status().isUnauthorized())
          .andExpect(content().string(""));
      verifyNoInteractions(audit);
    }
  }

  private PlayerImageSyncSummary summary(PlayerImageSyncRunStatus status) {
    PlayerImageSyncCounters counters = new PlayerImageSyncCounters(1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0);
    return new PlayerImageSyncSummary(
        43L,
        status,
        false,
        Instant.parse("2026-10-01T10:15:00Z"),
        Instant.parse("2026-10-01T10:17:32Z"),
        152000L,
        null,
        counters);
  }
}
