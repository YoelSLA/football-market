package footballmarket.models;

import static org.assertj.core.api.Assertions.*;

import footballmarket.models.enums.ExternalProvider;
import footballmarket.models.enums.ReviewCategory;
import footballmarket.models.enums.ReviewCause;
import footballmarket.models.enums.ReviewSubjectType;
import footballmarket.models.exceptions.InvalidCatalogEntityException;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class PendingReviewCaseTest {
  @Nested
  @DisplayName("Evidencia y detecciones de casos pendientes")
  class Observations {
    @Test
    @DisplayName("La redetección conserva el texto original y la primera fecha")
    void preserveOriginalEvidence() {
      // Arrange
      Instant first = Instant.parse("2026-10-01T00:00:00Z");
      Instant last = first.plusSeconds(60);
      Map<String, Object> original =
          Map.of("originalTeamName", "Texto original", "candidates", java.util.List.of());
      PendingReviewCase review =
          new PendingReviewCase(
              ReviewCategory.LEGACY_TEAM_ASSOCIATION,
              ReviewCause.NO_TEAM_MATCH,
              ReviewSubjectType.PLAYER,
              1L,
              null,
              null,
              "LEGACY_TEAM_ASSOCIATION:PLAYER:1",
              first,
              original);
      Map<String, Object> observed =
          Map.of("originalTeamName", "Texto cambiado", "candidates", java.util.List.of(2L, 3L));

      // Act
      review.observe(ReviewCause.MULTIPLE_TEAM_MATCHES, last, observed);

      // Assert
      assertThat(review.getFirstDetectedAt()).isEqualTo(first);
      assertThat(review.getLastDetectedAt()).isEqualTo(last);
      assertThat(review.getCauseCode()).isEqualTo(ReviewCause.MULTIPLE_TEAM_MATCHES);
      assertThat(review.getEvidence())
          .containsEntry("originalTeamName", "Texto original")
          .containsEntry("candidates", java.util.List.of(2L, 3L));
    }

    @Test
    @DisplayName("Reconocer el sujeto interno conserva la clave externa y rechaza reasignarlo")
    void reconcileSubjectWithoutReassignment() {
      // Arrange
      Instant detected = Instant.parse("2026-10-01T00:00:00Z");
      PendingReviewCase review =
          new PendingReviewCase(
              ReviewCategory.PLAYER_TEAM_UNRESOLVED,
              ReviewCause.TEAM_UNRESOLVED,
              ReviewSubjectType.PLAYER,
              null,
              ExternalProvider.FOOTBALL_DATA,
              "player-source",
              "PLAYER_TEAM_UNRESOLVED:external:player-source:missing",
              detected,
              Map.of("receivedTeamName", "Equipo recibido"));

      // Act
      review.recognizeSubject(42L);

      // Assert
      assertThat(review.getSubjectId()).isEqualTo(42L);
      assertThat(review.getCaseKey())
          .isEqualTo("PLAYER_TEAM_UNRESOLVED:external:player-source:missing");
      assertThat(review.getSubjectExternalId()).isEqualTo("player-source");
      assertThatThrownBy(() -> review.recognizeSubject(43L))
          .isInstanceOf(InvalidCatalogEntityException.class);
      assertThat(review.getSubjectId()).isEqualTo(42L);
    }

    @Test
    @DisplayName("Una fecha anterior no altera causa, fechas ni evidencia")
    void rejectOlderObservation() {
      // Arrange
      Instant detected = Instant.parse("2026-10-01T00:00:00Z");
      Map<String, Object> evidence = Map.of("originalTeamName", "Original");
      PendingReviewCase review =
          new PendingReviewCase(
              ReviewCategory.LEGACY_TEAM_ASSOCIATION,
              ReviewCause.NO_TEAM_MATCH,
              ReviewSubjectType.PLAYER,
              1L,
              null,
              null,
              "LEGACY_TEAM_ASSOCIATION:PLAYER:1",
              detected,
              evidence);

      // Act / Assert
      assertThatThrownBy(
              () ->
                  review.observe(
                      ReviewCause.MULTIPLE_TEAM_MATCHES,
                      detected.minusSeconds(1),
                      Map.of("originalTeamName", "Cambio")))
          .isInstanceOf(InvalidCatalogEntityException.class);
      assertThat(review.getCauseCode()).isEqualTo(ReviewCause.NO_TEAM_MATCH);
      assertThat(review.getEvidence()).isEqualTo(evidence);
      assertThat(review.getLastDetectedAt()).isEqualTo(detected);
    }
  }
}
