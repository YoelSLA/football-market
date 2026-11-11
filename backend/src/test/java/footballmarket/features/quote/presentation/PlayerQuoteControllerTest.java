package footballmarket.features.quote.presentation;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import footballmarket.features.catalog.domain.models.league.League;
import footballmarket.features.catalog.domain.models.player.Player;
import footballmarket.features.catalog.domain.models.team.Team;
import footballmarket.features.quote.application.QuoteQueryService;
import footballmarket.features.quote.application.exceptions.QuoteNotFoundException;
import footballmarket.features.quote.domain.enums.QuoteExecutionOrigin;
import footballmarket.features.quote.domain.enums.QuoteOrigin;
import footballmarket.features.quote.domain.models.PlayerQuote;
import footballmarket.features.quote.domain.models.QuoteExecution;
import footballmarket.features.quote.domain.records.PlayerRankingEntry;
import footballmarket.features.quote.domain.records.QuoteRankingPage;
import footballmarket.features.quote.domain.records.QuoteRankingQuery;
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

@WebMvcTest(PlayerQuoteController.class)
@Import(SecurityConfig.class)
@ActiveProfiles("test")
@org.junit.jupiter.api.Tag("lightweight")
class PlayerQuoteControllerTest {
  @Autowired private WebApplicationContext context;
  @Autowired private FilterChainProxy security;
  @Autowired private SecretKey key;
  @MockitoBean private QuoteQueryService queryService;
  @MockitoBean private PersistedIdentityValidator identityValidator;
  private MockMvc mvc;
  private String authorization;

  @BeforeEach
  void setUp() {
    when(identityValidator.exists("quotes@example.com")).thenReturn(true);
    mvc = MockMvcBuilders.webAppContextSetup(context).addFilters(security).build();
    authorization =
        "Bearer "
            + Jwts.builder()
                .subject("quotes@example.com")
                .expiration(Date.from(Instant.now().plusSeconds(300)))
                .signWith(key)
                .compact();
  }

  @Nested
  @DisplayName("Historial de cotizaciones")
  class History {

    @Test
    @DisplayName("Devuelve el historial del jugador con sus datos de auditoría")
    void returnsHistory() throws Exception {
      when(queryService.findHistory(7821L)).thenReturn(List.of(recomputed(), initial()));

      mvc.perform(get("/api/players/7821/quotes").header("Authorization", authorization))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.playerId").value(7821))
          .andExpect(jsonPath("$.quotes.length()").value(2))
          .andExpect(jsonPath("$.quotes[0].effectiveDate").value("2026-10-20"))
          .andExpect(jsonPath("$.quotes[0].price").value(105))
          .andExpect(jsonPath("$.quotes[0].previousPrice").value(100))
          .andExpect(jsonPath("$.quotes[0].multiplier").value(Matchers.closeTo(1.05, 0.0001)))
          .andExpect(jsonPath("$.quotes[0].score").value(Matchers.closeTo(0.6, 0.0001)))
          .andExpect(jsonPath("$.quotes[0].strategy").value("POSITIONAL_PERFORMANCE"))
          .andExpect(jsonPath("$.quotes[0].strategyVersion").value("v1"))
          .andExpect(jsonPath("$.quotes[0].sourceValuationDate").value("2026-10-19"))
          .andExpect(jsonPath("$.quotes[0].origin").value("RECOMPUTED"))
          .andExpect(jsonPath("$.quotes[0].calculatedAt").value("2026-10-20T18:00:00Z"))
          .andExpect(jsonPath("$.quotes[1].origin").value("INITIAL"))
          .andExpect(jsonPath("$.quotes[1].previousPrice").value(Matchers.nullValue()))
          .andExpect(jsonPath("$.quotes[1].strategy").value(Matchers.nullValue()));
    }

    @Test
    @DisplayName("Devuelve un historial vacío cuando el jugador no fue cotizado")
    void returnsEmptyHistory() throws Exception {
      when(queryService.findHistory(7821L)).thenReturn(List.of());

      mvc.perform(get("/api/players/7821/quotes").header("Authorization", authorization))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.playerId").value(7821))
          .andExpect(jsonPath("$.quotes.length()").value(0));
    }
  }

  @Nested
  @DisplayName("Cotización a una fecha dada")
  class QuoteOnDate {

    @Test
    @DisplayName("Devuelve la cotización vigente a la fecha consultada")
    void returnsQuoteOnDate() throws Exception {
      when(queryService.findOnDate(7821L, LocalDate.of(2026, 10, 20))).thenReturn(recomputed());

      mvc.perform(
              get("/api/players/7821/quotes")
                  .header("Authorization", authorization)
                  .queryParam("date", "2026-10-20"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.effectiveDate").value("2026-10-20"))
          .andExpect(jsonPath("$.price").value(105))
          .andExpect(jsonPath("$.origin").value("RECOMPUTED"))
          .andExpect(jsonPath("$.calculatedAt").value("2026-10-20T18:00:00Z"));
    }

    @Test
    @DisplayName("Traduce la ausencia de cotización a esa fecha")
    void translatesMissingQuote() throws Exception {
      when(queryService.findOnDate(any(), any())).thenThrow(new QuoteNotFoundException());

      mvc.perform(
              get("/api/players/7821/quotes")
                  .header("Authorization", authorization)
                  .queryParam("date", "2026-10-20"))
          .andExpect(status().isNotFound())
          .andExpect(jsonPath("$.code").value("QUOTE_NOT_FOUND"));
    }
  }

  @Nested
  @DisplayName("Cotización vigente")
  class Current {

    @Test
    @DisplayName("Devuelve la cotización vigente con su instante de cálculo de origen")
    void returnsCurrentQuote() throws Exception {
      when(queryService.findCurrent(7821L)).thenReturn(recomputed());

      mvc.perform(get("/api/players/7821/quotes/current").header("Authorization", authorization))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.effectiveDate").value("2026-10-20"))
          .andExpect(jsonPath("$.price").value(105))
          .andExpect(jsonPath("$.origin").value("RECOMPUTED"))
          .andExpect(jsonPath("$.calculatedAt").value("2026-10-20T18:00:00Z"));
    }

    @Test
    @DisplayName("Traduce la ausencia de cotización vigente")
    void translatesMissingCurrentQuote() throws Exception {
      when(queryService.findCurrent(7821L)).thenThrow(new QuoteNotFoundException());

      mvc.perform(get("/api/players/7821/quotes/current").header("Authorization", authorization))
          .andExpect(status().isNotFound())
          .andExpect(jsonPath("$.code").value("QUOTE_NOT_FOUND"));
    }
  }

  @Nested
  @DisplayName("Ranking por precio vigente")
  class Ranking {

    @Test
    @DisplayName("Devuelve los jugadores ordenados por precio con una entrada por posición")
    void returnsRanking() throws Exception {
      when(queryService.findRanking())
          .thenReturn(
              List.of(
                  new PlayerRankingEntry(
                      7821L,
                      105L,
                      LocalDate.of(2026, 10, 20),
                      QuoteOrigin.RECOMPUTED,
                      Instant.parse("2026-10-20T18:00:00Z"),
                      1,
                      null),
                  new PlayerRankingEntry(
                      7822L,
                      100L,
                      LocalDate.of(2026, 10, 20),
                      QuoteOrigin.CARRIED_FORWARD,
                      Instant.parse("2026-10-20T18:00:00Z"),
                      2,
                      null)));

      mvc.perform(get("/api/players/ranking").header("Authorization", authorization))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.ranking.length()").value(2))
          .andExpect(jsonPath("$.ranking[0].position").value(1))
          .andExpect(jsonPath("$.ranking[0].playerId").value(7821))
          .andExpect(jsonPath("$.ranking[0].price").value(105))
          .andExpect(jsonPath("$.ranking[0].effectiveDate").value("2026-10-20"))
          .andExpect(jsonPath("$.ranking[0].origin").value("RECOMPUTED"))
          .andExpect(jsonPath("$.ranking[0].calculatedAt").value("2026-10-20T18:00:00Z"))
          .andExpect(jsonPath("$.ranking[1].position").value(2))
          .andExpect(jsonPath("$.ranking[1].playerId").value(7822))
          .andExpect(jsonPath("$.ranking[1].price").value(100))
          .andExpect(jsonPath("$.ranking[1].origin").value("CARRIED_FORWARD"));
    }

    @Test
    @DisplayName("Devuelve un ranking vacío cuando todavía no hay cotizaciones")
    void returnsEmptyRanking() throws Exception {
      when(queryService.findRanking()).thenReturn(List.of());

      mvc.perform(get("/api/players/ranking").header("Authorization", authorization))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.ranking.length()").value(0));
    }
  }

  @Nested
  @DisplayName("Validación de la consulta y seguridad")
  class ValidationAndSecurity {

    @Test
    @DisplayName("Rechaza un identificador malformado")
    void rejectsMalformedIdentifier() throws Exception {
      mvc.perform(get("/api/players/abc/quotes").header("Authorization", authorization))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.code").value("INVALID_PARAMETER_TYPE"));
      verifyNoInteractions(queryService);
    }

    @Test
    @DisplayName("Rechaza una fecha de consulta malformada")
    void rejectsMalformedDate() throws Exception {
      mvc.perform(
              get("/api/players/7821/quotes")
                  .header("Authorization", authorization)
                  .queryParam("date", "2026-13-45"))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.code").value("INVALID_PARAMETER_TYPE"));
      verifyNoInteractions(queryService);
    }

    @Test
    @DisplayName("Rechaza las cuatro rutas sin un JWT válido antes del caso de uso")
    void rejectsUnauthorizedRequests() throws Exception {
      mvc.perform(get("/api/players/7821/quotes")).andExpect(status().isUnauthorized());
      mvc.perform(get("/api/players/7821/quotes").queryParam("date", "2026-10-20"))
          .andExpect(status().isUnauthorized());
      mvc.perform(get("/api/players/7821/quotes/current")).andExpect(status().isUnauthorized());
      mvc.perform(get("/api/players/ranking")).andExpect(status().isUnauthorized());
      verifyNoInteractions(queryService);
    }
  }

  @Nested
  @DisplayName("Ranking paginado por cotización")
  class PagedRanking {
    @Test
    @DisplayName("Conserva nulos identificatorios y los totales de una página fuera de rango")
    void preservesNullableIdentificationAndOutOfRangeTotals() throws Exception {
      // Arrange
      QuoteRankingPage.PlayerIdentity player =
          new QuoteRankingPage.PlayerIdentity("Jugador", null, null, null, null);
      PlayerRankingEntry entry =
          new PlayerRankingEntry(
              7821L,
              100,
              LocalDate.of(2026, 10, 20),
              QuoteOrigin.CARRIED_FORWARD,
              Instant.parse("2026-10-20T18:00:00Z"),
              1,
              player);
      QuoteRankingPage first = new QuoteRankingPage(List.of(entry), 0, 3, 9, List.of(), List.of());
      QuoteRankingPage outside = new QuoteRankingPage(List.of(), 99, 3, 9, List.of(), List.of());
      when(queryService.findRanking(any(QuoteRankingQuery.class))).thenReturn(first, outside);
      // Act / Assert
      mvc.perform(
              get("/api/rankings/quote")
                  .header("Authorization", authorization)
                  .queryParam("size", "3"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.content[0].player.position").value(Matchers.nullValue()))
          .andExpect(jsonPath("$.content[0].player.team").value(Matchers.nullValue()))
          .andExpect(jsonPath("$.content[0].player.league").value(Matchers.nullValue()));
      mvc.perform(
              get("/api/rankings/quote")
                  .header("Authorization", authorization)
                  .queryParam("size", "3")
                  .queryParam("page", "99"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.content").isEmpty())
          .andExpect(jsonPath("$.page").value(99))
          .andExpect(jsonPath("$.totalElements").value(9))
          .andExpect(jsonPath("$.totalPages").value(3));
      verify(queryService).findRanking(new QuoteRankingQuery(0, 3, null, null, null));
      verify(queryService).findRanking(new QuoteRankingQuery(99, 3, null, null, null));
    }

    @Test
    @DisplayName("Un fallo de persistencia devuelve INTERNAL_ERROR sin filtrar el detalle")
    void hidesTechnicalFailure() throws Exception {
      // Arrange
      when(queryService.findRanking(any(QuoteRankingQuery.class)))
          .thenThrow(new DataAccessResourceFailureException("detalle privado"));
      // Act / Assert
      mvc.perform(get("/api/rankings/quote").header("Authorization", authorization))
          .andExpect(status().isInternalServerError())
          .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"))
          .andExpect(
              jsonPath("$.message", Matchers.not(Matchers.containsString("detalle privado"))));
    }

    @Test
    @DisplayName("Expone posición global e identificación con filtros y parámetros de página")
    void returnsGlobalPositionsAndIdentification() throws Exception {
      // Arrange
      QuoteRankingPage.LeagueOption league = new QuoteRankingPage.LeagueOption(20L, "Liga");
      QuoteRankingPage.TeamIdentity team = new QuoteRankingPage.TeamIdentity(10L, "Equipo", null);
      QuoteRankingPage.PlayerIdentity player =
          new QuoteRankingPage.PlayerIdentity("Jugador", "Offence", null, team, league);
      PlayerRankingEntry entry =
          new PlayerRankingEntry(
              7821L,
              105,
              LocalDate.of(2026, 10, 20),
              QuoteOrigin.RECOMPUTED,
              Instant.parse("2026-10-20T18:00:00Z"),
              7,
              player);
      QuoteRankingPage page =
          new QuoteRankingPage(List.of(entry), 1, 8, 9, List.of(league), List.of("Offence"));
      when(queryService.findRanking(any(QuoteRankingQuery.class))).thenReturn(page);
      // Act / Assert
      mvc.perform(
              get("/api/rankings/quote")
                  .header("Authorization", authorization)
                  .queryParam("page", "1")
                  .queryParam("size", "8")
                  .queryParam("name", " Jugador ")
                  .queryParam("leagueId", "20")
                  .queryParam("position", "Offence"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.content[0].position").value(7))
          .andExpect(jsonPath("$.content[0].playerId").value(7821))
          .andExpect(jsonPath("$.content[0].price").value(105))
          .andExpect(jsonPath("$.content[0].effectiveDate").value("2026-10-20"))
          .andExpect(jsonPath("$.content[0].origin").value("RECOMPUTED"))
          .andExpect(jsonPath("$.content[0].calculatedAt").value("2026-10-20T18:00:00Z"))
          .andExpect(jsonPath("$.content[0].player.name").value("Jugador"))
          .andExpect(jsonPath("$.content[0].player.position").value("Offence"))
          .andExpect(jsonPath("$.content[0].player.imageUrl").value(Matchers.nullValue()))
          .andExpect(jsonPath("$.content[0].player.team.id").value(10))
          .andExpect(jsonPath("$.content[0].player.team.name").value("Equipo"))
          .andExpect(jsonPath("$.content[0].player.team.crestUrl").value(Matchers.nullValue()))
          .andExpect(jsonPath("$.content[0].player.league.id").value(20))
          .andExpect(jsonPath("$.content[0].player.league.name").value("Liga"))
          .andExpect(jsonPath("$.page").value(1))
          .andExpect(jsonPath("$.size").value(8))
          .andExpect(jsonPath("$.totalElements").value(9))
          .andExpect(jsonPath("$.totalPages").value(2))
          .andExpect(jsonPath("$.filterOptions.leagues[0].id").value(20))
          .andExpect(jsonPath("$.filterOptions.positions[0]").value("Offence"));
      verify(queryService).findRanking(new QuoteRankingQuery(1, 8, "Jugador", 20L, "Offence"));
    }

    @Test
    @DisplayName("Aplica defaults y devuelve página vacía sin inventar totales")
    void defaultsAndEmptyPage() throws Exception {
      // Arrange
      QuoteRankingPage empty = new QuoteRankingPage(List.of(), 0, 20, 0, List.of(), List.of());
      when(queryService.findRanking(any(QuoteRankingQuery.class))).thenReturn(empty);
      // Act / Assert
      mvc.perform(get("/api/rankings/quote").header("Authorization", authorization))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.content").isEmpty())
          .andExpect(jsonPath("$.page").value(0))
          .andExpect(jsonPath("$.size").value(20))
          .andExpect(jsonPath("$.totalElements").value(0))
          .andExpect(jsonPath("$.totalPages").value(0))
          .andExpect(jsonPath("$.filterOptions.leagues").isEmpty());
      verify(queryService).findRanking(new QuoteRankingQuery(0, 20, null, null, null));
    }

    @Test
    @DisplayName("Rechaza límites inválidos antes de invocar el caso de uso")
    void rejectsInvalidPaginationAndFilters() throws Exception {
      // Act / Assert
      for (String[] invalid :
          List.of(
              new String[] {"page", "-1"},
              new String[] {"size", "0"},
              new String[] {"size", "101"},
              new String[] {"leagueId", "0"},
              new String[] {"name", "x".repeat(256)},
              new String[] {"position", "x".repeat(256)})) {
        mvc.perform(
                get("/api/rankings/quote")
                    .header("Authorization", authorization)
                    .queryParam(invalid[0], invalid[1]))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
      }
      verifyNoInteractions(queryService);
    }

    @Test
    @DisplayName("El ranking paginado exige JWT y no invoca el servicio sin él")
    void requiresJwt() throws Exception {
      // Act / Assert
      mvc.perform(get("/api/rankings/quote")).andExpect(status().isUnauthorized());
      mvc.perform(get("/api/rankings/quote").header("Authorization", "Bearer invalid"))
          .andExpect(status().isUnauthorized());
      verifyNoInteractions(queryService);
    }
  }

  private PlayerQuote recomputed() {
    return new PlayerQuote(
        player(),
        LocalDate.of(2026, 10, 20),
        105L,
        100L,
        new BigDecimal("1.050000"),
        new BigDecimal("0.6"),
        ValuationStrategy.POSITIONAL_PERFORMANCE,
        "v1",
        LocalDate.of(2026, 10, 19),
        QuoteOrigin.RECOMPUTED,
        Instant.parse("2026-10-20T18:00:00Z"),
        new QuoteExecution(
            LocalDate.of(2026, 10, 20),
            QuoteExecutionOrigin.MANUAL,
            ValuationStrategy.POSITIONAL_PERFORMANCE,
            "v1",
            new BigDecimal("0.10"),
            new BigDecimal("0.10"),
            1L,
            1,
            1,
            0,
            0,
            0,
            Instant.parse("2026-10-20T18:00:00Z"),
            Instant.parse("2026-10-20T18:00:01Z")));
  }

  private PlayerQuote initial() {
    return new PlayerQuote(
        player(),
        LocalDate.of(2026, 10, 17),
        100L,
        null,
        BigDecimal.ONE,
        null,
        null,
        null,
        null,
        QuoteOrigin.INITIAL,
        Instant.parse("2026-10-17T18:00:00Z"));
  }

  private Player player() {
    League league = new League("League 1");
    Team team = new Team("Team 1", league, true);
    return new Player("Player 1", team, "Forward");
  }
}
