package footballmarket.repositories;

import static org.assertj.core.api.Assertions.assertThat;

import footballmarket.features.catalog.domain.enums.TeamResolutionFailure;
import footballmarket.features.catalog.domain.enums.TeamResolutionResult;
import footballmarket.features.catalog.domain.models.league.League;
import footballmarket.features.catalog.domain.models.team.Team;
import footballmarket.features.catalog.domain.models.team.TeamResolutionAttempt;
import footballmarket.features.catalog.infraestructure.LeagueRepository;
import footballmarket.features.catalog.infraestructure.TeamRepository;
import footballmarket.features.catalog.infraestructure.TeamResolutionAttemptRepository;
import footballmarket.support.ResetPlayerCatalogListener;
import footballmarket.support.TestcontainersConfiguration;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestExecutionListeners;

/**
 * Persistencia propia de los intentos de resolución.
 *
 * <p>El identificador lo deriva la asociación con {@code Team} y se asigna en el constructor, de
 * modo que confirmar un intento nuevo debe usar la vía de inserción y no la de actualización.
 */
@SpringBootTest
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@TestExecutionListeners(
    listeners = ResetPlayerCatalogListener.class,
    mergeMode = TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS)
@org.junit.jupiter.api.Tag("integration")
class TeamResolutionAttemptPersistenceTest {
  @Autowired private TeamRepository teams;
  @Autowired private LeagueRepository leagues;
  @Autowired private TeamResolutionAttemptRepository attempts;

  private Team team() {
    return teams.saveAndFlush(new Team("Team", leagues.saveAndFlush(new League("League")), true));
  }

  @Nested
  @DisplayName("Confirmación y relectura de intentos por equipo")
  class Persistence {
    @Test
    @DisplayName("Confirma un intento nuevo y conserva la evaluación válida")
    void persistsNewAttempt() {
      // Arrange
      Team team = team();
      Instant calledAt = Instant.parse("2026-10-01T10:00:00Z");
      TeamResolutionAttempt attempt = new TeamResolutionAttempt(team, calledAt, "Team");
      attempt.evaluated(calledAt, "Team", TeamResolutionResult.MATCH);

      // Act
      attempts.saveAndFlush(attempt);

      // Assert: la asociación con Team es diferida y no se fuerza su carga fuera de transacción.
      assertThat(attempts.findById(team.getId()))
          .hasValueSatisfying(
              persisted -> {
                assertThat(persisted.getId()).isEqualTo(team.getId());
                assertThat(persisted.getLastCallName()).isEqualTo("Team");
                assertThat(persisted.getLastValidEvaluationName()).isEqualTo("Team");
                assertThat(persisted.getValidResult()).isEqualTo(TeamResolutionResult.MATCH);
                assertThat(persisted.getTechnicalResult()).isNull();
              });
    }

    @Test
    @DisplayName("Actualiza el intento existente sin crear otro para el mismo equipo")
    void updatesExistingAttempt() {
      // Arrange
      Team team = team();
      Instant firstCall = Instant.parse("2026-10-01T10:00:00Z");
      attempts.saveAndFlush(new TeamResolutionAttempt(team, firstCall, "Team"));

      // Act
      TeamResolutionAttempt reloaded = attempts.findById(team.getId()).orElseThrow();
      reloaded.failed(
          Instant.parse("2026-10-01T11:00:00Z"),
          "Team",
          TeamResolutionFailure.SERVER_ERROR,
          Instant.parse("2026-10-01T12:00:00Z"));
      attempts.saveAndFlush(reloaded);

      // Assert
      assertThat(attempts.findAll()).hasSize(1);
      assertThat(attempts.findById(team.getId()))
          .hasValueSatisfying(
              persisted -> {
                assertThat(persisted.getTechnicalResult())
                    .isEqualTo(TeamResolutionFailure.SERVER_ERROR);
                assertThat(persisted.getRetryNotBefore())
                    .isEqualTo(Instant.parse("2026-10-01T12:00:00Z"));
              });
    }
  }
}
