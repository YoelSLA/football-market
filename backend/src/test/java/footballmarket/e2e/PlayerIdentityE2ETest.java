package footballmarket.e2e;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;

import footballmarket.config.WhoScoredProperties;
import footballmarket.features.auth.presentation.dtos.request.LoginRequestDTO;
import footballmarket.features.auth.presentation.dtos.request.RegisterRequestDTO;
import footballmarket.features.auth.presentation.dtos.response.LoginResponseDTO;
import footballmarket.features.catalog.presentation.dtos.response.PlayerResponseDTO;
import footballmarket.features.catalog.presentation.dtos.response.PlayerSyncResponseDTO;
import footballmarket.features.catalog.presentation.dtos.response.PlayersPageResponseDTO;
import footballmarket.features.identity.presentation.dtos.response.PlayerIdentitySyncRunResponseDTO;
import footballmarket.support.IdentityControlledProvider;
import footballmarket.support.TestcontainersConfiguration;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.convention.TestBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.web.client.RestClient;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@org.springframework.test.context.TestExecutionListeners(
    listeners = footballmarket.support.ResetPlayerCatalogListener.class,
    mergeMode =
        org.springframework.test.context.TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS)
@org.junit.jupiter.api.Tag("e2e")
class PlayerIdentityE2ETest {
  private static final IdentityControlledProvider PROVIDER = startProvider();
  @LocalServerPort private int port;
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

  @Nested
  @DisplayName("Journey actual, histórico y repetición")
  class HistoricalJourney {
    @Test
    @DisplayName(
        "Resuelve únicamente el pendiente y repetir el histórico no consulta red ni duplica referencias")
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
        PlayersPageResponseDTO original =
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
              original.content().stream()
                  .filter(player -> leagueName.equals(player.leagueName()))
                  .findFirst()
                  .orElseThrow();
          bindings.add(
              new WhoScoredProperties.Competition(local.leagueId(), tournaments.get(index - 1)));
        }
        doReturn(bindings).when(properties).competitions();
        PlayerIdentitySyncRunResponseDTO current =
            api.post()
                .uri("/api/players/identities/whoscored/sync")
                .headers(headers -> headers.setBearerAuth(token))
                .retrieve()
                .body(PlayerIdentitySyncRunResponseDTO.class);
        // Act
        PlayerIdentitySyncRunResponseDTO historical =
            api.post()
                .uri("/api/players/identities/whoscored/sync-runs/{id}/historical", current.id())
                .headers(headers -> headers.setBearerAuth(token))
                .retrieve()
                .body(PlayerIdentitySyncRunResponseDTO.class);
        int requests = PROVIDER.historicalRequests();
        PlayerIdentitySyncRunResponseDTO repeated =
            api.post()
                .uri("/api/players/identities/whoscored/sync-runs/{id}/historical", current.id())
                .headers(headers -> headers.setBearerAuth(token))
                .retrieve()
                .body(PlayerIdentitySyncRunResponseDTO.class);
        // Assert
        assertThat(current.counters().resolved()).isEqualTo(4);
        assertThat(current.counters().noMatch()).isEqualTo(1);
        assertThat(historical.sourceRunId()).isEqualTo(current.id());
        assertThat(historical.passType()).isEqualTo("HISTORICAL");
        assertThat(historical.id()).isNotEqualTo(current.id());
        assertThat(historical.status()).isEqualTo("COMPLETED");
        assertThat(historical.counters().selectedPlayers()).isEqualTo(1);
        assertThat(historical.counters().resolved()).isEqualTo(1);
        assertThat(historical.counters().pendingPlayers()).isZero();
        assertThat(repeated.id()).isNotEqualTo(historical.id());
        assertThat(repeated.sourceRunId()).isEqualTo(current.id());
        assertThat(repeated.counters().selectedPlayers()).isZero();
        assertThat(repeated.counters().resolved()).isZero();
        assertThat(repeated.status()).isEqualTo("COMPLETED");
        // Verify
        assertThat(requests).isEqualTo(5);
        assertThat(PROVIDER.historicalRequests()).isEqualTo(requests);
        PlayersPageResponseDTO after =
            api.get()
                .uri("/api/players?size=100")
                .headers(headers -> headers.setBearerAuth(token))
                .retrieve()
                .body(PlayersPageResponseDTO.class);
        assertThat(after.content()).containsExactlyElementsOf(original.content());
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
      PlayersPageResponseDTO original =
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
            original.content().stream()
                .filter(player -> leagueName.equals(player.leagueName()))
                .findFirst()
                .orElseThrow();
        bindings.add(
            new WhoScoredProperties.Competition(local.leagueId(), tournaments.get(index - 1)));
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
      assertThat(first.counters().resolved()).isEqualTo(5);
      assertThat(repeated.id()).isNotEqualTo(first.id());
      assertThat(repeated.counters().alreadyReferenced()).isEqualTo(5);
      assertThat(repeated.counters().resolved()).isZero();
      // Verify
      PlayersPageResponseDTO after =
          api.get()
              .uri("/api/players?size=100")
              .headers(headers -> headers.setBearerAuth(token))
              .retrieve()
              .body(PlayersPageResponseDTO.class);
      assertThat(after.content()).containsExactlyElementsOf(original.content());
    }
  }
}
