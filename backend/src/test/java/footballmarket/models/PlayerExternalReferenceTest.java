package footballmarket.models;

import static org.assertj.core.api.Assertions.*;

import footballmarket.models.enums.PlayerProvider;
import footballmarket.models.exceptions.InvalidPlayerException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class PlayerExternalReferenceTest {
  @Nested
  @DisplayName("Identidades externas del jugador")
  class References {
    @Test
    @DisplayName("El mismo identificador en dos proveedores no cambia la identidad interna")
    void distingueProveedoresSinDuplicarReferencia() {
      Player player = new Player("N", "T", "L", "P");
      player.addExternalReference(PlayerProvider.FOOTBALL_DATA, "44");
      player.addExternalReference(PlayerProvider.FOOTBALL_DATA, "44");
      player.addExternalReference(PlayerProvider.THE_SPORTS_DB, "44");
      assertThat(player.getId()).isNull();
      assertThat(player.getExternalReferences())
          .hasSize(2)
          .allSatisfy(reference -> assertThat(reference.getPlayer()).isSameAs(player));
      assertThatThrownBy(() -> player.getExternalReferences().clear())
          .isInstanceOf(UnsupportedOperationException.class);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t"})
    @DisplayName("Rechaza identidades externas vacías sin alterar las referencias")
    void rechazaIdentificadorInvalido(String externalId) {
      Player player = new Player("N", "T", "L", "P");
      assertThatThrownBy(
              () -> player.addExternalReference(PlayerProvider.FOOTBALL_DATA, externalId))
          .isInstanceOf(InvalidPlayerException.class);
      assertThat(player.getExternalReferences()).isEmpty();
    }

    @Test
    @DisplayName("Rechaza una referencia sin proveedor o jugador")
    void requiereJugadorYProveedor() {
      Player player = new Player("N", "T", "L", "P");
      assertThatThrownBy(() -> player.addExternalReference(null, "44"))
          .isInstanceOf(InvalidPlayerException.class);
      assertThatThrownBy(
              () -> new PlayerExternalReference(null, PlayerProvider.FOOTBALL_DATA, "44"))
          .isInstanceOf(InvalidPlayerException.class);
    }
  }
}
