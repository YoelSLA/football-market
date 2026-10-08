package footballmarket.models;

import static org.assertj.core.api.Assertions.*;

import footballmarket.models.enums.TeamResolutionResult;
import footballmarket.models.exceptions.InvalidCatalogEntityException;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class TeamIdentityMatcherTest {
  @Nested
  @DisplayName("Resolución externa estricta y completitud")
  class Matching {
    @Test
    @DisplayName("Una identidad repetida cuenta una sola vez con completitud acreditada")
    void deduplicateIdentity() {
      // Arrange
      TeamIdentityMatcher.Candidate first =
          new TeamIdentityMatcher.Candidate("source-id", "Atlético", "Soccer");
      TeamIdentityMatcher.Candidate duplicate =
          new TeamIdentityMatcher.Candidate("source-id", " ATLETICO ", "Soccer");

      // Act
      TeamIdentityMatcher.Evaluation result =
          TeamIdentityMatcher.evaluate("Atlético", List.of(first, duplicate), true);

      // Assert
      assertThat(result.result()).isEqualTo(TeamResolutionResult.MATCH);
      assertThat(result.externalId()).isEqualTo("source-id");
    }

    @Test
    @DisplayName("Dos identidades distintas coincidentes son ambiguas independientemente del orden")
    void distinctIdentitiesAreAmbiguous() {
      // Arrange
      TeamIdentityMatcher.Candidate first =
          new TeamIdentityMatcher.Candidate("first-source", "Arsenal", "Soccer");
      TeamIdentityMatcher.Candidate second =
          new TeamIdentityMatcher.Candidate("second-source", "ARSENAL", "Soccer");

      // Act
      TeamIdentityMatcher.Evaluation forward =
          TeamIdentityMatcher.evaluate("Arsenal", List.of(first, second), true);
      TeamIdentityMatcher.Evaluation reverse =
          TeamIdentityMatcher.evaluate("Arsenal", List.of(second, first), true);

      // Assert
      assertThat(forward.result()).isEqualTo(TeamResolutionResult.AMBIGUOUS);
      assertThat(reverse).isEqualTo(forward);
      assertThat(forward.externalId()).isNull();
    }

    @Test
    @DisplayName("Una sola coincidencia sin prueba de completitud no autoriza referencia")
    void incompleteSearchAbstains() {
      // Arrange
      TeamIdentityMatcher.Candidate candidate =
          new TeamIdentityMatcher.Candidate("source-id", "Arsenal", "Soccer");

      // Act
      TeamIdentityMatcher.Evaluation result =
          TeamIdentityMatcher.evaluate("Arsenal", List.of(candidate), false);

      // Assert
      assertThat(result.result()).isEqualTo(TeamResolutionResult.COMPLETENESS_UNPROVEN);
      assertThat(result.externalId()).isNull();
    }

    @Test
    @DisplayName("No tolera sufijos ni otros deportes para identificar equipos")
    void rejectSuffixAndSport() {
      // Arrange
      TeamIdentityMatcher.Candidate suffix =
          new TeamIdentityMatcher.Candidate("soccer-source", "Arsenal FC", "Soccer");
      TeamIdentityMatcher.Candidate otherSport =
          new TeamIdentityMatcher.Candidate("other-source", "Arsenal", "Basketball");

      // Act
      TeamIdentityMatcher.Evaluation result =
          TeamIdentityMatcher.evaluate("Arsenal", List.of(suffix, otherSport), true);

      // Assert
      assertThat(result.result()).isEqualTo(TeamResolutionResult.NO_MATCH);
      assertThat(result.externalId()).isNull();
    }

    @Test
    @DisplayName(
        "Duplicados contradictorios invalidan la evaluación aun sin completitud acreditada")
    void contradictoryDuplicatesAreInvalid() {
      // Arrange
      TeamIdentityMatcher.Candidate first =
          new TeamIdentityMatcher.Candidate("same-source", "Arsenal", "Soccer");
      TeamIdentityMatcher.Candidate contradiction =
          new TeamIdentityMatcher.Candidate("same-source", "Another Club", "Soccer");

      // Act / Assert
      assertThatThrownBy(
              () -> TeamIdentityMatcher.evaluate("Arsenal", List.of(first, contradiction), false))
          .isInstanceOf(InvalidCatalogEntityException.class);
    }
  }
}
