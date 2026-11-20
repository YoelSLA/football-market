package footballmarket.features.catalog.domain.records;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import footballmarket.features.catalog.domain.exceptions.InvalidPlayerException;
import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

@org.junit.jupiter.api.Tag("unit")
class PlayerCandidateTest {

  @Nested
  @DisplayName("Validación de campos obligatorios")
  class Validations {

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t", "null", "NULL", "Null", " null "})
    @DisplayName("Rechaza un candidato con posición inválida, en blanco o literal null")
    void rechazaCandidatoConPosicionInvalida(String invalidPosition) {
      assertThatThrownBy(
              () ->
                  new PlayerCandidate(
                      "ext-1",
                      "Player",
                      "Team",
                      "League",
                      invalidPosition,
                      LocalDate.of(2000, 1, 1),
                      "Spain",
                      "team-1"))
          .isInstanceOf(InvalidPlayerException.class);
    }

    @Test
    @DisplayName("Crea un candidato válido y normaliza espacios en la posición")
    void creaCandidatoValidoYNormalizaEspacios() {
      PlayerCandidate candidate =
          new PlayerCandidate(
              "ext-1",
              "Player",
              "Team",
              "League",
              "  Forward  ",
              LocalDate.of(2000, 1, 1),
              "Spain",
              "team-1");

      assertThat(candidate.position()).isEqualTo("Forward");
      assertThat(candidate.nationality()).isEqualTo("Spain");
    }
  }
}
