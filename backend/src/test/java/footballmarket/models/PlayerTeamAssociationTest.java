package footballmarket.models;

import static org.assertj.core.api.Assertions.*;

import footballmarket.models.enums.ExternalProvider;
import footballmarket.models.exceptions.InvalidPlayerException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class PlayerTeamAssociationTest {
  @Nested
  @DisplayName("Asociación actual y compatibilidad legacy durante V5")
  class Association {
    @Test
    @DisplayName("El jugador legacy conserva textos y actividad hasta asociarse por identidad")
    void associateLegacyPlayer() {
      // Arrange
      Player player = new Player("Jugador", "Texto anterior", "Liga anterior", "Forward");
      League league = new League("Liga actual");
      Team team = new Team("Equipo actual", league, true);
      player.deactivate();

      // Assert
      assertThat(player.getTeam()).isNull();
      assertThat(player.getLeague()).isNull();
      assertThat(player.getLegacyTeam()).isEqualTo("Texto anterior");
      assertThat(player.getLegacyLeague()).isEqualTo("Liga anterior");

      // Act
      player.associateTeam(team);

      // Assert
      assertThat(player.getTeam()).isSameAs(team);
      assertThat(player.getLeague()).isSameAs(league);
      assertThat(player.getLegacyTeam()).isEqualTo("Equipo actual");
      assertThat(player.getLegacyLeague()).isEqualTo("Liga actual");
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
      assertThat(player.getLegacyTeam()).isEqualTo("Segundo equipo");
      assertThat(player.getLegacyLeague()).isEqualTo("Segunda liga");
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
      player.refreshLegacyMirror();

      // Assert
      assertThat(player.getTeam()).isSameAs(team);
      assertThat(player.getLeague()).isSameAs(second);
      assertThat(player.getLegacyTeam()).isEqualTo("Equipo actual");
      assertThat(player.getLegacyLeague()).isEqualTo("Segunda liga");
    }

    @Test
    @DisplayName(
        "Varias referencias del mismo proveedor se conservan sin sustitución ni duplicación")
    void preserveMultipleProviderReferences() {
      // Arrange
      Player player = new Player("Jugador", "Equipo legacy", "Liga legacy", "Forward");
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
