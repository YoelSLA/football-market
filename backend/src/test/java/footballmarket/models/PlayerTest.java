package footballmarket.models;

import static org.assertj.core.api.Assertions.*;

import footballmarket.models.exceptions.InvalidPlayerException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class PlayerTest {

  private static Team team() {
    return new Team("Team", new League("League"), true);
  }

  @Nested
  @DisplayName("Actualización y estado del jugador")
  class Lifecycle {
    @Test
    @DisplayName("Un jugador nuevo espera un identificador interno generado y comienza activo")
    void creaJugadorActivo() {
      // Act
      Player player = new Player("Player", team(), "Forward");

      // Assert
      assertThat(player.getId()).isNull();
      assertThat(player.isActive()).isTrue();
    }

    @Test
    @DisplayName("Desactiva un jugador sin modificar su identificador")
    void desactivaJugador() {
      // Arrange
      Player player = new Player("Player", team(), "Forward");

      // Act
      player.deactivate();

      // Assert
      assertThat(player.isActive()).isFalse();
      assertThat(player.getId()).isNull();
    }

    @Test
    @DisplayName("Actualiza los datos del jugador sin cambiar su identificador")
    void actualizaJugador() {
      // Arrange
      Player player = new Player("Player", team(), "Forward");

      // Act
      League newLeague = new League("New league");
      Team newTeam = new Team("New team", newLeague, true);
      player.update("New name", newTeam, "Goalkeeper");

      // Assert
      assertThat(player.getName()).isEqualTo("New name");
      assertThat(player.getTeam()).isSameAs(newTeam);
      assertThat(player.getLeague()).isSameAs(newLeague);
      assertThat(player.getPosition()).isEqualTo("Goalkeeper");
      assertThat(player.getId()).isNull();
    }

    @Test
    @DisplayName("Reactiva un jugador previamente inactivo")
    void reactivaJugador() {
      // Arrange
      Player player = new Player("Player", team(), "Forward");
      player.deactivate();

      // Act
      player.activate();

      // Assert
      assertThat(player.isActive()).isTrue();
    }

    @Test
    @DisplayName("Rechaza asociar un jugador sin equipo resuelto")
    void rechazaAsociacionAusente() {
      // Arrange
      Player player = new Player("Player", team(), "Forward");

      // Act / Assert
      assertThatThrownBy(() -> player.associateTeam(null))
          .isInstanceOf(InvalidPlayerException.class);
    }
  }

  @Nested
  @DisplayName("Validación de los datos del jugador")
  class Validation {
    @Test
    @DisplayName("Los datos opcionales ausentes conservan los valores conocidos")
    void conservaOpcionales() {
      Player player = new Player("N", team(), "P");
      java.time.LocalDate birth = java.time.LocalDate.of(1990, 6, 20);
      player.updateOptionalDetails(birth, "Spain");
      player.updateOptionalDetails(null, " ");
      assertThat(player.getDateOfBirth()).isEqualTo(birth);
      assertThat(player.getNationality()).isEqualTo("Spain");
      assertThat(player.getImageUrl()).isNull();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t"})
    @DisplayName("Rechaza un nombre vacío o en blanco")
    void rechazaNombreInvalido(String invalid) {
      assertThatThrownBy(() -> new Player(invalid, team(), "P"))
          .isInstanceOf(InvalidPlayerException.class);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t"})
    @DisplayName("Rechaza una posición vacía o en blanco")
    void rechazaPosicionInvalida(String invalid) {
      assertThatThrownBy(() -> new Player("N", team(), invalid))
          .isInstanceOf(InvalidPlayerException.class);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t"})
    @DisplayName("Una actualización inválida no modifica parcialmente al jugador")
    void conservaDatosTrasActualizacionInvalida(String invalid) {
      // Arrange
      Player player = new Player("N", team(), "P");

      // Act / Assert
      assertThatThrownBy(() -> player.update("Changed", team(), invalid))
          .isInstanceOf(InvalidPlayerException.class);

      // Verify
      assertThat(player.getName()).isEqualTo("N");
    }
  }
}
