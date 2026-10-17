package footballmarket.models;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import footballmarket.models.exceptions.InvalidCatalogEntityException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** Verifica equivalencia y selección de textos originales sin persistencia ni normalizar salida. */
class PlayerPresentationTest {
  @Nested
  @DisplayName("Selección canónica de representaciones equivalentes")
  class CanonicalSelection {
    @Test
    @DisplayName("Una representación única permanece exactamente igual, incluidos sus espacios")
    void preserveSingleOriginal() {
      // Arrange
      String original = " José Pérez ";
      List<String> observations = List.of(original);

      // Act
      String selected = PlayerPresentation.canonical(observations);

      // Assert
      assertThat(selected).isEqualTo(original);
    }

    @Test
    @DisplayName("Prioriza diacríticos antes que casing y conserva un texto recibido")
    void preferDiacritics() {
      // Arrange
      List<String> observations = List.of("JOSE PEREZ", "Jose Perez", "josé pérez");

      // Act
      String selected = PlayerPresentation.canonical(observations);

      // Assert
      assertThat(selected).isEqualTo("josé pérez").isIn(observations);
    }

    @Test
    @DisplayName(
        "Entre formas equivalentes favorece casing mixto sobre mayúsculas o minúsculas completas")
    void preferNaturalCasing() {
      // Arrange
      List<String> observations = List.of("MIDFIELDER", "midfielder", "Midfielder");

      // Act
      String selected = PlayerPresentation.canonical(observations);

      // Assert
      assertThat(selected).isEqualTo("Midfielder");
    }

    @Test
    @DisplayName(
        "El ranking del conjunto es independiente de permutaciones y utiliza el desempate original")
    void selectIndependentOfOrder() {
      // Arrange
      List<String> observations = List.of("José Pérez", "JOSE PEREZ", "Jose Perez");
      List<String> permutation = new ArrayList<>(observations);

      // Act / Assert
      for (int index = 0; index < observations.size(); index++) {
        Collections.rotate(permutation, 1);
        assertThat(PlayerPresentation.canonical(permutation)).isEqualTo("José Pérez");
        Collections.reverse(permutation);
        assertThat(PlayerPresentation.canonical(permutation)).isEqualTo("José Pérez");
      }
      assertThat(PlayerPresentation.canonical(List.of("José Perez", "Jose Pérez")))
          .isEqualTo("Jose Pérez");
      assertThat(PlayerPresentation.canonical(List.of("Jose Pérez", "José Perez")))
          .isEqualTo("Jose Pérez");
    }

    @Test
    @DisplayName(
        "Un desacuerdo semántico se rechaza, no se resuelve escogiendo la mejor presentación")
    void rejectContradictoryTexts() {
      // Arrange
      List<String> observations = List.of("José Pérez", "Juan Pérez");

      // Act / Assert
      assertThatThrownBy(() -> PlayerPresentation.canonical(observations))
          .isInstanceOf(InvalidCatalogEntityException.class);
    }
  }

  @Nested
  @DisplayName("Estabilidad de presentación del Player existente")
  class ExistingPresentation {
    @Test
    @DisplayName(
        "Una actualización equivalente conserva nombre y posición persistidos aunque lleguen formas mejores")
    void preserveExistingRepresentation() {
      // Arrange
      League league = new League("League");
      Team team = new Team("Team", league, true);
      Player player = new Player(" José Pérez ", team, "midfielder");

      // Act
      player.update("JOSE   PEREZ", team, "Midfielder");
      player.update("Jose Perez", team, "MIDFIELDER");

      // Assert
      assertThat(player.getName()).isEqualTo(" José Pérez ");
      assertThat(player.getPosition()).isEqualTo("midfielder");
    }

    @Test
    @DisplayName("Una actualización válida con significado distinto sigue cambiando los atributos")
    void updateChangedMeaning() {
      // Arrange
      League league = new League("League");
      Team team = new Team("Team", league, true);
      Player player = new Player("José Pérez", team, "Midfielder");

      // Act
      player.update("Juan Pérez", team, "Forward");

      // Assert
      assertThat(player.getName()).isEqualTo("Juan Pérez");
      assertThat(player.getPosition()).isEqualTo("Forward");
    }
  }
}
