package footballmarket.models;

import static org.assertj.core.api.Assertions.*;

import footballmarket.models.exceptions.InvalidPlayerSnapshotException;
import footballmarket.models.exceptions.InvalidPlayerSynchronizationResultException;
import footballmarket.models.records.PlayerCandidate;
import footballmarket.models.records.PlayerSnapshot;
import footballmarket.models.records.PlayerSynchronizationResult;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class PlayerSynchronizationResultTest {

  @Nested
  @DisplayName("Contadores de sincronización")
  class Counters {
    @ParameterizedTest
    @DisplayName("Rechaza contadores negativos o superiores al total obtenido")
    @CsvSource({
      "-1,0,0,0,0",
      "1,-1,0,0,0",
      "1,0,-1,0,0",
      "1,0,0,-1,0",
      "1,0,0,0,-1",
      "1,1,1,0,0",
      "1,0,0,0,2"
    })
    void rechazaContadoresImposibles(
        int obtained, int created, int updated, int inactive, int discarded) {
      assertThatThrownBy(
              () ->
                  new PlayerSynchronizationResult(obtained, created, updated, inactive, discarded))
          .isInstanceOf(InvalidPlayerSynchronizationResultException.class);
    }

    @Test
    @DisplayName("Las inactivaciones son independientes de los registros obtenidos")
    void permiteRegistrosDuplicadosEInactivacionesIndependientes() {
      PlayerSynchronizationResult result = new PlayerSynchronizationResult(5, 1, 1, 10, 1);
      assertThat(result.markedInactive()).isEqualTo(10);
    }
  }

  @Nested
  @DisplayName("Consistencia del conjunto de jugadores")
  class Snapshot {
    @ParameterizedTest
    @CsvSource({"0,0", "1,1", "-1,0", "0,-1"})
    @DisplayName("Rechaza contadores incompatibles con los jugadores del snapshot")
    void rechazaContadoresInconsistentes(int obtained, int discarded) {
      // Arrange
      PlayerCandidate player = new PlayerCandidate("1", "N", "T", "L", "P", null, null);
      List<PlayerCandidate> players = List.of(player);

      // Act / Assert
      assertThatThrownBy(() -> new PlayerSnapshot(players, obtained, discarded))
          .isInstanceOf(InvalidPlayerSnapshotException.class);
    }

    @Test
    @DisplayName("El snapshot protege sus jugadores de cambios en la lista original")
    void copiaLaListaDeJugadores() {
      // Arrange
      PlayerCandidate player = new PlayerCandidate("1", "N", "T", "L", "P", null, null);
      ArrayList<PlayerCandidate> mutable = new ArrayList<>();
      mutable.add(player);

      // Act
      PlayerSnapshot snapshot = new PlayerSnapshot(mutable, 1, 0);
      mutable.clear();

      // Assert
      assertThat(snapshot.players()).containsExactly(player);
    }
  }
}
