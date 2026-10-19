package footballmarket.controllers;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import footballmarket.config.SecurityConfig;
import footballmarket.integrations.exceptions.FootballDataUnavailableException;
import footballmarket.models.League;
import footballmarket.models.Player;
import footballmarket.models.Team;
import footballmarket.models.User;
import footballmarket.models.enums.ExternalProvider;
import footballmarket.models.enums.UserRole;
import footballmarket.models.records.PlayerSynchronizationResult;
import footballmarket.orchestrators.PlayerSynchronizationOrchestrator;
import footballmarket.services.AuthenticationService;
import footballmarket.services.PlayerCatalogService;
import footballmarket.services.exceptions.PlayerNotFoundException;
import footballmarket.services.exceptions.PlayerSynchronizationPersistenceException;
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
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@WebMvcTest(PlayerController.class)
@Import(SecurityConfig.class)
@ActiveProfiles("test")
@org.junit.jupiter.api.Tag("lightweight")
class PlayerControllerTest {
  @Autowired private WebApplicationContext context;
  @Autowired private FilterChainProxy security;
  @Autowired private SecretKey key;
  @MockitoBean private PlayerCatalogService service;
  @MockitoBean private AuthenticationService authenticationService;
  @MockitoBean private PlayerSynchronizationOrchestrator orchestrator;
  private MockMvc mvc;
  private String authorization;

  @BeforeEach
  void setUp() {
    when(authenticationService.getCurrentUser("catalog@example.com"))
        .thenReturn(
            new User(1L, "catalog.user", "catalog@example.com", "password123", UserRole.USER));
    mvc = MockMvcBuilders.webAppContextSetup(context).addFilters(security).build();
    authorization =
        "Bearer "
            + Jwts.builder()
                .subject("catalog@example.com")
                .expiration(Date.from(Instant.now().plusSeconds(300)))
                .signWith(key)
                .compact();
  }

  @Nested
  @DisplayName("Sincronización de jugadores")
  class Synchronization {
    @Test
    @DisplayName("Un fallo técnico local devuelve el error seguro sin revelar la base")
    void devuelveErrorDePersistencia() throws Exception {
      when(orchestrator.synchronize()).thenThrow(new PlayerSynchronizationPersistenceException());
      mvc.perform(post("/api/players/sync").header("Authorization", authorization))
          .andExpect(status().isBadGateway())
          .andExpect(jsonPath("$.*").value(hasSize(6)))
          .andExpect(jsonPath("$.status").value(502))
          .andExpect(jsonPath("$.code").value("FOOTBALL_DATA_UNAVAILABLE"))
          .andExpect(
              jsonPath("$.message").value("No se pudo aplicar la sincronización del catálogo"))
          .andExpect(jsonPath("$.path").value("/api/players/sync"));
    }

    @Test
    @DisplayName("Sincroniza con JWT válido y devuelve los contadores contractuales")
    void sincronizaConJwtValidoYDevuelveContadoresExactos() throws Exception {
      PlayerSynchronizationResult result = new PlayerSynchronizationResult(5, 1, 2, 3, 1);
      when(orchestrator.synchronize()).thenReturn(result);
      mvc.perform(post("/api/players/sync").header("Authorization", authorization))
          .andExpect(status().isOk())
          .andExpect(
              content()
                  .json(
                      """
{"obtained":5,"created":1,"updated":2,"markedInactive":3,"discardedInvalid":1}
""",
                      org.springframework.test.json.JsonCompareMode.STRICT));
      verify(orchestrator).synchronize();
    }

    @Test
    @DisplayName("Responde con un error seguro cuando el proveedor falla")
    void devuelveElErrorContractualSeguroAnteFalloDelProveedor() throws Exception {
      when(orchestrator.synchronize()).thenThrow(new FootballDataUnavailableException());
      mvc.perform(post("/api/players/sync").header("Authorization", authorization))
          .andExpect(status().isBadGateway())
          .andExpect(jsonPath("$.*").value(hasSize(6)))
          .andExpect(jsonPath("$.timestamp").exists())
          .andExpect(jsonPath("$.status").value(502))
          .andExpect(jsonPath("$.error").value("Bad Gateway"))
          .andExpect(jsonPath("$.code").value("FOOTBALL_DATA_UNAVAILABLE"))
          .andExpect(jsonPath("$.path").value("/api/players/sync"))
          .andExpect(
              jsonPath("$.message")
                  .value("No se pudo completar la lectura del proveedor de jugadores"));
    }

    @Test
    @DisplayName("Rechaza sincronizar sin un token")
    void impideSincronizarSinAutorizacion() throws Exception {
      mvc.perform(post("/api/players/sync"))
          .andExpect(status().isUnauthorized())
          .andExpect(content().string(""));
      verifyNoInteractions(orchestrator, service);
    }

    @Test
    @DisplayName("Rechaza sincronizar con un JWT inválido")
    void impideSincronizarConJwtInvalido() throws Exception {
      mvc.perform(post("/api/players/sync").header("Authorization", "Bearer invalid"))
          .andExpect(status().isUnauthorized())
          .andExpect(content().string(""));
      verifyNoInteractions(orchestrator, service);
    }
  }

  @Nested
  @DisplayName("Consulta del catálogo")
  class Catalog {
    @Test
    @DisplayName("Serializa opcionales conocidos y no expone referencias externas")
    void devuelveOpcionalesConocidos() throws Exception {
      League league = new League("League");
      Team team = new Team("Team", league, true);
      ReflectionTestUtils.setField(league, "id", 12L);
      ReflectionTestUtils.setField(team, "id", 41L);
      Player player = new Player(" José Pérez ", team, "midfielder");
      ReflectionTestUtils.setField(player, "id", 7L);
      ReflectionTestUtils.setField(player, "imageUrl", "https://images.example/p.jpg");
      ReflectionTestUtils.setField(player, "fallbackImageUrl", "https://images.example/f.jpg");
      player.updateOptionalDetails(LocalDate.of(1990, 6, 20), "Spain");
      player.addExternalReference(ExternalProvider.FOOTBALL_DATA, "44");
      List<Player> players = List.of(player);
      Page<Player> page = new PageImpl<>(players, PageRequest.of(0, 20), 1);
      when(service.getActivePlayers(0, 20)).thenReturn(page);
      mvc.perform(get("/api/players").header("Authorization", authorization))
          .andExpect(status().isOk())
          .andExpect(
              content()
                  .json(
                      """
              {"content":[{"id":7,"name":" José Pérez ","teamId":41,"teamName":"Team","leagueId":12,"leagueName":"League","position":"midfielder",
               "dateOfBirth":"1990-06-20","nationality":"Spain","imageUrl":"https://images.example/p.jpg","fallbackImageUrl":"https://images.example/f.jpg"}],
              "page":0,"size":20,"totalElements":1,"totalPages":1}
              """,
                      org.springframework.test.json.JsonCompareMode.STRICT));
    }

    @Test
    @DisplayName("Devuelve los campos exactos del catálogo y su paginación predeterminada")
    void devuelveCamposExactosYMetadatosPredeterminados() throws Exception {
      League league = new League("League");
      Team team = new Team("Team", league, true);
      ReflectionTestUtils.setField(league, "id", 12L);
      ReflectionTestUtils.setField(team, "id", 41L);
      Player player = new Player("Name", team, "Forward");
      ReflectionTestUtils.setField(player, "id", 7L);
      player.addExternalReference(ExternalProvider.FOOTBALL_DATA, "44");
      List<Player> players = List.of(player);
      Page<Player> page = new PageImpl<>(players, PageRequest.of(0, 20), 1);
      when(service.getActivePlayers(0, 20)).thenReturn(page);
      mvc.perform(get("/api/players").header("Authorization", authorization))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.*").value(hasSize(5)))
          .andExpect(
              content()
                  .json(
                      """
{"content":[{"id":7,"name":"Name","teamId":41,"teamName":"Team","leagueId":12,"leagueName":"League","position":"Forward","dateOfBirth":null,"nationality":null,"imageUrl":null,"fallbackImageUrl":null}],
"page":0,"size":20,"totalElements":1,"totalPages":1}
""",
                      org.springframework.test.json.JsonCompareMode.STRICT));
    }

    @ParameterizedTest
    @DisplayName("Acepta los límites válidos de paginación con resultados vacíos")
    @CsvSource({"0,1", "2,100"})
    void aceptaLimitesDePaginacionYPaginasVacias(int page, int size) throws Exception {
      when(service.getActivePlayers(page, size)).thenReturn(Page.empty(PageRequest.of(page, size)));
      mvc.perform(
              get("/api/players")
                  .param("page", "" + page)
                  .param("size", "" + size)
                  .header("Authorization", authorization))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.*").value(hasSize(5)))
          .andExpect(jsonPath("$.content").isEmpty())
          .andExpect(jsonPath("$.page").value(page))
          .andExpect(jsonPath("$.size").value(size))
          .andExpect(jsonPath("$.totalElements").value(0))
          .andExpect(jsonPath("$.totalPages").value(0));
    }

    @ParameterizedTest
    @DisplayName("Rechaza parámetros de paginación inválidos antes de consultar el catálogo")
    @CsvSource({"-1,20", "0,0", "0,101", "abc,20", "0,2147483648"})
    void rechazaPaginacionInvalida(String page, String size) throws Exception {
      mvc.perform(
              get("/api/players")
                  .param("page", page)
                  .param("size", size)
                  .header("Authorization", authorization))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.*").value(hasSize(6)))
          .andExpect(jsonPath("$.status").value(400))
          .andExpect(jsonPath("$.path").value("/api/players"))
          .andExpect(jsonPath("$.timestamp").exists())
          .andExpect(jsonPath("$.error").value("Bad Request"))
          .andExpect(
              jsonPath("$.code")
                  .value(
                      page.equals("abc") || size.equals("2147483648")
                          ? "INVALID_PARAMETER_TYPE"
                          : "INVALID_PLAYER_PAGE"))
          .andExpect(jsonPath("$.message").isNotEmpty());
      verifyNoInteractions(service);
    }

    @Test
    @DisplayName("Rechaza consultar el catálogo sin token")
    void rechazaJwtAusente() throws Exception {
      mvc.perform(get("/api/players"))
          .andExpect(status().isUnauthorized())
          .andExpect(content().string(""));
      verifyNoInteractions(service);
    }

    @Test
    @DisplayName("Rechaza consultar el catálogo con un JWT inválido")
    void rechazaJwtInvalido() throws Exception {
      mvc.perform(get("/api/players").header("Authorization", "Bearer invalid"))
          .andExpect(status().isUnauthorized())
          .andExpect(content().string(""));
      verifyNoInteractions(service);
    }
  }

  @Nested
  @DisplayName("Consulta de un jugador individual")
  class PlayerDetail {
    @Test
    @DisplayName("Devuelve la identidad exacta de un jugador visible")
    void devuelveJugadorVisible() throws Exception {
      when(service.getVisiblePlayerById(7821L)).thenReturn(visiblePlayer(7821L, "Joel Robles"));
      mvc.perform(get("/api/players/{id}", 7821L).header("Authorization", authorization))
          .andExpect(status().isOk())
          .andExpect(
              content()
                  .json(
                      """
{"id":7821,"name":"Joel Robles","teamId":41,"teamName":"Real Betis Balompié","leagueId":12,"leagueName":"Primera Division","position":"Goalkeeper","dateOfBirth":"1990-06-20","nationality":"Spain","imageUrl":"https://images.example/cutout.png","fallbackImageUrl":"https://images.example/thumb.png"}
""",
                      org.springframework.test.json.JsonCompareMode.STRICT));
    }

    @Test
    @DisplayName("Serializa los opcionales nulos sin inventar valores")
    void devuelveOpcionalesNulos() throws Exception {
      when(service.getVisiblePlayerById(7L))
          .thenReturn(visiblePlayerWithoutOptionals(7L, "Nombre"));
      mvc.perform(get("/api/players/{id}", 7L).header("Authorization", authorization))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.dateOfBirth").value(org.hamcrest.Matchers.nullValue()))
          .andExpect(jsonPath("$.nationality").value(org.hamcrest.Matchers.nullValue()))
          .andExpect(jsonPath("$.imageUrl").value(org.hamcrest.Matchers.nullValue()))
          .andExpect(jsonPath("$.fallbackImageUrl").value(org.hamcrest.Matchers.nullValue()));
    }

    @Test
    @DisplayName("Responde 404 con el mismo cuerpo si el jugador no es visible")
    void responde404SiNoEsVisible() throws Exception {
      when(service.getVisiblePlayerById(999999L))
          .thenThrow(new PlayerNotFoundException("No encontramos el jugador solicitado"));
      mvc.perform(get("/api/players/{id}", 999999L).header("Authorization", authorization))
          .andExpect(status().isNotFound())
          .andExpect(jsonPath("$.*").value(hasSize(6)))
          .andExpect(jsonPath("$.status").value(404))
          .andExpect(jsonPath("$.error").value("Not Found"))
          .andExpect(jsonPath("$.code").value("PLAYER_NOT_FOUND"))
          .andExpect(jsonPath("$.message").value("No encontramos el jugador solicitado"))
          .andExpect(jsonPath("$.path").value("/api/players/999999"))
          .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    @DisplayName("Rechaza un identificador con formato inválido antes de consultar")
    void rechazaIdentificadorInvalido() throws Exception {
      mvc.perform(get("/api/players/{id}", "abc").header("Authorization", authorization))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.*").value(hasSize(6)))
          .andExpect(jsonPath("$.status").value(400))
          .andExpect(jsonPath("$.code").value("INVALID_PARAMETER_TYPE"))
          .andExpect(jsonPath("$.path").value("/api/players/abc"))
          .andExpect(jsonPath("$.timestamp").exists());
      verifyNoInteractions(service);
    }

    @Test
    @DisplayName("Rechaza consultar un jugador sin token")
    void rechazaDetalleSinToken() throws Exception {
      mvc.perform(get("/api/players/{id}", 7821L))
          .andExpect(status().isUnauthorized())
          .andExpect(content().string(""));
      verifyNoInteractions(service);
    }

    @Test
    @DisplayName("Rechaza consultar un jugador con un JWT inválido")
    void rechazaDetalleConJwtInvalido() throws Exception {
      mvc.perform(get("/api/players/{id}", 7821L).header("Authorization", "Bearer invalid"))
          .andExpect(status().isUnauthorized())
          .andExpect(content().string(""));
      verifyNoInteractions(service);
    }

    @Test
    @DisplayName("Documenta el contrato del endpoint individual con Spring REST Docs")
    void documentaContratoDeJugadorVisible() throws Exception {
      when(service.getVisiblePlayerById(7821L)).thenReturn(visiblePlayer(7821L, "Joel Robles"));
      mvc.perform(
              get("/api/players/{id}", 7821L)
                  .header("Authorization", authorization)
                  .accept(MediaType.APPLICATION_JSON))
          .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Documenta el error de jugador no encontrado con Spring REST Docs")
    void documentaJugadorNoEncontrado() throws Exception {
      when(service.getVisiblePlayerById(999999L))
          .thenThrow(new PlayerNotFoundException("No encontramos el jugador solicitado"));
      mvc.perform(get("/api/players/{id}", 999999L).header("Authorization", authorization))
          .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Documenta el identificador inválido con Spring REST Docs")
    void documentaIdentificadorInvalido() throws Exception {
      mvc.perform(get("/api/players/{id}", "abc").header("Authorization", authorization))
          .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Documenta el acceso sin autorización con Spring REST Docs")
    void documentaAccesoNoAutorizado() throws Exception {
      mvc.perform(get("/api/players/{id}", 7821L)).andExpect(status().isUnauthorized());
    }

    private Player visiblePlayer(long id, String name) {
      League league = new League("Primera Division");
      Team team = new Team("Real Betis Balompié", league, true);
      ReflectionTestUtils.setField(league, "id", 12L);
      ReflectionTestUtils.setField(team, "id", 41L);
      Player player = new Player(name, team, "Goalkeeper");
      ReflectionTestUtils.setField(player, "id", id);
      ReflectionTestUtils.setField(player, "imageUrl", "https://images.example/cutout.png");
      ReflectionTestUtils.setField(player, "fallbackImageUrl", "https://images.example/thumb.png");
      player.updateOptionalDetails(LocalDate.of(1990, 6, 20), "Spain");
      return player;
    }

    /** Jugador visible cuyos cuatro opcionales nunca fueron informados por la fuente. */
    private Player visiblePlayerWithoutOptionals(long id, String name) {
      League league = new League("Primera Division");
      Team team = new Team("Real Betis Balompié", league, true);
      ReflectionTestUtils.setField(league, "id", 12L);
      ReflectionTestUtils.setField(team, "id", 41L);
      Player player = new Player(name, team, "Goalkeeper");
      ReflectionTestUtils.setField(player, "id", id);
      return player;
    }
  }
}
