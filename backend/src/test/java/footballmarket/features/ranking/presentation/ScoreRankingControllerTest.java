package footballmarket.features.ranking.presentation;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import footballmarket.features.ranking.application.ScoreRankingQueryService;
import footballmarket.features.ranking.domain.records.ScoreRankingFilterOptions;
import footballmarket.features.ranking.domain.records.ScoreRankingPage;
import footballmarket.features.ranking.domain.records.ScoreRankingQuery;
import footballmarket.features.valuation.domain.enums.ValuationStrategy;
import footballmarket.security.PersistedIdentityValidator;
import footballmarket.security.SecurityConfig;
import io.jsonwebtoken.Jwts;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Date;
import java.util.List;
import javax.crypto.SecretKey;
import org.hamcrest.Matchers;
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

@WebMvcTest(ScoreRankingController.class)
@Import(SecurityConfig.class)
@ActiveProfiles("test")
@org.junit.jupiter.api.Tag("lightweight")
class ScoreRankingControllerTest {
  @Autowired private WebApplicationContext context;
  @Autowired private FilterChainProxy security;
  @Autowired private SecretKey key;
  @MockitoBean private ScoreRankingQueryService queryService;
  @MockitoBean private PersistedIdentityValidator identityValidator;
  private MockMvc mvc;
  private String authorization;

  @BeforeEach
  void setUp() {
    when(identityValidator.exists("ranking@example.com")).thenReturn(true);
    mvc = MockMvcBuilders.webAppContextSetup(context).addFilters(security).build();
    authorization =
        "Bearer "
            + Jwts.builder()
                .subject("ranking@example.com")
                .expiration(Date.from(Instant.now().plusSeconds(300)))
                .signWith(key)
                .compact();
  }

  @Nested
  @DisplayName("Contrato paginado del ranking por score")
  class Contract {
    @Test
    @DisplayName("Expone la identidad, el score y la posición global de la página")
    void returnsPageWithPlayer() throws Exception {
      // Arrange
      ScoreRankingFilterOptions.League league = new ScoreRankingFilterOptions.League(4L, "Liga");
      ScoreRankingPage.Team team = new ScoreRankingPage.Team(31L, "Equipo", null);
      ScoreRankingPage.Player player =
          new ScoreRankingPage.Player("Jugador", "Forward", null, team, league);
      ScoreRankingPage.Entry entry =
          new ScoreRankingPage.Entry(
              7, 7821L, new BigDecimal("0.913400"), LocalDate.of(2026, 11, 10), player);
      ScoreRankingFilterOptions options =
          new ScoreRankingFilterOptions(List.of(league), List.of("Forward"));
      ScoreRankingPage result =
          new ScoreRankingPage(
              ValuationStrategy.POSITIONAL_PERFORMANCE,
              LocalDate.of(2026, 11, 12),
              List.of(entry),
              0,
              20,
              12,
              options);
      when(queryService.findScoreRanking(any())).thenReturn(result);

      // Act / Assert
      mvc.perform(
              get("/api/rankings/score")
                  .param("name", "jug")
                  .header("Authorization", authorization))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.*").value(Matchers.hasSize(8)))
          .andExpect(jsonPath("$.content[0].*").value(Matchers.hasSize(5)))
          .andExpect(jsonPath("$.content[0].position").value(7))
          .andExpect(jsonPath("$.content[0].playerId").value(7821))
          .andExpect(jsonPath("$.content[0].score").value(Matchers.closeTo(0.9134, 0.000001)))
          .andExpect(jsonPath("$.content[0].valuationDate").value("2026-11-10"))
          .andExpect(jsonPath("$.content[0].player.*").value(Matchers.hasSize(5)))
          .andExpect(jsonPath("$.content[0].player.name").value("Jugador"))
          .andExpect(jsonPath("$.content[0].player.position").value("Forward"))
          .andExpect(jsonPath("$.content[0].player.imageUrl").value(Matchers.nullValue()))
          .andExpect(jsonPath("$.content[0].player.team.*").value(Matchers.hasSize(3)))
          .andExpect(jsonPath("$.content[0].player.team.id").value(31))
          .andExpect(jsonPath("$.content[0].player.team.name").value("Equipo"))
          .andExpect(jsonPath("$.content[0].player.team.crestUrl").value(Matchers.nullValue()))
          .andExpect(jsonPath("$.content[0].player.league.*").value(Matchers.hasSize(2)))
          .andExpect(jsonPath("$.content[0].player.league.id").value(4))
          .andExpect(jsonPath("$.content[0].player.league.name").value("Liga"))
          .andExpect(jsonPath("$.strategy").value("POSITIONAL_PERFORMANCE"))
          .andExpect(jsonPath("$.quoteDate").value("2026-11-12"))
          .andExpect(jsonPath("$.page").value(0))
          .andExpect(jsonPath("$.size").value(20))
          .andExpect(jsonPath("$.totalElements").value(12))
          .andExpect(jsonPath("$.totalPages").value(1))
          .andExpect(jsonPath("$.filterOptions.*").value(Matchers.hasSize(2)))
          .andExpect(jsonPath("$.filterOptions.leagues[0].id").value(4))
          .andExpect(jsonPath("$.filterOptions.leagues[0].name").value("Liga"))
          .andExpect(jsonPath("$.filterOptions.positions[0]").value("Forward"));
    }

    @Test
    @DisplayName("Devuelve la página vacía con encabezado y opciones presentes")
    void returnsEmptyPage() throws Exception {
      // Arrange
      ScoreRankingFilterOptions options = new ScoreRankingFilterOptions(List.of(), List.of());
      ScoreRankingPage result = new ScoreRankingPage(null, null, List.of(), 0, 20, 0, options);
      when(queryService.findScoreRanking(any())).thenReturn(result);

      // Act / Assert
      mvc.perform(get("/api/rankings/score").header("Authorization", authorization))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.*").value(Matchers.hasSize(8)))
          .andExpect(jsonPath("$.strategy").value(Matchers.nullValue()))
          .andExpect(jsonPath("$.quoteDate").value(Matchers.nullValue()))
          .andExpect(jsonPath("$.content").isEmpty())
          .andExpect(jsonPath("$.page").value(0))
          .andExpect(jsonPath("$.size").value(20))
          .andExpect(jsonPath("$.totalElements").value(0))
          .andExpect(jsonPath("$.totalPages").value(0))
          .andExpect(jsonPath("$.filterOptions.leagues").isEmpty())
          .andExpect(jsonPath("$.filterOptions.positions").isEmpty());
      verify(queryService).findScoreRanking(new ScoreRankingQuery(0, 20, null, null, null));
    }

    @Test
    @DisplayName("Una página fuera de rango conserva el encabezado y los totales")
    void returnsOutsidePageWithTotals() throws Exception {
      // Arrange
      ScoreRankingFilterOptions.League league = new ScoreRankingFilterOptions.League(4L, "Liga");
      ScoreRankingFilterOptions options =
          new ScoreRankingFilterOptions(List.of(league), List.of("Forward"));
      ScoreRankingPage result =
          new ScoreRankingPage(
              ValuationStrategy.GENERAL_ATTRIBUTES,
              LocalDate.of(2026, 11, 12),
              List.of(),
              3,
              1,
              2,
              options);
      when(queryService.findScoreRanking(any())).thenReturn(result);

      // Act / Assert
      mvc.perform(
              get("/api/rankings/score")
                  .param("page", "3")
                  .param("size", "1")
                  .header("Authorization", authorization))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.strategy").value("GENERAL_ATTRIBUTES"))
          .andExpect(jsonPath("$.quoteDate").value("2026-11-12"))
          .andExpect(jsonPath("$.content").isEmpty())
          .andExpect(jsonPath("$.page").value(3))
          .andExpect(jsonPath("$.size").value(1))
          .andExpect(jsonPath("$.totalElements").value(2))
          .andExpect(jsonPath("$.totalPages").value(2))
          .andExpect(jsonPath("$.filterOptions.leagues[0].id").value(4));
    }

    @Test
    @DisplayName("Un filtro sin coincidencias conserva las opciones del universo global")
    void returnsEmptyFilteredPageWithOptions() throws Exception {
      // Arrange
      ScoreRankingFilterOptions.League league = new ScoreRankingFilterOptions.League(4L, "Liga");
      ScoreRankingFilterOptions options =
          new ScoreRankingFilterOptions(List.of(league), List.of("Forward"));
      ScoreRankingPage result =
          new ScoreRankingPage(
              ValuationStrategy.POSITIONAL_PERFORMANCE,
              LocalDate.of(2026, 11, 12),
              List.of(),
              0,
              20,
              0,
              options);
      when(queryService.findScoreRanking(any())).thenReturn(result);

      // Act / Assert
      mvc.perform(
              get("/api/rankings/score")
                  .param("name", "inexistente")
                  .header("Authorization", authorization))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.content").isEmpty())
          .andExpect(jsonPath("$.totalElements").value(0))
          .andExpect(jsonPath("$.totalPages").value(0))
          .andExpect(jsonPath("$.filterOptions.leagues[0].id").value(4))
          .andExpect(jsonPath("$.filterOptions.positions[0]").value("Forward"));
      verify(queryService)
          .findScoreRanking(new ScoreRankingQuery(0, 20, "inexistente", null, null));
    }

    @Test
    @DisplayName("Transfiere página, tamaño y filtros documentados al servicio")
    void forwardsDocumentedParameters() throws Exception {
      // Arrange
      ScoreRankingFilterOptions options = new ScoreRankingFilterOptions(List.of(), List.of());
      ScoreRankingPage result = new ScoreRankingPage(null, null, List.of(), 1, 5, 0, options);
      when(queryService.findScoreRanking(any())).thenReturn(result);

      // Act / Assert
      mvc.perform(
              get("/api/rankings/score")
                  .param("page", "1")
                  .param("size", "5")
                  .param("name", "  Ana  ")
                  .param("leagueId", "4")
                  .param("position", " Forward ")
                  .header("Authorization", authorization))
          .andExpect(status().isOk());
      verify(queryService).findScoreRanking(new ScoreRankingQuery(1, 5, "Ana", 4L, "Forward"));
    }

    @Test
    @DisplayName("Rechaza una página negativa antes de invocar el servicio")
    void rejectsNegativePage() throws Exception {
      mvc.perform(
              get("/api/rankings/score").param("page", "-1").header("Authorization", authorization))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
      verifyNoInteractions(queryService);
    }

    @Test
    @DisplayName("Rechaza tamaños fuera del rango permitido")
    void rejectsInvalidSizes() throws Exception {
      mvc.perform(
              get("/api/rankings/score").param("size", "0").header("Authorization", authorization))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
      mvc.perform(
              get("/api/rankings/score")
                  .param("size", "101")
                  .header("Authorization", authorization))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
      verifyNoInteractions(queryService);
    }

    @Test
    @DisplayName("Rechaza la consulta sin JWT")
    void rejectsMissingJwt() throws Exception {
      mvc.perform(get("/api/rankings/score")).andExpect(status().isUnauthorized());
      verifyNoInteractions(queryService);
    }

    @Test
    @DisplayName("Rechaza un JWT inválido")
    void rejectsInvalidJwt() throws Exception {
      mvc.perform(get("/api/rankings/score").header("Authorization", "Bearer invalid.token"))
          .andExpect(status().isUnauthorized());
      verifyNoInteractions(queryService);
    }

    @Test
    @DisplayName("No devuelve contenido parcial ante un fallo de persistencia")
    void returnsInternalError() throws Exception {
      when(queryService.findScoreRanking(any()))
          .thenThrow(new DataAccessResourceFailureException("database unavailable"));
      mvc.perform(get("/api/rankings/score").header("Authorization", authorization))
          .andExpect(status().isInternalServerError())
          .andExpect(jsonPath("$.*").value(Matchers.hasSize(6)))
          .andExpect(jsonPath("$.status").value(500))
          .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"))
          .andExpect(jsonPath("$.path").value("/api/rankings/score"))
          .andExpect(
              jsonPath("$.message")
                  .value(Matchers.not(Matchers.containsString("database unavailable"))))
          .andExpect(jsonPath("$.content").doesNotExist());
    }
  }
}
