package footballmarket.controllers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import footballmarket.config.SecurityConfig;
import footballmarket.integrations.exceptions.HeadedBrowserUnavailableException;
import footballmarket.models.User;
import footballmarket.models.enums.ExtractionRunStatus;
import footballmarket.models.enums.MetricSource;
import footballmarket.models.enums.UserRole;
import footballmarket.models.records.PlayerStatisticsCoverage;
import footballmarket.models.records.PlayerStatisticsSyncCounters;
import footballmarket.models.records.PlayerStatisticsSyncSummary;
import footballmarket.services.AuthenticationService;
import footballmarket.services.PlayerStatisticsCoverageService;
import footballmarket.services.PlayerStatisticsSynchronizationService;
import footballmarket.services.exceptions.PlayerStatisticsSyncInProgressException;
import footballmarket.services.exceptions.PlayerStatisticsSynchronizationException;
import io.jsonwebtoken.Jwts;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Date;
import java.util.List;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
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

/** Contrato HTTP de los dos canales de estadísticas y de la cobertura. */
@WebMvcTest(PlayerStatisticsController.class)
@Import(SecurityConfig.class)
@ActiveProfiles("test")
@org.junit.jupiter.api.Tag("lightweight")
class PlayerStatisticsControllerTest {
  private static final String BASE = "/api/players/statistics/whoscored";
  @Autowired private WebApplicationContext context;
  @Autowired private FilterChainProxy security;
  @Autowired private SecretKey key;
  @MockitoBean private PlayerStatisticsSynchronizationService synchronization;
  @MockitoBean private PlayerStatisticsCoverageService coverage;
  @MockitoBean private AuthenticationService authenticationService;
  private MockMvc mvc;
  private String authorization;

  @BeforeEach
  void setUp() {
    when(authenticationService.getCurrentUser("statistics@example.com"))
        .thenReturn(
            new User(
                1L, "statistics.user", "statistics@example.com", "password123", UserRole.USER));
    mvc = MockMvcBuilders.webAppContextSetup(context).addFilters(security).build();
    authorization =
        "Bearer "
            + Jwts.builder()
                .subject("statistics@example.com")
                .expiration(Date.from(Instant.now().plusSeconds(300)))
                .signWith(key)
                .compact();
  }

  private PlayerStatisticsSyncSummary summary(
      MetricSource source, ExtractionRunStatus status, long pending, boolean coverageComplete) {
    return new PlayerStatisticsSyncSummary(
        42L,
        source,
        LocalDate.of(2026, 10, 15),
        status,
        Instant.parse("2026-10-15T12:00:00Z"),
        Instant.parse("2026-10-15T12:29:48Z"),
        new PlayerStatisticsSyncCounters(2100, 35, 105, 102, 1, 2, pending, 12000, 75, 4),
        2040,
        coverageComplete);
  }

  private PlayerStatisticsCoverage coverage(List<String> systemicallyAbsent, long pending) {
    return new PlayerStatisticsCoverage(
        MetricSource.JSON,
        LocalDate.of(2026, 10, 15),
        2135,
        2100,
        35,
        102,
        pending,
        2,
        pending == 0,
        systemicallyAbsent,
        42L,
        Instant.parse("2026-10-15T12:29:48Z"));
  }

  @Nested
  @DisplayName("Lote de scraping")
  class Scraping {
    @Test
    @DisplayName("Un lote sin cuerpo usa los valores por defecto y devuelve el resumen completo")
    void emptyBodyUsesDefaults() throws Exception {
      when(synchronization.synchronizeScraping(any()))
          .thenReturn(summary(MetricSource.SCRAPING, ExtractionRunStatus.PARTIAL, 1998, false));
      mvc.perform(
              post(BASE + "/sync/scraping")
                  .header("Authorization", authorization)
                  .contentType("application/json")
                  .content("{}"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.source").value("SCRAPING"))
          .andExpect(jsonPath("$.status").value("PARTIAL"))
          .andExpect(jsonPath("$.runId").value(42))
          .andExpect(jsonPath("$.players.pending").value(1998))
          .andExpect(jsonPath("$.coverageComplete").value(false));
    }

    @Test
    @DisplayName("El máximo retirado se rechaza incluso si era válido en el contrato anterior")
    void rejectsRetiredLimit() throws Exception {
      mvc.perform(
              post(BASE + "/sync/scraping")
                  .header("Authorization", authorization)
                  .contentType("application/json")
                  .content("{\"maxPlayers\":1}"))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
      verifyNoInteractions(synchronization);
    }

    @Test
    @DisplayName("Una fecha ISO explícita se envía a la fachada sin límite de jugadores")
    void explicitDateAndLimit() throws Exception {
      when(synchronization.synchronizeScraping(any()))
          .thenReturn(summary(MetricSource.SCRAPING, ExtractionRunStatus.COMPLETED, 0, true));
      mvc.perform(
              post(BASE + "/sync/scraping")
                  .header("Authorization", authorization)
                  .contentType("application/json")
                  .content("{\"extractionDate\":\"2026-10-15\"}"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.coverageComplete").value(true));
      org.mockito.ArgumentCaptor<footballmarket.models.records.PlayerStatisticsSyncRequest>
          request =
              org.mockito.ArgumentCaptor.forClass(
                  footballmarket.models.records.PlayerStatisticsSyncRequest.class);
      org.mockito.Mockito.verify(synchronization).synchronizeScraping(request.capture());
      assertThat(request.getValue().extractionDate()).isEqualTo(LocalDate.of(2026, 10, 15));
    }

    @Test
    @DisplayName("Un cuerpo ausente se rechaza con INVALID_REQUEST sin invocar la fachada")
    void missingBody() throws Exception {
      mvc.perform(
              post(BASE + "/sync/scraping")
                  .header("Authorization", authorization)
                  .contentType("application/json"))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
      verifyNoInteractions(synchronization);
    }

    @Test
    @DisplayName("Una fecha no ISO se rechaza con INVALID_STATISTICS_EXTRACTION_DATE")
    void invalidDate() throws Exception {
      mvc.perform(
              post(BASE + "/sync/scraping")
                  .header("Authorization", authorization)
                  .contentType("application/json")
                  .content("{\"extractionDate\":123}"))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.code").value("INVALID_STATISTICS_EXTRACTION_DATE"));
      verifyNoInteractions(synchronization);
    }

    @Test
    @DisplayName("El campo retirado con texto se rechaza con INVALID_REQUEST")
    void invalidMaxPlayers() throws Exception {
      mvc.perform(
              post(BASE + "/sync/scraping")
                  .header("Authorization", authorization)
                  .contentType("application/json")
                  .content("{\"maxPlayers\":\"dos\"}"))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
      verifyNoInteractions(synchronization);
    }

    @Test
    @DisplayName("Un máximo cero se rechaza por validación del contrato")
    void zeroMaxPlayers() throws Exception {
      mvc.perform(
              post(BASE + "/sync/scraping")
                  .header("Authorization", authorization)
                  .contentType("application/json")
                  .content("{\"maxPlayers\":0}"))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
      verifyNoInteractions(synchronization);
    }

    @Test
    @DisplayName("Una ejecución activa del canal responde 409 sin invalidar el lote")
    void syncInProgress() throws Exception {
      when(synchronization.synchronizeScraping(any()))
          .thenThrow(new PlayerStatisticsSyncInProgressException());
      mvc.perform(
              post(BASE + "/sync/scraping")
                  .header("Authorization", authorization)
                  .contentType("application/json")
                  .content("{}"))
          .andExpect(status().isConflict())
          .andExpect(jsonPath("$.code").value("PLAYER_STATISTICS_SYNC_IN_PROGRESS"));
    }

    @Test
    @DisplayName("Un fallo interno antes del resumen responde 500 con código estable")
    void internalFailure() throws Exception {
      when(synchronization.synchronizeScraping(any()))
          .thenThrow(new PlayerStatisticsSynchronizationException());
      mvc.perform(
              post(BASE + "/sync/scraping")
                  .header("Authorization", authorization)
                  .contentType("application/json")
                  .content("{}"))
          .andExpect(status().isInternalServerError())
          .andExpect(jsonPath("$.code").value("PLAYER_STATISTICS_SYNC_FAILED"));
    }

    @Test
    @DisplayName("La ausencia de navegador visible responde 503 con HEADED_BROWSER_UNAVAILABLE")
    void headedBrowserUnavailable() throws Exception {
      when(synchronization.synchronizeScraping(any()))
          .thenThrow(new HeadedBrowserUnavailableException());
      mvc.perform(
              post(BASE + "/sync/scraping")
                  .header("Authorization", authorization)
                  .contentType("application/json")
                  .content("{}"))
          .andExpect(status().isServiceUnavailable())
          .andExpect(jsonPath("$.code").value("HEADED_BROWSER_UNAVAILABLE"));
    }

    @Test
    @DisplayName("Sin JWT el lote de scraping no alcanza la fachada")
    void withoutToken() throws Exception {
      mvc.perform(post(BASE + "/sync/scraping").contentType("application/json").content("{}"))
          .andExpect(status().isUnauthorized());
      verifyNoInteractions(synchronization);
    }
  }

  @Nested
  @DisplayName("Validación del cuerpo de scraping")
  class BodyValidation {
    @ParameterizedTest
    @ValueSource(strings = {"null", "0", "-1", "1.5", "1.0", "\"1\"", "2147483648", "true", "{}"})
    @DisplayName("Rechaza la presencia de maxPlayers, incluido null, antes del Service")
    void rechazaMaximosInvalidos(String maximum) throws Exception {
      // Act / Assert
      mvc.perform(
              post(BASE + "/sync/scraping")
                  .header("Authorization", authorization)
                  .contentType("application/json")
                  .content("{\"maxPlayers\":" + maximum + "}"))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
      verifyNoInteractions(synchronization);
    }

    @ParameterizedTest
    @ValueSource(
        strings = {
          "\"2999-01-01\"",
          "\"not-a-date\"",
          "123",
          "123.5",
          "true",
          "{}",
          "[]",
          "[2026,10,15]",
          "\"2026-02-30\"",
          "\"2026-1-5\"",
          "\"\"",
          "\"2026-10-15T00:00:00Z\""
        })
    @DisplayName("Fecha futura o malformada usa el código específico de extracción")
    void rechazaFechasInvalidas(String date) throws Exception {
      // Act / Assert
      mvc.perform(
              post(BASE + "/sync/scraping")
                  .header("Authorization", authorization)
                  .contentType("application/json")
                  .content("{\"extractionDate\":" + date + "}"))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.code").value("INVALID_STATISTICS_EXTRACTION_DATE"));
      verifyNoInteractions(synchronization);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "{"})
    @DisplayName("Body ausente o JSON malformado produce INVALID_REQUEST")
    void rechazaBodyAusenteOMalformado(String body) throws Exception {
      // Act / Assert
      mvc.perform(
              post(BASE + "/sync/scraping")
                  .header("Authorization", authorization)
                  .contentType("application/json")
                  .content(body))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
      verifyNoInteractions(synchronization);
    }

    @ParameterizedTest
    @ValueSource(
        strings = {"{}", "{\"extractionDate\":null}", "{\"extractionDate\":\"2026-10-15\"}"})
    @DisplayName("Acepta la fecha omitida, null y una cadena ISO de fecha real no futura")
    void aceptaFechaOpcional(String body) throws Exception {
      // Arrange
      when(synchronization.synchronizeScraping(any()))
          .thenReturn(summary(MetricSource.SCRAPING, ExtractionRunStatus.COMPLETED, 0, true));

      // Act / Assert
      mvc.perform(
              post(BASE + "/sync/scraping")
                  .header("Authorization", authorization)
                  .contentType("application/json")
                  .content(body))
          .andExpect(status().isOk());
    }
  }

  @Nested
  @DisplayName("Entrada JSON sin parámetros ni cuerpo")
  class Json {
    @Test
    @DisplayName("Sin entrada la fachada JSON recibe la llamada sin argumentos funcionales")
    void withoutInput() throws Exception {
      when(synchronization.synchronizeJson())
          .thenReturn(summary(MetricSource.JSON, ExtractionRunStatus.COMPLETED, 0, true));
      mvc.perform(post(BASE + "/sync/json").header("Authorization", authorization))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.source").value("JSON"))
          .andExpect(jsonPath("$.players.selected").value(105))
          .andExpect(jsonPath("$.coverageComplete").value(true));
    }

    @Test
    @DisplayName("Una fecha sin pendientes devuelve COMPLETED con selección y llamadas cero")
    void withoutPending() throws Exception {
      PlayerStatisticsSyncSummary empty =
          new PlayerStatisticsSyncSummary(
              43L,
              MetricSource.JSON,
              LocalDate.of(2026, 10, 15),
              ExtractionRunStatus.COMPLETED,
              Instant.parse("2026-10-15T12:00:00Z"),
              Instant.parse("2026-10-15T12:00:03Z"),
              new PlayerStatisticsSyncCounters(2100, 35, 0, 0, 0, 0, 0, 0, 0, 0),
              0,
              true);
      when(synchronization.synchronizeJson()).thenReturn(empty);
      mvc.perform(post(BASE + "/sync/json").header("Authorization", authorization))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.status").value("COMPLETED"))
          .andExpect(jsonPath("$.players.selected").value(0))
          .andExpect(jsonPath("$.providerCalls").value(0));
    }

    @Test
    @DisplayName(
        "Un query parameter no permitido se rechaza con INVALID_REQUEST sin invocar la fachada")
    void rejectsQueryParameter() throws Exception {
      mvc.perform(post(BASE + "/sync/json?source=JSON").header("Authorization", authorization))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
      verifyNoInteractions(synchronization);
    }

    @Test
    @DisplayName("Un body no vacío se rechaza con INVALID_REQUEST sin invocar la fachada")
    void rejectsBody() throws Exception {
      mvc.perform(
              post(BASE + "/sync/json")
                  .header("Authorization", authorization)
                  .contentType("application/json")
                  .content("{}"))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
      verifyNoInteractions(synchronization);
    }
  }

  @Nested
  @DisplayName("Cobertura consultable")
  class Coverage {
    @Test
    @DisplayName("La cobertura informa pendientes, fallidos y ausencia sistémica")
    void coverageContract() throws Exception {
      when(coverage.coverage(any(), any()))
          .thenReturn(coverage(List.of("second_yellow_total"), 1998));
      mvc.perform(
              get(BASE + "/coverage")
                  .header("Authorization", authorization)
                  .param("source", "JSON")
                  .param("extractionDate", "2026-10-15"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.source").value("JSON"))
          .andExpect(jsonPath("$.catalogPlayers").value(2135))
          .andExpect(jsonPath("$.eligiblePlayers").value(2100))
          .andExpect(jsonPath("$.excludedWithoutReference").value(35))
          .andExpect(jsonPath("$.pendingPlayers").value(1998))
          .andExpect(jsonPath("$.failedPlayers").value(2))
          .andExpect(jsonPath("$.coverageComplete").value(false))
          .andExpect(jsonPath("$.systemicallyAbsentMetrics[0]").value("second_yellow_total"))
          .andExpect(jsonPath("$.lastRunId").value(42));
    }

    @Test
    @DisplayName("La ausencia sistémica se devuelve vacía cuando ninguna clave carece de presencia")
    void withoutSystemicAbsence() throws Exception {
      when(coverage.coverage(any(), any())).thenReturn(coverage(List.of(), 0));
      mvc.perform(
              get(BASE + "/coverage")
                  .header("Authorization", authorization)
                  .param("source", "SCRAPING"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.systemicallyAbsentMetrics").isEmpty())
          .andExpect(jsonPath("$.coverageComplete").value(true));
    }

    @Test
    @DisplayName("Un canal ausente se rechaza con INVALID_STATISTICS_SOURCE")
    void missingSource() throws Exception {
      mvc.perform(get(BASE + "/coverage").header("Authorization", authorization))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.code").value("INVALID_STATISTICS_SOURCE"));
    }

    @Test
    @DisplayName("Un canal desconocido se rechaza con INVALID_STATISTICS_SOURCE")
    void invalidSource() throws Exception {
      mvc.perform(
              get(BASE + "/coverage").header("Authorization", authorization).param("source", "CSV"))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.code").value("INVALID_STATISTICS_SOURCE"));
    }

    @Test
    @DisplayName("Una fecha futura se rechaza con INVALID_STATISTICS_EXTRACTION_DATE")
    void futureDate() throws Exception {
      mvc.perform(
              get(BASE + "/coverage")
                  .header("Authorization", authorization)
                  .param("source", "JSON")
                  .param(
                      "extractionDate",
                      LocalDate.now(java.time.ZoneOffset.UTC).plusDays(1).toString()))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.code").value("INVALID_STATISTICS_EXTRACTION_DATE"));
    }

    @Test
    @DisplayName("Una fecha con formato inválido se rechaza con INVALID_STATISTICS_EXTRACTION_DATE")
    void malformedDate() throws Exception {
      mvc.perform(
              get(BASE + "/coverage")
                  .header("Authorization", authorization)
                  .param("source", "JSON")
                  .param("extractionDate", "15-10-2026"))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.code").value("INVALID_STATISTICS_EXTRACTION_DATE"));
    }

    @Test
    @DisplayName("Sin JWT la cobertura no se consulta")
    void withoutToken() throws Exception {
      mvc.perform(get(BASE + "/coverage").param("source", "JSON"))
          .andExpect(status().isUnauthorized());
      verifyNoInteractions(coverage);
    }

    @Test
    @DisplayName("La respuesta de cobertura no expone valores individuales de métricas")
    void coverageOmitsIndividualMetrics() throws Exception {
      when(coverage.coverage(any(), any())).thenReturn(coverage(List.of(), 0));
      mvc.perform(
              get(BASE + "/coverage")
                  .header("Authorization", authorization)
                  .param("source", "JSON"))
          .andExpect(status().isOk())
          .andExpect(
              content()
                  .string(
                      org.hamcrest.Matchers.not(
                          org.hamcrest.Matchers.containsString("\"value\""))));
    }
  }
}
