package footballmarket.models.enums;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import footballmarket.features.catalog.domain.exceptions.InvalidPlayerException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@org.junit.jupiter.api.Tag("unit")
class GeneralPositionTest {
  @Nested
  @DisplayName("Traducción de la posición persistida por el catálogo")
  class Translation {
    @Test
    @DisplayName("Reconoce las denominaciones del catálogo")
    void recognisesCatalogueDenominations() {
      assertThat(GeneralPosition.fromPlayerPosition("Goalkeeper"))
          .isEqualTo(GeneralPosition.GOALKEEPER);
      assertThat(GeneralPosition.fromPlayerPosition("Defence")).isEqualTo(GeneralPosition.DEFENDER);
      assertThat(GeneralPosition.fromPlayerPosition("Midfield"))
          .isEqualTo(GeneralPosition.MIDFIELDER);
      assertThat(GeneralPosition.fromPlayerPosition("Offence")).isEqualTo(GeneralPosition.FORWARD);
    }

    @Test
    @DisplayName("Reconoce los equivalentes en inglés de las cuatro posiciones")
    void recognisesEnglishDenominations() {
      assertThat(GeneralPosition.fromPlayerPosition("Goalkeeper"))
          .isEqualTo(GeneralPosition.GOALKEEPER);
      assertThat(GeneralPosition.fromPlayerPosition("Defender"))
          .isEqualTo(GeneralPosition.DEFENDER);
      assertThat(GeneralPosition.fromPlayerPosition("Midfielder"))
          .isEqualTo(GeneralPosition.MIDFIELDER);
      assertThat(GeneralPosition.fromPlayerPosition("Forward")).isEqualTo(GeneralPosition.FORWARD);
    }

    @Test
    @DisplayName("Rechaza una posición ausente, nula o desconocida")
    void rejectsUnknownPositions() {
      assertThatThrownBy(() -> GeneralPosition.fromPlayerPosition(null))
          .isInstanceOf(InvalidPlayerException.class);
      assertThatThrownBy(() -> GeneralPosition.fromPlayerPosition("Left Winger"))
          .isInstanceOf(InvalidPlayerException.class);
    }
  }
}
