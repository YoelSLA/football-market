package footballmarket.models;

import static org.assertj.core.api.Assertions.*;

import footballmarket.models.enums.ExternalProvider;
import footballmarket.models.exceptions.InvalidCatalogEntityException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class TeamLeagueTest {
  @Nested
  @DisplayName("Identidad y cambios actuales de equipos y ligas")
  class Identity {
    @Test
    @DisplayName("Renombrar la liga conserva sus referencias y la asociación de sus equipos")
    void renameLeaguePreservesAssociation() {
      // Arrange
      League league = new League("Liga original");
      league.addExternalReference(ExternalProvider.FOOTBALL_DATA, "competition-source");
      Team team = new Team("Equipo", league, true);

      // Act
      league.rename("Nombre nuevo");

      // Assert
      assertThat(team.getLeague()).isSameAs(league);
      assertThat(team.getLeague().getName()).isEqualTo("Nombre nuevo");
      assertThat(league.getExternalReferences())
          .singleElement()
          .satisfies(
              reference -> {
                assertThat(reference.getLeague()).isSameAs(league);
                assertThat(reference.getExternalId()).isEqualTo("competition-source");
              });
    }

    @Test
    @DisplayName("El cambio de nombre y liga conserva la referencia externa del equipo")
    void changeLeaguePreservesReference() {
      // Arrange
      League first = new League("Primera liga");
      League second = new League("Segunda liga");
      Team team = new Team("Nombre original", first, true);
      team.addExternalReference(ExternalProvider.FOOTBALL_DATA, "team-source");
      TeamExternalReference reference = team.getExternalReferences().getFirst();

      // Act
      team.update("Nombre actual", second);

      // Assert
      assertThat(team.getName()).isEqualTo("Nombre actual");
      assertThat(team.getLeague()).isSameAs(second);
      assertThat(team.getExternalReferences()).containsExactly(reference);
      assertThat(reference.getTeam()).isSameAs(team);
    }

    @Test
    @DisplayName("Retirada y regreso conservan la liga y las referencias históricas")
    void retirementPreservesIdentity() {
      // Arrange
      League league = new League("Liga");
      Team team = new Team("Equipo", league, true);
      team.addExternalReference(ExternalProvider.FOOTBALL_DATA, "team-source");
      TeamExternalReference reference = team.getExternalReferences().getFirst();

      // Act
      team.retire();

      // Assert
      assertThat(team.isCurrent()).isFalse();
      assertThat(team.getExternalReferences()).containsExactly(reference);
      assertThat(team.getLeague()).isSameAs(league);

      // Act
      team.markCurrent();

      // Assert
      assertThat(team.isCurrent()).isTrue();
      assertThat(team.getExternalReferences()).containsExactly(reference);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t", "\n"})
    @DisplayName("Un nombre inválido no altera el equipo ni su liga")
    void invalidRenameIsAtomic(String invalidName) {
      // Arrange
      League first = new League("Primera liga");
      League second = new League("Segunda liga");
      Team team = new Team("Original", first, true);

      // Act / Assert
      assertThatThrownBy(() -> team.update(invalidName, second))
          .isInstanceOf(InvalidCatalogEntityException.class);
      assertThat(team.getName()).isEqualTo("Original");
      assertThat(team.getLeague()).isSameAs(first);
    }

    @Test
    @DisplayName("Una liga ausente no permite actualizar parcialmente el equipo")
    void missingLeagueIsAtomic() {
      // Arrange
      League league = new League("Liga");
      Team team = new Team("Original", league, true);

      // Act / Assert
      assertThatThrownBy(() -> team.update("Cambio", null))
          .isInstanceOf(InvalidCatalogEntityException.class);
      assertThat(team.getName()).isEqualTo("Original");
      assertThat(team.getLeague()).isSameAs(league);
    }
  }

  @Nested
  @DisplayName("Referencias externas inmutables por proveedor")
  class References {
    @Test
    @DisplayName(
        "Referencias sin propietario o proveedor se rechazan antes de modificar las colecciones")
    void requireReferenceOwnerAndProvider() {
      // Arrange
      League league = new League("League");
      Team team = new Team("Team", league, true);

      // Act / Assert
      assertThatThrownBy(
              () -> new LeagueExternalReference(null, ExternalProvider.FOOTBALL_DATA, "id"))
          .isInstanceOf(InvalidCatalogEntityException.class);
      assertThatThrownBy(
              () -> new TeamExternalReference(null, ExternalProvider.FOOTBALL_DATA, "id"))
          .isInstanceOf(InvalidCatalogEntityException.class);
      assertThatThrownBy(() -> league.addExternalReference(null, "id"))
          .isInstanceOf(InvalidCatalogEntityException.class);
      assertThatThrownBy(() -> team.addExternalReference(null, "id"))
          .isInstanceOf(InvalidCatalogEntityException.class);

      // Verify
      assertThat(league.getExternalReferences()).isEmpty();
      assertThat(team.getExternalReferences()).isEmpty();
    }

    @Test
    @DisplayName("Agregar la misma identidad es idempotente y otra del mismo proveedor se rechaza")
    void rejectReplacement() {
      // Arrange
      League league = new League("Liga");
      Team team = new Team("Equipo", league, true);
      league.addExternalReference(ExternalProvider.FOOTBALL_DATA, "league-source");
      team.addExternalReference(ExternalProvider.FOOTBALL_DATA, "team-source");

      // Act
      league.addExternalReference(ExternalProvider.FOOTBALL_DATA, "league-source");
      team.addExternalReference(ExternalProvider.FOOTBALL_DATA, "team-source");

      // Assert
      assertThat(league.getExternalReferences()).hasSize(1);
      assertThat(team.getExternalReferences()).hasSize(1);
      assertThatThrownBy(() -> league.addExternalReference(ExternalProvider.FOOTBALL_DATA, "other"))
          .isInstanceOf(InvalidCatalogEntityException.class);
      assertThatThrownBy(() -> team.addExternalReference(ExternalProvider.FOOTBALL_DATA, "other"))
          .isInstanceOf(InvalidCatalogEntityException.class);
      assertThat(team.getExternalReferences())
          .extracting(TeamExternalReference::getExternalId)
          .containsExactly("team-source");
      assertThat(league.getExternalReferences())
          .extracting(LeagueExternalReference::getExternalId)
          .containsExactly("league-source");
    }

    @Test
    @DisplayName("Un equipo admite identidades independientes de ambos proveedores")
    void separateProviders() {
      // Arrange
      League league = new League("Liga");
      Team team = new Team("Equipo", league, true);

      // Act
      team.addExternalReference(ExternalProvider.FOOTBALL_DATA, "source");
      team.addExternalReference(ExternalProvider.THE_SPORTS_DB, "source");

      // Assert
      assertThat(team.getExternalReferences())
          .extracting(TeamExternalReference::getProvider)
          .containsExactly(ExternalProvider.FOOTBALL_DATA, ExternalProvider.THE_SPORTS_DB);
      assertThatThrownBy(() -> team.getExternalReferences().clear())
          .isInstanceOf(UnsupportedOperationException.class);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t"})
    @DisplayName("Una identidad externa vacía no se incorpora a equipo ni liga")
    void rejectEmptyIdentity(String externalId) {
      // Arrange
      League league = new League("Liga");
      Team team = new Team("Equipo", league, true);

      // Act / Assert
      assertThatThrownBy(
              () -> team.addExternalReference(ExternalProvider.FOOTBALL_DATA, externalId))
          .isInstanceOf(InvalidCatalogEntityException.class);
      assertThatThrownBy(
              () -> league.addExternalReference(ExternalProvider.FOOTBALL_DATA, externalId))
          .isInstanceOf(InvalidCatalogEntityException.class);
      assertThat(team.getExternalReferences()).isEmpty();
      assertThat(league.getExternalReferences()).isEmpty();
    }
  }
}
