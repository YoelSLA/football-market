package footballmarket.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import footballmarket.integrations.TheSportsDbIntegration;
import footballmarket.integrations.exceptions.InvalidTheSportsDbResponseException;
import footballmarket.integrations.exceptions.TheSportsDbUnavailableException;
import footballmarket.models.Player;
import footballmarket.models.records.LeagueCandidate;
import footballmarket.models.records.PlayerCandidate;
import footballmarket.models.records.PlayerSnapshot;
import footballmarket.models.records.TeamCandidate;
import footballmarket.support.ResetPlayerCatalogListener;
import footballmarket.support.TestcontainersConfiguration;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@TestExecutionListeners(
    listeners = ResetPlayerCatalogListener.class,
    mergeMode = TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS)
class TeamEnrichmentServiceTest {
  @Autowired private TeamEnrichmentService enrichment;
  @Autowired private PlayerCatalogService catalog;
  @MockitoBean private TheSportsDbIntegration integration;

  private PlayerSnapshot snapshot(String teamName) {
    LeagueCandidate league = new LeagueCandidate("10", "League");
    TeamCandidate team = new TeamCandidate("100", teamName, "10", true);
    PlayerCandidate player =
        new PlayerCandidate("1", "Player", teamName, "League", "Forward", null, null, "100");
    return new PlayerSnapshot(List.of(player), 1, 0, List.of(league), List.of(team), List.of());
  }

  @Nested
  @DisplayName("Evaluación independiente y elegibilidad del nombre actual")
  class Eligibility {
    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    @DisplayName(
        "Una respuesta válida consume el nombre sin afectar al catálogo, con o sin completitud acreditada")
    void consumeValidEvaluation(boolean complete) {
      // Arrange
      catalog.applySynchronization(snapshot("Team"));
      TheSportsDbIntegration.TeamData candidate =
          new TheSportsDbIntegration.TeamData("sports-team", "Team", "Soccer");
      TheSportsDbIntegration.TeamSearch response =
          new TheSportsDbIntegration.TeamSearch(List.of(candidate), complete);
      when(integration.searchTeams("Team")).thenReturn(response);
      Long playerId = catalog.getActivePlayers(0, 20).getContent().getFirst().getId();

      // Act
      enrichment.enrichCurrentTeams();
      enrichment.enrichCurrentTeams();

      // Verify: la API funcional no expone referencias ni intentos; persistencia los cubre aparte.
      verify(integration, times(1)).searchTeams("Team");
      assertThat(catalog.getActivePlayers(0, 20).getContent())
          .extracting(Player::getId)
          .containsExactly(playerId);
    }

    @Test
    @DisplayName(
        "Un fallo técnico permite otra sincronización sin cambiar nombre ni revertir el catálogo")
    void retryAfterTechnicalFailure() {
      // Arrange
      catalog.applySynchronization(snapshot("Team"));
      TheSportsDbIntegration.TeamSearch empty =
          new TheSportsDbIntegration.TeamSearch(List.of(), true);
      when(integration.searchTeams("Team"))
          .thenThrow(new TheSportsDbUnavailableException())
          .thenReturn(empty);

      // Act
      enrichment.enrichCurrentTeams();
      enrichment.enrichCurrentTeams();
      enrichment.enrichCurrentTeams();

      // Verify
      verify(integration, times(2)).searchTeams("Team");
      assertThat(catalog.getActivePlayers(0, 20).getContent())
          .extracting(Player::getName)
          .containsExactly("Player");
    }

    @Test
    @DisplayName(
        "Una evaluación no resuelta habilita el nombre cambiado pero no un cambio cosmético equivalente")
    void evaluateOnlySemanticRename() {
      // Arrange
      catalog.applySynchronization(snapshot("Team"));
      TheSportsDbIntegration.TeamSearch empty =
          new TheSportsDbIntegration.TeamSearch(List.of(), true);
      when(integration.searchTeams("Team")).thenReturn(empty);
      when(integration.searchTeams("New Team")).thenReturn(empty);
      enrichment.enrichCurrentTeams();

      // Act
      catalog.applySynchronization(snapshot("TEAM"));
      enrichment.enrichCurrentTeams();
      catalog.applySynchronization(snapshot("New Team"));
      enrichment.enrichCurrentTeams();

      // Verify
      verify(integration, times(1)).searchTeams("Team");
      verify(integration, times(1)).searchTeams("New Team");
      assertThat(catalog.getActivePlayers(0, 20).getContent())
          .singleElement()
          .satisfies(player -> assertThat(player.getTeam().getName()).isEqualTo("New Team"));
    }

    @Test
    @DisplayName(
        "El fallo de un equipo no impide evaluar al siguiente ni altera jugadores confirmados")
    void allowPartialEnrichment() {
      // Arrange
      PlayerSnapshot initial = snapshot("Team");
      TeamCandidate secondTeam = new TeamCandidate("200", "Other Team", "10", true);
      PlayerCandidate secondPlayer =
          new PlayerCandidate(
              "2", "Other Player", "Other Team", "League", "Forward", null, null, "200");
      PlayerSnapshot photo =
          new PlayerSnapshot(
              List.of(initial.players().getFirst(), secondPlayer),
              2,
              0,
              initial.leagues(),
              List.of(initial.teams().getFirst(), secondTeam),
              List.of());
      catalog.applySynchronization(photo);
      TheSportsDbIntegration.TeamSearch empty =
          new TheSportsDbIntegration.TeamSearch(List.of(), false);
      when(integration.searchTeams("Team")).thenThrow(new InvalidTheSportsDbResponseException());
      when(integration.searchTeams("Other Team")).thenReturn(empty);

      // Act
      enrichment.enrichCurrentTeams();

      // Verify
      verify(integration, times(1)).searchTeams("Team");
      verify(integration, times(1)).searchTeams("Other Team");
      assertThat(catalog.getActivePlayers(0, 20).getContent())
          .extracting(Player::getName)
          .containsExactly("Player", "Other Player");
    }
  }
}
