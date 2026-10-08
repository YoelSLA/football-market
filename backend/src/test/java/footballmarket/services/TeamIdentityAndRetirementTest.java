package footballmarket.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import footballmarket.integrations.TheSportsDbIntegration;
import footballmarket.models.Player;
import footballmarket.models.records.InvalidPlayerObservation;
import footballmarket.models.records.LeagueCandidate;
import footballmarket.models.records.PlayerCandidate;
import footballmarket.models.records.PlayerSnapshot;
import footballmarket.models.records.PlayerSynchronizationResult;
import footballmarket.models.records.TeamCandidate;
import footballmarket.support.ResetPlayerCatalogListener;
import footballmarket.support.TestcontainersConfiguration;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * Comportamiento funcional de identidad, protecciones y vigencia observable por la API del Service.
 */
@SpringBootTest
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@TestExecutionListeners(
    listeners = ResetPlayerCatalogListener.class,
    mergeMode = TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS)
class TeamIdentityAndRetirementTest {
  @Autowired private PlayerCatalogService catalog;
  @MockitoBean private TheSportsDbIntegration integration;

  private PlayerCandidate candidate(String externalId, String name, String teamId) {
    return new PlayerCandidate(
        externalId,
        name,
        "101".equals(teamId) ? "Team A" : "Team B",
        "League",
        "Forward",
        null,
        null,
        teamId);
  }

  private PlayerSnapshot snapshot(PlayerCandidate... candidates) {
    return snapshot(List.of(), candidates);
  }

  private PlayerSnapshot snapshot(
      List<InvalidPlayerObservation> invalid, PlayerCandidate... candidates) {
    LeagueCandidate league = new LeagueCandidate("10", "League");
    TeamCandidate first = new TeamCandidate("101", "Team A", "10", true);
    TeamCandidate second = new TeamCandidate("102", "Team B", "10", true);
    return new PlayerSnapshot(
        List.of(candidates),
        candidates.length,
        invalid.size(),
        List.of(league),
        List.of(first, second),
        invalid);
  }

  private Long firstPlayerId() {
    return catalog.getActivePlayers(0, 20).getContent().getFirst().getId();
  }

  @Nested
  @DisplayName("Identidad interna estable ante cambios del proveedor")
  class StableIdentity {
    @Test
    @DisplayName(
        "Un cambio de nombre y de liga conserva el identificador del equipo y la asociación")
    void preserveIdentityAcrossRenameAndLeagueChange() {
      // Arrange
      PlayerCandidate original = candidate("1", "Player", "101");
      catalog.applySynchronization(snapshot(original));
      Long playerId = firstPlayerId();
      LeagueCandidate renamedLeague = new LeagueCandidate("10", "Renamed League");
      TeamCandidate renamedTeam = new TeamCandidate("101", "Team A Renamed", "10", true);
      PlayerCandidate renamed = candidate("1", "Player", "101");

      // Act
      PlayerSnapshot photo =
          new PlayerSnapshot(
              List.of(renamed), 1, 0, List.of(renamedLeague), List.of(renamedTeam), List.of());
      catalog.applySynchronization(photo);

      // Verify
      assertThat(catalog.getActivePlayers(0, 20).getContent())
          .singleElement()
          .satisfies(
              player -> {
                assertThat(player.getId()).isEqualTo(playerId);
                assertThat(player.getTeam().getName()).isEqualTo("Team A Renamed");
                assertThat(player.getLeague().getName()).isEqualTo("Renamed League");
              });
    }

    @Test
    @DisplayName(
        "Una transferencia válida conserva la identidad del jugador y deriva la liga del nuevo equipo")
    void transferKeepsPlayerIdentity() {
      // Arrange
      catalog.applySynchronization(snapshot(candidate("1", "Player", "101")));
      Long playerId = firstPlayerId();

      // Act
      catalog.applySynchronization(snapshot(candidate("1", "Player", "102")));

      // Verify
      assertThat(catalog.getActivePlayers(0, 20).getContent())
          .singleElement()
          .satisfies(
              player -> {
                assertThat(player.getId()).isEqualTo(playerId);
                assertThat(player.getTeam().getName()).isEqualTo("Team B");
                assertThat(player.getLeague().getName()).isEqualTo("League");
              });
    }
  }

  @Nested
  @DisplayName("Protección de presentes identificables inválidos")
  class InvalidPresenceProtection {
    @Test
    @DisplayName("Un presente sin equipo resoluble conserva estado y no se procesa")
    void keepStateWhenTeamUnresolved() {
      // Arrange
      catalog.applySynchronization(snapshot(candidate("1", "Player", "101")));
      Long playerId = firstPlayerId();
      InvalidPlayerObservation observation =
          new InvalidPlayerObservation("1", "999", "Team X", "position");

      // Act
      PlayerSynchronizationResult result =
          catalog.applySynchronization(snapshot(List.of(observation)));

      // Assert
      assertThat(result.discardedInvalid()).isEqualTo(1);

      // Verify
      assertThat(catalog.getActivePlayers(0, 20).getContent())
          .extracting(Player::getId)
          .containsExactly(playerId);
    }

    @Test
    @DisplayName("Un equipo recibido sin liga resoluble no altera el catálogo consultable")
    void keepCatalogWhenLeagueUnresolved() {
      // Arrange
      catalog.applySynchronization(snapshot(candidate("1", "Player", "101")));

      // Act
      PlayerSnapshot photo =
          new PlayerSnapshot(
              List.of(),
              0,
              0,
              List.of(),
              List.of(new TeamCandidate("999", "Orphan Team", "404", true)),
              List.of());
      PlayerSynchronizationResult result = catalog.applySynchronization(photo);

      // Assert
      assertThat(result.markedInactive()).isZero();

      // Verify
      assertThat(catalog.getActivePlayers(0, 20).getContent())
          .extracting(Player::getTeam)
          .hasSize(1);
    }
  }

  @Nested
  @DisplayName("Vigencia del equipo, retirada y regreso")
  class TeamCurrency {
    @Test
    @DisplayName("La ausencia confirmada retira el equipo y desactiva sus jugadores sin borrarlos")
    void retireAbsentTeamAndDeactivatePlayers() {
      // Arrange
      catalog.applySynchronization(snapshot(candidate("1", "Player", "101")));
      Long playerId = firstPlayerId();

      // Act
      catalog.applySynchronization(snapshot());

      // Assert / Verify
      assertThat(catalog.getActivePlayers(0, 20)).isEmpty();
      assertThat(catalog.applySynchronization(snapshot(candidate("1", "Player", "101"))).updated())
          .isEqualTo(1);
      assertThat(catalog.getActivePlayers(0, 20).getContent())
          .extracting(Player::getId)
          .containsExactly(playerId);
    }

    @Test
    @DisplayName("Un equipo retirado y sus jugadores no aparecen aunque el jugador siga activo")
    void excludeRetiredTeamFromCatalog() {
      // Arrange
      catalog.applySynchronization(snapshot(candidate("1", "Player", "101")));
      when(integration.searchTeams("Team B"))
          .thenReturn(new TheSportsDbIntegration.TeamSearch(List.of(), false));

      // Act
      catalog.applySynchronization(snapshot(candidate("1", "Player", "101")));

      // Verify
      assertThat(catalog.getActivePlayers(0, 20)).isEmpty();
    }

    @Test
    @DisplayName("El regreso válido de un equipo reactiva a sus jugadores presentes válidos")
    void reactivateOnReturn() {
      // Arrange
      catalog.applySynchronization(snapshot(candidate("1", "Player", "101")));
      Long playerId = firstPlayerId();
      catalog.applySynchronization(snapshot());
      assertThat(catalog.getActivePlayers(0, 20)).isEmpty();

      // Act
      PlayerSynchronizationResult result =
          catalog.applySynchronization(snapshot(candidate("1", "Player", "101")));

      // Assert
      assertThat(result.updated()).isEqualTo(1);

      // Verify
      assertThat(catalog.getActivePlayers(0, 20).getContent())
          .extracting(Player::getId)
          .containsExactly(playerId);
    }
  }
}
