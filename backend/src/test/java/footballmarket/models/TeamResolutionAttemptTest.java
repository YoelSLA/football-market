package footballmarket.models;

import static org.assertj.core.api.Assertions.*;

import footballmarket.models.enums.TeamResolutionFailure;
import footballmarket.models.enums.TeamResolutionResult;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class TeamResolutionAttemptTest {
  @Nested
  @DisplayName("Elegibilidad y oportunidades de resolución externa")
  class Eligibility {
    @Test
    @DisplayName("Una evaluación válida consume el nombre normalizado, no otro nombre")
    void validEvaluationConsumesName() {
      // Arrange
      League league = new League("Liga");
      Team team = new Team("Atlético", league, true);
      Instant calledAt = Instant.parse("2026-10-01T00:00:00Z");
      TeamResolutionAttempt attempt = new TeamResolutionAttempt(team, calledAt, team.getName());

      // Act
      attempt.evaluated(calledAt, team.getName(), TeamResolutionResult.COMPLETENESS_UNPROVEN);

      // Assert
      assertThat(attempt.eligible(" ATLETICO ", calledAt.plusSeconds(1))).isFalse();
      assertThat(attempt.eligible("Nombre distinto", calledAt.plusSeconds(1))).isTrue();
    }

    @Test
    @DisplayName(
        "Un fallo técnico conserva la evaluación previa y permite intentar el nuevo nombre")
    void failureDoesNotConsumeNewName() {
      // Arrange
      League league = new League("Liga");
      Team team = new Team("Original", league, true);
      Instant first = Instant.parse("2026-10-01T00:00:00Z");
      TeamResolutionAttempt attempt = new TeamResolutionAttempt(team, first, "Original");
      attempt.evaluated(first, "Original", TeamResolutionResult.NO_MATCH);

      // Act
      attempt.failed(
          first.plusSeconds(1), "Cambio", TeamResolutionFailure.TIMEOUT_OR_TRANSPORT, null);

      // Assert
      assertThat(attempt.getLastValidEvaluationName()).isEqualTo("Original");
      assertThat(attempt.getLastValidEvaluationAt()).isEqualTo(first);
      assertThat(attempt.getValidResult()).isEqualTo(TeamResolutionResult.NO_MATCH);
      assertThat(attempt.eligible("Cambio", first.plusSeconds(2))).isTrue();
      assertThat(attempt.eligible("Original", first.plusSeconds(2))).isFalse();
    }

    @Test
    @DisplayName("Retry-After pospone nuevas llamadas pero no consume una evaluación válida")
    void rateLimitDefersOpportunity() {
      // Arrange
      League league = new League("Liga");
      Team team = new Team("Equipo", league, true);
      Instant first = Instant.parse("2026-10-01T00:00:00Z");
      Instant retryAt = first.plusSeconds(60);
      TeamResolutionAttempt attempt = new TeamResolutionAttempt(team, first, "Equipo");

      // Act
      attempt.failed(first, "Equipo", TeamResolutionFailure.RATE_LIMITED, retryAt);

      // Assert
      assertThat(attempt.eligible("Equipo", retryAt.minusSeconds(1))).isFalse();
      assertThat(attempt.eligible("Equipo", retryAt)).isTrue();
      assertThat(attempt.getLastValidEvaluationAt()).isNull();
    }
  }
}
