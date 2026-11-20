package footballmarket.features.statistics.presentation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import footballmarket.features.statistics.application.PlayerStatisticsSynchronizationService;
import footballmarket.features.statistics.application.exceptions.PlayerStatisticsSyncInProgressException;
import footballmarket.features.statistics.application.exceptions.PlayerStatisticsSynchronizationException;
import footballmarket.features.statistics.domain.enums.ExtractionRunStatus;
import footballmarket.features.statistics.domain.enums.MetricSource;
import footballmarket.features.statistics.domain.records.PlayerStatisticsSyncCounters;
import footballmarket.features.statistics.domain.records.PlayerStatisticsSyncSummary;
import footballmarket.integrations.whoscored.exceptions.HeadedBrowserUnavailableException;
import footballmarket.security.PersistedIdentityValidator;
import footballmarket.security.SecurityConfig;
import io.jsonwebtoken.Jwts;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Date;
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

/** Contrato HTTP de los dos canales de estadísticas y ausencia de la consulta retirada. */
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
  @MockitoBean private PersistedIdentityValidator identityValidator;
  private MockMvc mvc;
  private String authorization;

  @BeforeEach
  void setUp() {
    when(identityValidator.exists("statistics@example.com")).thenReturn(true);
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
      org.mockito.ArgumentCaptor<
              footballmarket.features.statistics.domain.records.PlayerStatisticsSyncRequest>
          request =
              org.mockito.ArgumentCaptor.forClass(
                  footballmarket.features.statistics.domain.records.PlayerStatisticsSyncRequest
                      .class);
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
  @DisplayName("Consulta de cobertura retirada")
  class Coverage {
    @ParameterizedTest
    @ValueSource(strings = {"", "?source=JSON", "?source=CSV&extractionDate=invalid"})
    @DisplayName("El GET retirado devuelve 404 sin validar filtros ni iniciar sincronizaciones")
    void removedCoverageHasNoHandler(String query) throws Exception {
      mvc.perform(get(BASE + "/coverage" + query).header("Authorization", authorization))
          .andExpect(status().isNotFound());
      verifyNoInteractions(synchronization);
    }
  }
}
