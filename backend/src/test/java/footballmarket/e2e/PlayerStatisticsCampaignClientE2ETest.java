package footballmarket.e2e;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import footballmarket.config.WhoScoredProperties;
import footballmarket.controllers.dtos.requests.LoginRequestDTO;
import footballmarket.controllers.dtos.requests.RegisterRequestDTO;
import footballmarket.controllers.dtos.responses.LoginResponseDTO;
import footballmarket.controllers.dtos.responses.PlayerIdentitySyncRunResponseDTO;
import footballmarket.controllers.dtos.responses.PlayerResponseDTO;
import footballmarket.controllers.dtos.responses.PlayersPageResponseDTO;
import footballmarket.integrations.WhoScoredStatisticsScrapingIntegration;
import footballmarket.integrations.records.WhoScoredScrapedContextRow;
import footballmarket.integrations.records.WhoScoredScrapedMetricCell;
import footballmarket.integrations.records.WhoScoredScrapedPlayer;
import footballmarket.integrations.records.WhoScoredStructuredDataBlock;
import footballmarket.support.IdentityControlledProvider;
import footballmarket.support.ResetPlayerCatalogListener;
import footballmarket.support.TestcontainersConfiguration;
import footballmarket.tools.PlayerStatisticsSyncClient;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.bean.override.convention.TestBean;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.web.client.RestClient;

/**
 * Cliente operativo de scraping sobre HTTP real contra la aplicación y proveedores controlados.
 *
 * <p>El token viaja solo en la cabecera y el cliente se invoca con un entorno controlado; ninguna
 * ruta imprime ni acepta el JWT.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@TestExecutionListeners(
    listeners = ResetPlayerCatalogListener.class,
    mergeMode = TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS)
@org.junit.jupiter.api.Tag("e2e")
class PlayerStatisticsCampaignClientE2ETest {
  private static final IdentityControlledProvider PROVIDER = startProvider();
  private static final String BASE_URL = "http://localhost";
  @LocalServerPort private int port;
  @MockitoSpyBean private WhoScoredProperties properties;
  @MockitoBean private WhoScoredStatisticsScrapingIntegration integration;

  @org.springframework.beans.factory.annotation.Autowired
  private footballmarket.repositories.PlayerExternalReferenceRepository references;

  @org.springframework.beans.factory.annotation.Autowired
  private footballmarket.repositories.PlayerRepository players;

  @TestBean(name = "whoScoredRestClient", methodName = "providerClient")
  private RestClient whoScored;

  @TestBean(name = "footballDataRestClient", methodName = "providerClient")
  private RestClient footballData;

  @TestBean(name = "theSportsDbRestClient", methodName = "providerClient")
  private RestClient sportsDb;

  private static IdentityControlledProvider startProvider() {
    try {
      return new IdentityControlledProvider();
    } catch (java.io.IOException failure) {
      throw new ExceptionInInitializerError(failure);
    }
  }

  static RestClient providerClient() {
    return PROVIDER.client();
  }

  @AfterAll
  static void stopProvider() {
    PROVIDER.close();
  }

  private String baseUrl() {
    return BASE_URL + ":" + port;
  }

  private PlayerStatisticsSyncClient client() {
    return new PlayerStatisticsSyncClient();
  }

  private String authenticatedToken() {
    RestClient api = RestClient.builder().baseUrl(baseUrl()).build();
    RegisterRequestDTO register =
        new RegisterRequestDTO(
            "statistics.campaign", "statistics-campaign@test.com", "password123");
    api.post().uri("/api/auth/register").body(register).retrieve().toBodilessEntity();
    return api.post()
        .uri("/api/auth/login")
        .body(new LoginRequestDTO(register.email(), register.password()))
        .retrieve()
        .body(LoginResponseDTO.class)
        .token();
  }

  private void confirmIdentities(String token) {
    RestClient api = RestClient.builder().baseUrl(baseUrl()).build();
    api.post()
        .uri("/api/players/sync")
        .headers(headers -> headers.setBearerAuth(token))
        .retrieve()
        .toBodilessEntity();
    PlayersPageResponseDTO catalogue =
        api.get()
            .uri("/api/players?size=100")
            .headers(headers -> headers.setBearerAuth(token))
            .retrieve()
            .body(PlayersPageResponseDTO.class);
    List<Integer> tournaments = List.of(2, 3, 4, 5, 22);
    List<WhoScoredProperties.Competition> bindings = new ArrayList<>();
    for (int index = 1; index <= 5; index++) {
      String leagueName = "League " + index;
      PlayerResponseDTO local =
          catalogue.content().stream()
              .filter(player -> leagueName.equals(player.leagueName()))
              .findFirst()
              .orElseThrow();
      bindings.add(
          new WhoScoredProperties.Competition(local.leagueId(), tournaments.get(index - 1)));
    }
    org.mockito.Mockito.doReturn(bindings).when(properties).competitions();
    PlayerIdentitySyncRunResponseDTO identities =
        api.post()
            .uri("/api/players/identities/whoscored/sync")
            .headers(headers -> headers.setBearerAuth(token))
            .retrieve()
            .body(PlayerIdentitySyncRunResponseDTO.class);
    assertThat(identities.id()).isPositive();
  }

  @Nested
  @DisplayName("Modos del cliente de scraping")
  class Campaign {
    @BeforeEach
    void arrangeProvider() {
      when(integration.openSession())
          .thenReturn(mock(WhoScoredStatisticsScrapingIntegration.Session.class));
      when(integration.scrape(any(), any(), any()))
          .thenAnswer(invocation -> scraped(invocation.getArgument(1)));
    }

    /**
     * Fila del proveedor para el jugador recorrido.
     *
     * <p>La competición es la del tournaments enlazado a la liga del jugador. Devolver una fija
     * solo resolvería el contexto de un jugador de cada cinco, y los demás se confirmarían sin
     * datos, que es justo lo que la campaña debe detectar como falta de progreso.
     */
    private WhoScoredScrapedPlayer scraped(String playerId) {
      WhoScoredScrapedContextRow row =
          new WhoScoredScrapedContextRow(
              "initial",
              tournamentOf(playerId),
              "200",
              "11",
              "2025/2026",
              "2024/2025",
              List.of(
                  new WhoScoredScrapedMetricCell("apps", "6"),
                  new WhoScoredScrapedMetricCell("rating", "6.9033333333333333"),
                  new WhoScoredScrapedMetricCell("goal", "-")),
              java.time.Instant.parse("2026-10-15T12:00:00Z"));
      return new WhoScoredScrapedPlayer(
          playerId, List.of(row), new WhoScoredStructuredDataBlock(List.of()), 2, List.of());
    }

    /** Competición del proveedor que la aplicación asocia a la liga del jugador indicado. */
    private String tournamentOf(String providerPlayerId) {
      Long playerId =
          references
              .findByProviderAndExternalId(
                  footballmarket.models.enums.ExternalProvider.WHOSCORED, providerPlayerId)
              .orElseThrow()
              .getPlayer()
              .getId();
      Long leagueId = players.findVisibleById(playerId).orElseThrow().getTeam().getLeague().getId();
      return String.valueOf(
          properties.competitions().stream()
              .filter(competition -> competition.leagueId().equals(leagueId))
              .findFirst()
              .orElseThrow()
              .tournamentId());
    }

    @Test
    @DisplayName("El modo campaña encadena lotes de un jugador hasta la cobertura completa")
    void campaignHastaCoberturaCompleta() {
      // Arrange
      String token = authenticatedToken();
      confirmIdentities(token);
      String date = LocalDate.now(java.time.ZoneOffset.UTC).toString();

      // Act
      int exit =
          client()
              .run(
                  new String[] {
                    "--mode",
                    "campaign",
                    "--extraction-date",
                    date,
                    "--max-players",
                    "1",
                    "--base-url",
                    baseUrl()
                  },
                  Map.of("FOOTBALL_MARKET_TOKEN", token));

      // Assert
      assertThat(exit).isZero();
    }

    @Test
    @DisplayName("Sin token en el entorno el cliente no ejecuta ningún lote")
    void sinTokenNoEjecuta() {
      // Act
      int exit = client().run(new String[] {"--mode", "batch", "--base-url", baseUrl()}, Map.of());

      // Assert
      assertThat(exit).isEqualTo(2);
    }

    @Test
    @DisplayName(
        "Un token inválido termina con código distinto de cero y no declara cobertura completa")
    void tokenInvalido() {
      // Act
      int exit =
          client()
              .run(
                  new String[] {"--mode", "batch", "--base-url", baseUrl()},
                  Map.of("FOOTBALL_MARKET_TOKEN", "token-invalido"));

      // Assert
      assertThat(exit).isNotZero();
    }

    @Test
    @DisplayName("El cliente rechaza una opción no reconocida sin contactar la aplicación")
    void opcionNoReconocida() {
      // Act
      int exit =
          client()
              .run(
                  new String[] {"--mode", "json", "--base-url", baseUrl()},
                  Map.of("FOOTBALL_MARKET_TOKEN", "token"));

      // Assert
      assertThat(exit).isEqualTo(2);
    }
  }
}
