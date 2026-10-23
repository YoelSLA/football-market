package footballmarket.models;

import static org.assertj.core.api.Assertions.*;

import footballmarket.models.enums.ExternalProvider;
import footballmarket.models.exceptions.InvalidPlayerException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@org.junit.jupiter.api.Tag("unit")
class PlayerTeamAssociationTest {
  @Nested
  @DisplayName("Asociación vigente y derivación de liga")
  class Association {
    @Test
    @DisplayName("Un jugador sin equipo no expone liga y admite asociarse por identidad")
    void associateWithoutTeam() {
      // Arrange
      League league = new League("Liga actual");
      Team team = new Team("Equipo actual", league, true);
      Player player = new Player("Jugador", team, "Forward");
      player.deactivate();

      // Assert
      assertThat(player.getTeam()).isSameAs(team);
      assertThat(player.getLeague()).isSameAs(league);

      // Act
      Team other = new Team("Otro equipo", new League("Otra liga"), true);
      player.associateTeam(other);

      // Assert
      assertThat(player.getTeam()).isSameAs(other);
      assertThat(player.getLeague()).isSameAs(other.getLeague());
      assertThat(player.isActive()).isFalse();
    }

    @Test
    @DisplayName("Una transferencia válida conserva las referencias y deriva la nueva liga")
    void transferPreservesReferences() {
      // Arrange
      League firstLeague = new League("Primera liga");
      League secondLeague = new League("Segunda liga");
      Team firstTeam = new Team("Primer equipo", firstLeague, true);
      Team secondTeam = new Team("Segundo equipo", secondLeague, true);
      Player player = new Player("Jugador", firstTeam, "Forward");
      player.addExternalReference(ExternalProvider.FOOTBALL_DATA, "player-source");
      PlayerExternalReference reference = player.getExternalReferences().getFirst();

      // Act
      player.update("Nombre actual", secondTeam, "Midfielder");

      // Assert
      assertThat(player.getTeam()).isSameAs(secondTeam);
      assertThat(player.getLeague()).isSameAs(secondLeague);
      assertThat(player.getExternalReferences()).containsExactly(reference);
    }

    @Test
    @DisplayName("La ausencia de equipo no altera parcialmente nombre ni posición")
    void invalidUpdateIsAtomic() {
      // Arrange
      League league = new League("Liga");
      Team team = new Team("Equipo", league, true);
      Player player = new Player("Original", team, "Forward");

      // Act / Assert
      assertThatThrownBy(() -> player.update("Cambio", (Team) null, "Midfielder"))
          .isInstanceOf(InvalidPlayerException.class);
      assertThat(player.getName()).isEqualTo("Original");
      assertThat(player.getPosition()).isEqualTo("Forward");
      assertThat(player.getTeam()).isSameAs(team);
    }

    @Test
    @DisplayName("Un cambio de liga del equipo no introduce una liga propia en el jugador")
    void leagueAlwaysDerivedFromTeam() {
      // Arrange
      League first = new League("Primera liga");
      League second = new League("Segunda liga");
      Team team = new Team("Equipo original", first, true);
      Player player = new Player("Jugador", team, "Forward");

      // Act
      team.update("Equipo actual", second);

      // Assert
      assertThat(player.getTeam()).isSameAs(team);
      assertThat(player.getLeague()).isSameAs(second);
    }

    @Test
    @DisplayName(
        "Varias referencias del mismo proveedor se conservan sin sustitución ni duplicación")
    void preserveMultipleProviderReferences() {
      // Arrange
      Player player =
          new Player("Jugador", new Team("Equipo", new League("Liga"), true), "Forward");
      player.addExternalReference(ExternalProvider.FOOTBALL_DATA, "player-source");

      PlayerExternalReference original = player.getExternalReferences().getFirst();

      // Act
      player.addExternalReference(ExternalProvider.FOOTBALL_DATA, "other-source");
      player.addExternalReference(ExternalProvider.FOOTBALL_DATA, "player-source");

      // Assert
      assertThat(player.getExternalReferences())
          .extracting(PlayerExternalReference::getExternalId)
          .containsExactly("player-source", "other-source");
      assertThat(player.getExternalReferences().getFirst()).isSameAs(original);
      assertThat(player.getExternalReferences())
          .allSatisfy(
              reference -> {
                assertThat(reference.getPlayer()).isSameAs(player);
                assertThat(reference.getProvider()).isEqualTo(ExternalProvider.FOOTBALL_DATA);
              });
    }
  }
}
