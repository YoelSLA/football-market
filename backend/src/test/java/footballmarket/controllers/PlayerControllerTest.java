package footballmarket.controllers;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.*;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.*;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import footballmarket.config.SecurityConfig;
import footballmarket.integrations.exceptions.FootballDataUnavailableException;
import footballmarket.models.Player;
import footballmarket.models.User;
import footballmarket.models.records.PlayerSynchronizationResult;
import footballmarket.orchestrators.PlayerSynchronizationOrchestrator;
import footballmarket.services.AuthenticationService;
import footballmarket.services.PlayerCatalogService;
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
import org.springframework.security.web.FilterChainProxy;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
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
    void sincronizaConJwtValidoYDevuelveContadoresExactos() throws Exception {
      when(orchestrator.synchronize()).thenReturn(new PlayerSynchronizationResult(5, 1, 2, 3, 1));
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
                      fieldWithPath("created").description("Identificadores nuevos"),
                      fieldWithPath("updated")
                          .description("Identificadores existentes, incluidas reactivaciones"),
                      fieldWithPath("markedInactive").description("Transiciones a inactivo"),
                      fieldWithPath("discardedInvalid")
                          .description("Registros descartados por campos obligatorios"))));
      verify(orchestrator).synchronize();
    }

    @Test
    void devuelveElErrorContractualSeguroAnteFalloDelProveedor() throws Exception {
      when(orchestrator.synchronize()).thenThrow(new FootballDataUnavailableException());
      mvc.perform(post("/api/players/sync").header("Authorization", authorization))
          .andExpect(status().isBadGateway())
          .andExpect(jsonPath("$.*").value(hasSize(5)))
          .andExpect(jsonPath("$.timestamp").exists())
          .andExpect(jsonPath("$.status").value(502))
          .andExpect(jsonPath("$.error").value("Bad Gateway"))
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
                      fieldWithPath("message").description("Mensaje seguro"),
                      fieldWithPath("path").description("Ruta solicitada"))));
    }

    @Test
    void impideSincronizarSinAutorizacion() throws Exception {
      mvc.perform(post("/api/players/sync"))
          .andExpect(status().isUnauthorized())
          .andExpect(content().string(""))
          .andDo(document("players-sync-unauthorized"));
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
    void devuelveCamposExactosYMetadatosPredeterminados() throws Exception {
      when(service.getActivePlayers(0, 20))
          .thenReturn(
              new PageImpl<>(
                  List.of(new Player(7L, "Name", "Team", "League", "Forward")),
                  PageRequest.of(0, 20),
                  1));
      mvc.perform(get("/api/players").header("Authorization", authorization))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.*").value(hasSize(5)))
          .andExpect(
              content()
                  .json(
                      """
{"content":[{"id":7,"name":"Name","team":"Team","league":"League","position":"Forward"}],
"page":0,"size":20,"totalElements":1,"totalPages":1}
""",
                      org.springframework.test.json.JsonCompareMode.STRICT))
          .andDo(
              document(
                  "players-list",
                  responseFields(
                      fieldWithPath("content").description("Jugadores activos de la página"),
                      fieldWithPath("content[].id").description("Identificador externo"),
                      fieldWithPath("content[].name").description("Nombre"),
                      fieldWithPath("content[].team").description("Equipo"),
                      fieldWithPath("content[].league").description("Liga"),
                      fieldWithPath("content[].position").description("Posición"),
                      fieldWithPath("page").description("Página desde cero"),
                      fieldWithPath("size").description("Tamaño solicitado"),
                      fieldWithPath("totalElements").description("Total de activos"),
                      fieldWithPath("totalPages").description("Total de páginas"))));
    }

    @ParameterizedTest
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
    @CsvSource({"-1,20", "0,0", "0,101", "abc,20", "0,2147483648"})
    void rechazaPaginacionInvalida(String page, String size) throws Exception {
      mvc.perform(
              get("/api/players")
                  .param("page", page)
                  .param("size", size)
                  .header("Authorization", authorization))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.*").value(hasSize(5)))
          .andExpect(jsonPath("$.status").value(400))
          .andExpect(jsonPath("$.path").value("/api/players"))
          .andExpect(jsonPath("$.timestamp").exists())
          .andExpect(jsonPath("$.error").value("Bad Request"))
          .andExpect(jsonPath("$.message").isNotEmpty())
          .andDo(
              document(
                  "players-invalid-" + page + "-" + size,
                  responseFields(
                      fieldWithPath("timestamp").description("Fecha del error"),
                      fieldWithPath("status").description("Código HTTP"),
                      fieldWithPath("error").description("Descripción HTTP"),
                      fieldWithPath("message").description("Mensaje de validación"),
                      fieldWithPath("path").description("Ruta solicitada"))));
      verifyNoInteractions(service);
    }

    @Test
    void rechazaJwtAusenteOInvalido() throws Exception {
      mvc.perform(get("/api/players"))
          .andExpect(status().isUnauthorized())
          .andExpect(content().string(""))
          .andDo(document("players-unauthorized"));
      mvc.perform(get("/api/players").header("Authorization", "Bearer invalid"))
          .andExpect(status().isUnauthorized())
          .andExpect(content().string(""))
          .andDo(document("players-invalid-jwt"));
      verifyNoInteractions(service);
    }
  }
}
