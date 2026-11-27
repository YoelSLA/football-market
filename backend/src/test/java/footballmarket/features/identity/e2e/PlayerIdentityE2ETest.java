package footballmarket.features.identity.e2e;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;

import footballmarket.features.auth.presentation.dtos.request.LoginRequestDTO;
import footballmarket.features.auth.presentation.dtos.request.RegisterRequestDTO;
import footballmarket.features.auth.presentation.dtos.response.LoginResponseDTO;
import footballmarket.features.catalog.domain.models.player.Player;
import footballmarket.features.catalog.presentation.dtos.response.PlayerSyncResponseDTO;
import footballmarket.features.identity.presentation.dtos.response.PlayerIdentitySyncRunResponseDTO;
import footballmarket.integrations.whoscored.config.WhoScoredProperties;
import footballmarket.support.IdentityCatalogueFixture;
import footballmarket.support.IdentityControlledProvider;
import footballmarket.support.TestcontainersConfiguration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.convention.TestBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.web.client.RestClient;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import({TestcontainersConfiguration.class, IdentityCatalogueFixture.class})
@org.springframework.test.context.TestExecutionListeners(
    listeners = footballmarket.support.ResetPlayerCatalogListener.class,
    mergeMode =
        org.springframework.test.context.TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS)
@org.junit.jupiter.api.Tag("e2e")
class PlayerIdentityE2ETest {
  private static final IdentityControlledProvider PROVIDER = startProvider();
  @LocalServerPort private int port;
  @Autowired private IdentityCatalogueFixture catalogueFixture;
  @MockitoSpyBean private WhoScoredProperties properties;

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

  /** Atributos observables del catálogo persistido, independientes de la elegibilidad actual. */
  private record CatalogIdentity(
      Long id,
      String name,
      Long teamId,
      String teamName,
      Long leagueId,
      String leagueName,
      String position,
      LocalDate dateOfBirth,
      String nationality,
      String imageUrl,
      String fallbackImageUrl) {}

  private List<CatalogIdentity> catalogIdentities() {
    return catalogueFixture.identitySnapshot().stream()
        .map(PlayerIdentityE2ETest::catalogIdentity)
        .toList();
  }

  private static CatalogIdentity catalogIdentity(Player player) {
    return new CatalogIdentity(
        player.getId(),
        player.getName(),
        player.getTeam().getId(),
        player.getTeam().getName(),
        player.getLeague().getId(),
        player.getLeague().getName(),
        player.getPosition(),
        player.getDateOfBirth(),
        player.getNationality(),
        player.getImageUrl(),
        player.getFallbackImageUrl());
  }

  @Nested
  @DisplayName("Journey de sincronización completa y repetición")
  class HistoricalJourney {
    @Test
    @DisplayName(
        "Una sola llamada resuelve actual e histórico y repetir conserva las referencias sin red")
    void resolvesPendingFromHttp() {
      // Arrange
      PROVIDER.historicalScenario(true);
      try {
        RestClient api = RestClient.builder().baseUrl("http://localhost:" + port).build();
        RegisterRequestDTO register =
            new RegisterRequestDTO(
                "identity.historical", "identity-historical@test.com", "password123");
        api.post().uri("/api/auth/register").body(register).retrieve().toBodilessEntity();
        LoginRequestDTO login = new LoginRequestDTO(register.email(), register.password());
        LoginResponseDTO authenticated =
            api.post().uri("/api/auth/login").body(login).retrieve().body(LoginResponseDTO.class);
        String token = authenticated.token();
        api.post()
            .uri("/api/players/sync")
            .headers(headers -> headers.setBearerAuth(token))
            .retrieve()
            .body(PlayerSyncResponseDTO.class);
        List<CatalogIdentity> original = catalogIdentities();
        List<Integer> tournaments = List.of(2, 3, 4, 5, 22);
        List<WhoScoredProperties.Competition> bindings = new ArrayList<>();
        for (int index = 1; index <= 5; index++) {
          // La liga recién sincronizada aún no es elegible: sus jugadores no tienen métricas.
          Long leagueId = catalogueFixture.leagueId("League " + index);
          bindings.add(new WhoScoredProperties.Competition(leagueId, tournaments.get(index - 1)));
        }
        doReturn(bindings).when(properties).competitions();
        // Act
        PlayerIdentitySyncRunResponseDTO current =
            api.post()
                .uri("/api/players/identities/whoscored/sync")
                .headers(headers -> headers.setBearerAuth(token))
                .retrieve()
                .body(PlayerIdentitySyncRunResponseDTO.class);
        int requests = PROVIDER.historicalRequests();
        PlayerIdentitySyncRunResponseDTO repeated =
            api.post()
                .uri("/api/players/identities/whoscored/sync")
                .headers(headers -> headers.setBearerAuth(token))
                .retrieve()
                .body(PlayerIdentitySyncRunResponseDTO.class);
        // Assert
        assertThat(current.status()).isEqualTo("COMPLETED");
        assertThat(current.selectedPlayers()).isEqualTo(5);
        assertThat(current.referencedPlayers()).isEqualTo(5);
        assertThat(current.noMatchPlayers()).isZero();
        assertThat(current.unresolvedPlayers()).isZero();
        assertThat(repeated).isEqualTo(current);
        assertThat(repeated.status()).isEqualTo("COMPLETED");
        // Verify
        assertThat(requests).isEqualTo(5);
        assertThat(PROVIDER.historicalRequests()).isEqualTo(requests);
        assertThat(catalogIdentities()).containsExactlyElementsOf(original);
      } finally {
        PROVIDER.historicalScenario(false);
      }
    }
  }

  @Nested
  @DisplayName("Journey de identificación actual")
  class CurrentJourney {
    @Test
    @DisplayName(
        "Autentica, carga catálogo por HTTP e identifica sin alterar atributos ni duplicar referencias")
    void identifiesAndRepeatsFromHttp() {
      // Arrange: todos los datos funcionales entran por HTTP y DTO productivos.
      RestClient api = RestClient.builder().baseUrl("http://localhost:" + port).build();
      RegisterRequestDTO register =
          new RegisterRequestDTO("identity.journey", "identity-journey@test.com", "password123");
      api.post().uri("/api/auth/register").body(register).retrieve().toBodilessEntity();
      LoginRequestDTO login = new LoginRequestDTO(register.email(), register.password());
      LoginResponseDTO authenticated =
          api.post().uri("/api/auth/login").body(login).retrieve().body(LoginResponseDTO.class);
      String token = authenticated.token();
      api.post()
          .uri("/api/players/sync")
          .headers(headers -> headers.setBearerAuth(token))
          .retrieve()
          .body(PlayerSyncResponseDTO.class);
      List<CatalogIdentity> original = catalogIdentities();
      List<Integer> tournaments = List.of(2, 3, 4, 5, 22);
      List<WhoScoredProperties.Competition> bindings = new ArrayList<>();
      for (int index = 1; index <= 5; index++) {
        // La liga recién sincronizada aún no es elegible: sus jugadores no tienen métricas.
        Long leagueId = catalogueFixture.leagueId("League " + index);
        bindings.add(new WhoScoredProperties.Competition(leagueId, tournaments.get(index - 1)));
      }
      doReturn(bindings).when(properties).competitions();
      // Act
      PlayerIdentitySyncRunResponseDTO first =
          api.post()
              .uri("/api/players/identities/whoscored/sync")
              .headers(headers -> headers.setBearerAuth(token))
              .retrieve()
              .body(PlayerIdentitySyncRunResponseDTO.class);
      PlayerIdentitySyncRunResponseDTO repeated =
          api.post()
              .uri("/api/players/identities/whoscored/sync")
              .headers(headers -> headers.setBearerAuth(token))
              .retrieve()
              .body(PlayerIdentitySyncRunResponseDTO.class);
      // Assert
      assertThat(first.status()).isEqualTo("COMPLETED");
      assertThat(first.selectedPlayers()).isEqualTo(5);
      assertThat(first.referencedPlayers()).isEqualTo(5);
      assertThat(first.noMatchPlayers()).isZero();
      assertThat(first.unresolvedPlayers()).isZero();
      assertThat(repeated).isEqualTo(first);
      // Verify
      assertThat(catalogIdentities()).containsExactlyElementsOf(original);
    }
  }
}
