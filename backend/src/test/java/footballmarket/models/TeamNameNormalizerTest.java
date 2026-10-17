package footballmarket.models;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

/** Regresión de la comparación estricta de nombres usada por el dominio de equipos. */
class TeamNameNormalizerTest {
  @Nested
  @DisplayName("Normalización estricta de nombres")
  class StrictNormalization {
    @ParameterizedTest
    @ValueSource(strings = {"Arsenal", "ARSENAL", "  arsenal  ", "Ársenal"})
    @DisplayName("Ignora mayúsculas, diacríticos y espacios externos")
    void normalizaVariantes(String name) {
      // Act
      String normalized = TeamNameNormalizer.normalize(name);

      // Assert
      assertThat(normalized).isEqualTo("ARSENAL");
    }

    @ParameterizedTest
    @ValueSource(strings = {"Real   Madrid", "Real\tMadrid", "Real\nMadrid", "Real\u00a0Madrid"})
    @DisplayName("Colapsa espacios internos sin eliminar palabras")
    void colapsaEspacios(String name) {
      // Act
      String normalized = TeamNameNormalizer.normalize(name);

      // Assert
      assertThat(normalized).isEqualTo("REAL MADRID");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t\n", "\u00a0"})
    @DisplayName("Un nombre ausente produce una clave vacía, no una identidad inventada")
    void conservaAusencia(String name) {
      // Act
      String normalized = TeamNameNormalizer.normalize(name);

      // Assert
      assertThat(normalized).isEmpty();
    }

    @Test
    @DisplayName("No retira sufijos ni aplica comparación aproximada")
    void conservaSufijosYPuntuacion() {
      // Act
      String plain = TeamNameNormalizer.normalize("Arsenal");
      String suffix = TeamNameNormalizer.normalize("Arsenal FC");
      String punctuation = TeamNameNormalizer.normalize("Arsenal-FC");

      // Assert
      assertThat(suffix).isEqualTo("ARSENAL FC").isNotEqualTo(plain);
      assertThat(punctuation).isEqualTo("ARSENAL-FC").isNotEqualTo(suffix);
    }

    @Test
    @DisplayName("Usa mayúsculas independientes del locale y admite diacríticos descompuestos")
    void normalizaUnicode() {
      // Act
      String normalized = TeamNameNormalizer.normalize("i Á A\u0301");

      // Assert
      assertThat(normalized).isEqualTo("I A A");
    }
  }
}
