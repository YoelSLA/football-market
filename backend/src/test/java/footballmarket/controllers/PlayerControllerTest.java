package footballmarket.controllers;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.*;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.*;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import footballmarket.config.SecurityConfig;
import footballmarket.integrations.exceptions.FootballDataUnavailableException;
import footballmarket.models.League;
import footballmarket.models.Player;
import footballmarket.models.Team;
import footballmarket.models.User;
import footballmarket.models.enums.ExternalProvider;
import footballmarket.models.records.PlayerSynchronizationResult;
import footballmarket.orchestrators.PlayerSynchronizationOrchestrator;
import footballmarket.services.AuthenticationService;
import footballmarket.services.PlayerCatalogService;
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
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.restdocs.RestDocumentationContextProvider;
import org.springframework.restdocs.RestDocumentationExtension;
import org.springframework.restdocs.payload.JsonFieldType;
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
@ExtendWith(RestDocumentationExtension.class)
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
  void setUp(RestDocumentationContextProvider documentation) {
    when(authenticationService.getCurrentUser("catalog@example.com"))
        .thenReturn(new User(1L, "catalog@example.com", "password123"));
    mvc =
        MockMvcBuilders.webAppContextSetup(context)
            .addFilters(security)
            .apply(documentationConfiguration(documentation))
            .build();
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
          .andExpect(jsonPath("$.path").value("/api/players/sync"))
          .andDo(
              document(
                  "players-sync-persistence-error",
                  responseFields(
                      fieldWithPath("timestamp").description("Fecha del error"),
                      fieldWithPath("status").description("Código HTTP"),
                      fieldWithPath("error").description("Descripción HTTP"),
                      fieldWithPath("code").description("Código estable del error"),
                      fieldWithPath("message").description("Mensaje seguro"),
                      fieldWithPath("path").description("Ruta solicitada"))));
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
                      org.springframework.test.json.JsonCompareMode.STRICT))
          .andDo(
              document(
                  "players-sync",
                  responseFields(
                      fieldWithPath("obtained")
                          .description("Registros leídos antes de validar y consolidar"),
                      fieldWithPath("created")
                          .description("Jugadores nuevos con su referencia externa"),
                      fieldWithPath("updated")
                          .description(
                              "Jugadores resueltos por referencia, incluidas reactivaciones"),
                      fieldWithPath("markedInactive").description("Transiciones a inactivo"),
                      fieldWithPath("discardedInvalid")
                          .description(
                              "Registros descartados por obligatorios o conflictos de identidad"))));
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
                  .value("No se pudo completar la lectura del proveedor de jugadores"))
          .andDo(
              document(
                  "players-sync-unavailable",
                  responseFields(
                      fieldWithPath("timestamp").description("Fecha del error"),
                      fieldWithPath("status").description("Código HTTP"),
                      fieldWithPath("error").description("Descripción HTTP"),
                      fieldWithPath("code").description("Código estable del error"),
                      fieldWithPath("message").description("Mensaje seguro"),
                      fieldWithPath("path").description("Ruta solicitada"))));
    }

    @Test
    @DisplayName("Rechaza sincronizar sin un token")
    void impideSincronizarSinAutorizacion() throws Exception {
      mvc.perform(post("/api/players/sync"))
          .andExpect(status().isUnauthorized())
          .andExpect(content().string(""))
          .andDo(document("players-sync-unauthorized"));
      verifyNoInteractions(orchestrator, service);
    }

    @Test
    @DisplayName("Rechaza sincronizar con un JWT inválido")
    void impideSincronizarConJwtInvalido() throws Exception {
      mvc.perform(post("/api/players/sync").header("Authorization", "Bearer invalid"))
          .andExpect(status().isUnauthorized())
          .andExpect(content().string(""))
          .andDo(document("players-sync-invalid-jwt"));
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
                      org.springframework.test.json.JsonCompareMode.STRICT))
          .andDo(
              document(
                  "players-list-known-optionals",
                  responseFields(
                      fieldWithPath("content").description("Jugadores activos"),
                      fieldWithPath("content[].id").description("ID interno"),
                      fieldWithPath("content[].name").description("Nombre"),
                      fieldWithPath("content[].teamId").description("ID interno del equipo"),
                      fieldWithPath("content[].teamName").description("Nombre actual del equipo"),
                      fieldWithPath("content[].leagueId")
                          .description("ID interno de la liga del equipo"),
                      fieldWithPath("content[].leagueName")
                          .description("Nombre actual de la liga del equipo"),
                      fieldWithPath("content[].position").description("Posición"),
                      fieldWithPath("content[].dateOfBirth").description("Fecha de nacimiento"),
                      fieldWithPath("content[].nationality").description("Nacionalidad"),
                      fieldWithPath("content[].imageUrl")
                          .description("Imagen conservada sin enriquecimiento"),
                      fieldWithPath("content[].fallbackImageUrl")
                          .description("Imagen alternativa conservada localmente"),
                      fieldWithPath("page").description("Página"),
                      fieldWithPath("size").description("Tamaño"),
                      fieldWithPath("totalElements").description("Total"),
                      fieldWithPath("totalPages").description("Páginas"))));
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
                      org.springframework.test.json.JsonCompareMode.STRICT))
          .andDo(
              document(
                  "players-list",
                  responseFields(
                      fieldWithPath("content").description("Jugadores activos de la página"),
                      fieldWithPath("content[].id")
                          .description("Identificador interno de FootballMarket"),
                      fieldWithPath("content[].name").description("Nombre"),
                      fieldWithPath("content[].teamId").description("ID interno del equipo"),
                      fieldWithPath("content[].teamName").description("Nombre actual del equipo"),
                      fieldWithPath("content[].leagueId")
                          .description("ID interno de la liga del equipo"),
                      fieldWithPath("content[].leagueName")
                          .description("Nombre actual de la liga del equipo"),
                      fieldWithPath("content[].position").description("Posición"),
                      fieldWithPath("content[].dateOfBirth")
                          .type(JsonFieldType.STRING)
                          .optional()
                          .description("Fecha de nacimiento o null"),
                      fieldWithPath("content[].nationality")
                          .type(JsonFieldType.STRING)
                          .optional()
                          .description("Nacionalidad o null"),
                      fieldWithPath("content[].imageUrl")
                          .type(JsonFieldType.STRING)
                          .optional()
                          .description("Imagen principal o null"),
                      fieldWithPath("content[].fallbackImageUrl")
                          .type(JsonFieldType.STRING)
                          .optional()
                          .description("Imagen alternativa o null"),
                      fieldWithPath("page").description("Página desde cero"),
                      fieldWithPath("size").description("Tamaño solicitado"),
                      fieldWithPath("totalElements").description("Total de activos"),
                      fieldWithPath("totalPages").description("Total de páginas"))));
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
          .andExpect(jsonPath("$.totalPages").value(0))
          .andDo(
              document(
                  "players-empty-" + size,
                  responseFields(
                      fieldWithPath("content").description("Página vacía de jugadores activos"),
                      fieldWithPath("page").description("Página solicitada"),
                      fieldWithPath("size").description("Tamaño solicitado"),
                      fieldWithPath("totalElements").description("Total de jugadores activos"),
                      fieldWithPath("totalPages").description("Total de páginas"))));
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
          .andExpect(jsonPath("$.message").isNotEmpty())
          .andDo(
              document(
                  "players-invalid-" + page + "-" + size,
                  responseFields(
                      fieldWithPath("timestamp").description("Fecha del error"),
                      fieldWithPath("status").description("Código HTTP"),
                      fieldWithPath("error").description("Descripción HTTP"),
                      fieldWithPath("code").description("Código estable del error"),
                      fieldWithPath("message").description("Mensaje de validación"),
                      fieldWithPath("path").description("Ruta solicitada"))));
      verifyNoInteractions(service);
    }

    @Test
    @DisplayName("Rechaza consultar el catálogo sin token")
    void rechazaJwtAusente() throws Exception {
      mvc.perform(get("/api/players"))
          .andExpect(status().isUnauthorized())
          .andExpect(content().string(""))
          .andDo(document("players-unauthorized"));
      verifyNoInteractions(service);
    }

    @Test
    @DisplayName("Rechaza consultar el catálogo con un JWT inválido")
    void rechazaJwtInvalido() throws Exception {
      mvc.perform(get("/api/players").header("Authorization", "Bearer invalid"))
          .andExpect(status().isUnauthorized())
          .andExpect(content().string(""))
          .andDo(document("players-invalid-jwt"));
      verifyNoInteractions(service);
    }
  }
}
