package footballmarket.integration;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import footballmarket.integrations.footballdata.FootballDataUnavailableException;
import footballmarket.models.Player;
import footballmarket.models.PlayerSnapshot;
import footballmarket.repositories.PlayerRepository;
import footballmarket.security.JWTProvider;
import footballmarket.services.FootballDataPlayerService;
import footballmarket.services.PlayerCatalogService;
import footballmarket.services.PlayerSynchronizationOrchestrator;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
class PlayerCatalogIntegrationTest {
  @Container static final PostgreSQLContainer database = new PostgreSQLContainer("postgres:18");

  @DynamicPropertySource
  static void databaseProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", database::getJdbcUrl);
    registry.add("spring.datasource.username", database::getUsername);
    registry.add("spring.datasource.password", database::getPassword);
  }

  @Autowired private PlayerRepository repository;
  @Autowired private PlayerCatalogService service;
  @Autowired private PlayerSynchronizationOrchestrator orchestrator;
  @MockitoBean private FootballDataPlayerService source;
  @Autowired private WebApplicationContext context;
  @Autowired private FilterChainProxy security;
  @Autowired private JWTProvider jwtProvider;

  @Test
  void authenticatedHttpFlowKeepsLocalCatalogAvailableAfterProviderFailure() throws Exception {
    var mvc = MockMvcBuilders.webAppContextSetup(context).addFilters(security).build();
    String authorization = "Bearer " + jwtProvider.generateToken("catalog@example.com");
    when(source.fetchSnapshot())
        .thenReturn(
            new PlayerSnapshot(List.of(new Player(7L, "Name", "Team", "League", "Forward")), 1, 0));
    mvc.perform(post("/players/sync").header("Authorization", authorization))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.created").value(1));
    when(source.fetchSnapshot()).thenThrow(new FootballDataUnavailableException());
    mvc.perform(post("/players/sync").header("Authorization", authorization))
        .andExpect(status().isBadGateway())
        .andExpect(jsonPath("$.status").value(502));
    mvc.perform(get("/players").header("Authorization", authorization))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[0].id").value(7))
        .andExpect(jsonPath("$.size").value(20))
        .andExpect(jsonPath("$.totalElements").value(1));
    clearInvocations(source);
    mvc.perform(post("/players/sync")).andExpect(status().isUnauthorized());
    verifyNoInteractions(source);
  }

  @AfterEach
  void removeTestPlayers() {
    repository.deleteAll();
  }

  @Test
  void appliesAtomicUpsertReactivationAndInactivationWithoutDeletion() {
    Player returning = new Player(1L, "Old", "T", "L", "P");
    returning.deactivate();
    repository.saveAllAndFlush(List.of(returning, new Player(2L, "Absent", "T", "L", "P")));
    var snapshot =
        new PlayerSnapshot(
            List.of(new Player(1L, "New", "T", "L", "P"), new Player(3L, "Created", "T", "L", "P")),
            2,
            0);
    var result = service.applySynchronization(snapshot);
    assertThat(result.created()).isEqualTo(1);
    assertThat(result.updated()).isEqualTo(1);
    assertThat(result.markedInactive()).isEqualTo(1);
    assertThat(repository.count()).isEqualTo(3);
    assertThat(repository.findById(1L).orElseThrow().getName()).isEqualTo("New");
    assertThat(repository.findById(1L).orElseThrow().isActive()).isTrue();
    assertThat(repository.findById(2L).orElseThrow().isActive()).isFalse();
    var repeated = service.applySynchronization(snapshot);
    assertThat(repeated.created()).isZero();
    assertThat(repeated.updated()).isEqualTo(2);
    assertThat(repeated.markedInactive()).isZero();
  }

  @Test
  void failedSnapshotLeavesPersistedCatalogUntouched() {
    repository.saveAndFlush(new Player(1L, "Original", "T", "L", "P"));
    when(source.fetchSnapshot()).thenThrow(new FootballDataUnavailableException());
    assertThatThrownBy(orchestrator::synchronize)
        .isInstanceOf(FootballDataUnavailableException.class);
    assertThat(repository.count()).isEqualTo(1);
    assertThat(repository.findById(1L).orElseThrow().getName()).isEqualTo("Original");
    assertThat(repository.findById(1L).orElseThrow().isActive()).isTrue();
  }

  @Test
  void databaseFailureRollsBackUpdatesCreationsAndInactivations() {
    repository.saveAllAndFlush(
        List.of(
            new Player(1L, "Original", "T", "L", "P"),
            new Player(2L, "Keep active", "T", "L", "P")));
    var snapshot =
        new PlayerSnapshot(
            List.of(
                new Player(1L, "Changed", "T", "L", "P"),
                new Player(3L, "Created", "T", "L", "P"),
                new Player(4L, "x".repeat(256), "T", "L", "P")),
            3,
            0);
    assertThatThrownBy(() -> service.applySynchronization(snapshot))
        .isInstanceOf(RuntimeException.class);
    assertThat(repository.count()).isEqualTo(2);
    assertThat(repository.findById(1L).orElseThrow().getName()).isEqualTo("Original");
    assertThat(repository.findById(2L).orElseThrow().isActive()).isTrue();
  }

  @BeforeEach
  void clean() {
    repository.deleteAll();
  }

  @Test
  void persistsExternalIdsAndPagesOnlyActivePlayers() {
    Player inactive = new Player(2L, "Inactive", "T", "L", "P");
    inactive.deactivate();
    repository.saveAllAndFlush(
        List.of(
            new Player(1L, "First", "T", "L", "P"),
            inactive,
            new Player(3L, "Last", "T", "L", "P")));
    var page = service.getActivePlayers(1, 1);
    assertThat(page.getContent()).extracting(Player::getId).containsExactly(3L);
    assertThat(page.getTotalElements()).isEqualTo(2);
    assertThat(page.getTotalPages()).isEqualTo(2);
    assertThat(repository.findById(2L)).get().extracting(Player::isActive).isEqualTo(false);
    assertThat(service.getActivePlayers(2, 1).getContent()).isEmpty();
  }
}
