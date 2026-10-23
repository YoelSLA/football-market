package footballmarket.security;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import footballmarket.config.SecurityConfig;
import footballmarket.models.User;
import footballmarket.models.enums.ExtractionRunStatus;
import footballmarket.models.enums.MetricSource;
import footballmarket.models.enums.UserRole;
import footballmarket.models.records.PlayerStatisticsSyncCounters;
import footballmarket.models.records.PlayerStatisticsSyncSummary;
import footballmarket.services.AuthenticationService;
import footballmarket.services.PlayerStatisticsCoverageService;
import footballmarket.services.PlayerStatisticsSynchronizationService;
import io.jsonwebtoken.Jwts;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Date;
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

/** Autenticación de las rutas de estadísticas con la cadena de filtros real. */
@WebMvcTest(footballmarket.controllers.PlayerStatisticsController.class)
@Import(SecurityConfig.class)
@ActiveProfiles("test")
@org.junit.jupiter.api.Tag("lightweight")
class PlayerStatisticsSecurityIntegrationTest {
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
    when(authenticationService.getCurrentUser("security@example.com"))
        .thenReturn(
            new User(1L, "security.user", "security@example.com", "password123", UserRole.USER));
    when(synchronization.synchronizeScraping(any()))
        .thenReturn(
            new PlayerStatisticsSyncSummary(
                1L,
                MetricSource.SCRAPING,
                LocalDate.of(2026, 10, 15),
                ExtractionRunStatus.COMPLETED,
                Instant.parse("2026-10-15T12:00:00Z"),
                Instant.parse("2026-10-15T12:01:00Z"),
                new PlayerStatisticsSyncCounters(1, 0, 1, 1, 0, 0, 0, 1, 0, 0),
                18,
                true));
    mvc = MockMvcBuilders.webAppContextSetup(context).addFilters(security).build();
    authorization =
        "Bearer "
            + Jwts.builder()
                .subject("security@example.com")
                .expiration(Date.from(Instant.now().plusSeconds(300)))
                .signWith(key)
                .compact();
  }

  @Nested
  @DisplayName("Acceso a las rutas de estadísticas")
  class Access {
    @Test
    @DisplayName("Un lote de scraping sin token no alcanza el caso de uso")
    void scrapingSinToken() throws Exception {
      mvc.perform(post(BASE + "/sync/scraping").contentType("application/json").content("{}"))
          .andExpect(status().isUnauthorized());
      verifyNoInteractions(synchronization);
    }

    @Test
    @DisplayName("La entrada JSON sin token no alcanza el caso de uso")
    void jsonSinToken() throws Exception {
      mvc.perform(post(BASE + "/sync/json")).andExpect(status().isUnauthorized());
      verifyNoInteractions(synchronization);
    }

    @Test
    @DisplayName("La cobertura sin token no se consulta")
    void coberturaSinToken() throws Exception {
      mvc.perform(get(BASE + "/coverage").param("source", "SCRAPING"))
          .andExpect(status().isUnauthorized());
      verifyNoInteractions(coverage);
    }

    @Test
    @DisplayName("Un token expirado se rechaza antes de ejecutar el lote")
    void tokenExpirado() throws Exception {
      String expired =
          "Bearer "
              + Jwts.builder()
                  .subject("security@example.com")
                  .expiration(Date.from(Instant.now().minusSeconds(60)))
                  .signWith(key)
                  .compact();
      mvc.perform(
              post(BASE + "/sync/scraping")
                  .header("Authorization", expired)
                  .contentType("application/json")
                  .content("{}"))
          .andExpect(status().isUnauthorized());
      verifyNoInteractions(synchronization);
    }

    @Test
    @DisplayName("Una firma inválida se rechaza antes de ejecutar el lote")
    void firmaInvalida() throws Exception {
      mvc.perform(
              post(BASE + "/sync/scraping")
                  .header("Authorization", authorization + "invalida")
                  .contentType("application/json")
                  .content("{}"))
          .andExpect(status().isUnauthorized());
      verifyNoInteractions(synchronization);
    }

    @Test
    @DisplayName("Un token vigente de usuario persistido ejecuta el lote sin exigir rol adicional")
    void tokenVigente() throws Exception {
      mvc.perform(
              post(BASE + "/sync/scraping")
                  .header("Authorization", authorization)
                  .contentType("application/json")
                  .content("{\"maxPlayers\":1}"))
          .andExpect(status().isOk());
    }
  }
}
