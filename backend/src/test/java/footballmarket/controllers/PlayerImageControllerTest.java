package footballmarket.controllers;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import footballmarket.config.SecurityConfig;
import footballmarket.integrations.exceptions.InvalidTheSportsDbConfigurationException;
import footballmarket.models.User;
import footballmarket.models.enums.PlayerImageResolutionStatus;
import footballmarket.models.enums.PlayerImageSyncRunItemResult;
import footballmarket.models.enums.PlayerImageSyncRunStatus;
import footballmarket.models.records.PlayerImageSyncCounters;
import footballmarket.models.records.PlayerImageSyncRunItemRecord;
import footballmarket.models.records.PlayerImageSyncSummary;
import footballmarket.services.AuthenticationService;
import footballmarket.services.PlayerImageAuditService;
import footballmarket.services.PlayerImageSynchronizationService;
import footballmarket.services.exceptions.PlayerImageRunNotFoundException;
import footballmarket.services.exceptions.PlayerImageSyncInProgressException;
import footballmarket.services.exceptions.PlayerImageSynchronizationException;
import io.jsonwebtoken.Jwts;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.restdocs.RestDocumentationContextProvider;
import org.springframework.restdocs.RestDocumentationExtension;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@WebMvcTest(PlayerImageController.class)
@Import(SecurityConfig.class)
@ActiveProfiles("test")
@ExtendWith(RestDocumentationExtension.class)
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
  void setUp(RestDocumentationContextProvider documentation) {
    when(authenticationService.getCurrentUser("images@example.com"))
        .thenReturn(new User(1L, "images@example.com", "password123"));
    mvc =
        MockMvcBuilders.webAppContextSetup(context)
            .addFilters(security)
            .apply(documentationConfiguration(documentation))
            .build();
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
                      org.springframework.test.json.JsonCompareMode.STRICT))
          .andDo(
              document(
                  "player-images-sync",
                  responseFields(
                      fieldWithPath("id").description("Identificador interno del run"),
                      fieldWithPath("status").description("Estado final de la ejecución"),
                      fieldWithPath("force").description("Refresco forzado"),
                      fieldWithPath("startedAt").description("Inicio UTC"),
                      fieldWithPath("finishedAt").description("Fin UTC"),
                      fieldWithPath("durationMillis").description("Duración en milisegundos"),
                      fieldWithPath("failureReason").description("Motivo de fallo global o null"),
                      fieldWithPath("counters").description("Resultados y omisiones"),
                      fieldWithPath("counters.evaluated").description("Activos evaluados"),
                      fieldWithPath("counters.processed")
                          .description("Intentos reales de enriquecimiento"),
                      fieldWithPath("counters.found").description("Jugadores con imagen"),
                      fieldWithPath("counters.notFound").description("Ausencias funcionales"),
                      fieldWithPath("counters.retryableErrors").description("Errores transitorios"),
                      fieldWithPath("counters.failed").description("Fallos individuales"),
                      fieldWithPath("counters.conflicts")
                          .description("Conflictos, subconjunto de failed"),
                      fieldWithPath("counters.skippedFound").description("Omitidos con imagen"),
                      fieldWithPath("counters.skippedRetryWindow")
                          .description("Omitidos por ventana"),
                      fieldWithPath("counters.skippedFailed")
                          .description("Omitidos por fallo anterior"),
                      fieldWithPath("counters.interrupted").description("Items interrumpidos"))));
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
          .andExpect(jsonPath("$.counters.found").value(1))
          .andDo(document("player-images-sync-partial"));
    }

    @Test
    @DisplayName("La segunda ejecución responde 409 con el código estable")
    void rejectsConcurrentSync() throws Exception {
      when(synchronization.synchronize(false)).thenThrow(new PlayerImageSyncInProgressException());
      mvc.perform(post("/api/players/images/sync").header("Authorization", authorization))
          .andExpect(status().isConflict())
          .andExpect(jsonPath("$.code").value("PLAYER_IMAGE_SYNC_IN_PROGRESS"))
          .andExpect(jsonPath("$.path").value("/api/players/images/sync"))
          .andDo(document("player-images-sync-conflict"));
    }

    @Test
    @DisplayName("Sin JWT no se invoca la sincronización")
    void rejectsAnonymousSync() throws Exception {
      mvc.perform(post("/api/players/images/sync"))
          .andExpect(status().isUnauthorized())
          .andExpect(content().string(""))
          .andDo(document("player-images-sync-unauthorized"));
      verifyNoInteractions(synchronization);
    }

    @Test
    @DisplayName("Un fallo global devuelve un error seguro sin perder resultados previos")
    void globalFailure() throws Exception {
      when(synchronization.synchronize(false)).thenThrow(new PlayerImageSynchronizationException());
      mvc.perform(post("/api/players/images/sync").header("Authorization", authorization))
          .andExpect(status().isBadGateway())
          .andExpect(jsonPath("$.code").value("PLAYER_IMAGE_SYNC_FAILED"))
          .andDo(document("player-images-sync-global-failure"));
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
              jsonPath("$.message").value("No se pudo iniciar la sincronización de imágenes"))
          .andDo(document("player-images-sync-invalid-configuration"));
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
          .andExpect(jsonPath("$.content[0].items").doesNotExist())
          .andDo(document("player-images-runs"));
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
          .andExpect(jsonPath("$.items").doesNotExist())
          .andDo(document("player-images-run"));
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
          .andExpect(jsonPath("$.totalElements").value(1))
          .andDo(document("player-images-run-items"));
    }

    @Test
    @DisplayName("Una página inválida se rechaza antes de invocar al Service")
    void rejectsInvalidPage() throws Exception {
      mvc.perform(
              get("/api/players/images/sync-runs")
                  .param("size", "101")
                  .header("Authorization", authorization))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.code").value("INVALID_PLAYER_PAGE"))
          .andDo(document("player-images-runs-invalid-page"));
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
          .andExpect(jsonPath("$.code").value("INVALID_PLAYER_PAGE"))
          .andDo(document("player-images-run-items-invalid-page"));
      verifyNoInteractions(audit);
    }

    @Test
    @DisplayName("Un run inexistente responde 404 con el código estable")
    void missingRun() throws Exception {
      when(audit.getRun(999L)).thenThrow(new PlayerImageRunNotFoundException());
      mvc.perform(get("/api/players/images/sync-runs/999").header("Authorization", authorization))
          .andExpect(status().isNotFound())
          .andExpect(jsonPath("$.code").value("PLAYER_IMAGE_SYNC_RUN_NOT_FOUND"))
          .andDo(document("player-images-run-not-found"));
    }

    @Test
    @DisplayName("Los items de un run inexistente responden 404")
    void missingItems() throws Exception {
      when(audit.getItems(999L, 0, 20)).thenThrow(new PlayerImageRunNotFoundException());
      mvc.perform(
              get("/api/players/images/sync-runs/999/items").header("Authorization", authorization))
          .andExpect(status().isNotFound())
          .andExpect(jsonPath("$.code").value("PLAYER_IMAGE_SYNC_RUN_NOT_FOUND"))
          .andDo(document("player-images-run-items-not-found"));
    }

    @Test
    @DisplayName("Sin JWT ninguno de los tres GET consulta la auditoría")
    void requiresJwt() throws Exception {
      mvc.perform(get("/api/players/images/sync-runs"))
          .andExpect(status().isUnauthorized())
          .andExpect(content().string(""))
          .andDo(document("player-images-runs-unauthorized"));
      mvc.perform(get("/api/players/images/sync-runs/43"))
          .andExpect(status().isUnauthorized())
          .andExpect(content().string(""))
          .andDo(document("player-images-run-unauthorized"));
      mvc.perform(get("/api/players/images/sync-runs/43/items"))
          .andExpect(status().isUnauthorized())
          .andExpect(content().string(""))
          .andDo(document("player-images-run-items-unauthorized"));
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
