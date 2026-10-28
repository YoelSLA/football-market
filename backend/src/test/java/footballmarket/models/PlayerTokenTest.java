package footballmarket.models;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import footballmarket.features.auth.domain.User;
import footballmarket.features.catalog.domain.models.player.Player;
import footballmarket.features.market.domain.exceptions.InsufficientPlayerTokensException;
import footballmarket.features.market.domain.exceptions.InvalidTokenQuantityException;
import footballmarket.features.market.domain.exceptions.TokenPlayerMismatchException;
import footballmarket.features.market.domain.models.PlayerToken;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Reglas de dominio de la tenencia de tokens: no negatividad, mismo jugador y conservación del
 * total.
 */
@org.junit.jupiter.api.Tag("unit")
class PlayerTokenTest {

  private static final long PLAYER_ID = 7821L;

  private static Player player() {
    return TestFixtures.player(PLAYER_ID);
  }

  private static User user(long id) {
    return TestFixtures.user(id);
  }

  @Nested
  @DisplayName("Construcción de la tenencia")
  class Construction {
    @Test
    @DisplayName("La tenencia emitida arranca con la cantidad emitida y la vacía con cero")
    void construyeTenenciasEmitidaYVacia() {
      // Arrange / Act
      PlayerToken emitted = PlayerToken.issued(player(), user(1L), 100);
      PlayerToken empty = PlayerToken.emptyHolding(player(), user(2L));

      // Assert
      assertThat(emitted.getQuantity()).isEqualTo(100);
      assertThat(empty.getQuantity()).isZero();
    }

    @Test
    @DisplayName("Una tenencia con cantidad negativa no es válida")
    void rechazaUnaCantidadNegativa() {
      // Act / Assert
      assertThatThrownBy(() -> PlayerToken.issued(player(), user(1L), -1))
          .isInstanceOf(InvalidTokenQuantityException.class);
    }

    @Test
    @DisplayName("Una tenencia restaurada conserva su estado persistido")
    void restauraElEstadoPersistido() {
      // Arrange
      Player player = player();
      User owner = user(9L);

      // Act
      PlayerToken restored = PlayerToken.restore(7L, player, owner, 12, null);

      // Assert
      assertThat(restored.getId()).isEqualTo(7L);
      assertThat(restored.getQuantity()).isEqualTo(12);
      assertThat(restored.getOwner()).isSameAs(owner);
      assertThat(restored.getPlayer()).isSameAs(player);
    }
  }

  @Nested
  @DisplayName("Transferencia entre tenencias")
  class Transfer {
    @Test
    @DisplayName("Transferir más de la tenencia disponible es rechazado")
    void rechazaTransferirMasDeLaTenencia() {
      // Arrange
      PlayerToken seller = PlayerToken.issued(player(), user(1L), 5);
      PlayerToken buyer = PlayerToken.emptyHolding(player(), user(2L));

      // Act / Assert
      assertThatThrownBy(() -> seller.transferTo(6, buyer))
          .isInstanceOf(InsufficientPlayerTokensException.class);
      assertThat(seller.getQuantity()).isEqualTo(5);
      assertThat(buyer.getQuantity()).isZero();
    }

    @Test
    @DisplayName("Una cantidad no positiva no representa una transferencia")
    void rechazaCantidadesNoPositivas() {
      // Arrange
      PlayerToken seller = PlayerToken.issued(player(), user(1L), 5);
      PlayerToken buyer = PlayerToken.emptyHolding(player(), user(2L));

      // Act / Assert
      assertThatThrownBy(() -> seller.transferTo(0, buyer))
          .isInstanceOf(InvalidTokenQuantityException.class);
    }

    @Test
    @DisplayName("La transferencia conserva el total entre ambos propietarios")
    void conservaElTotalEntrePropietarios() {
      // Arrange
      PlayerToken seller = PlayerToken.issued(player(), user(1L), 100);
      PlayerToken buyer = PlayerToken.emptyHolding(player(), user(2L));

      // Act
      seller.transferTo(10, buyer);

      // Assert
      assertThat(seller.getQuantity()).isEqualTo(90);
      assertThat(buyer.getQuantity()).isEqualTo(10);
      assertThat(seller.getQuantity() + buyer.getQuantity()).isEqualTo(100);
    }

    @Test
    @DisplayName("Transferir la tenencia completa la deja en cero sin dejar un resultado negativo")
    void permiteTransferirLaTenenciaCompleta() {
      // Arrange
      PlayerToken seller = PlayerToken.issued(player(), user(1L), 3);
      PlayerToken buyer = PlayerToken.emptyHolding(player(), user(2L));

      // Act
      seller.transferTo(3, buyer);

      // Assert
      assertThat(seller.getQuantity()).isZero();
      assertThat(buyer.getQuantity()).isEqualTo(3);
    }

    @Test
    @DisplayName("Transferir entre tenencias de jugadores distintos es rechazado sin mover tokens")
    void rechazaTransferirEntreJugadoresDistintos() {
      // Arrange: la suma del jugador solo se conserva si ambas tenencias son del mismo jugador.
      PlayerToken seller = PlayerToken.issued(player(), user(1L), 5);
      PlayerToken otherPlayerBuyer =
          PlayerToken.emptyHolding(TestFixtures.player(PLAYER_ID + 1), user(2L));

      // Act / Assert
      assertThatThrownBy(() -> seller.transferTo(1, otherPlayerBuyer))
          .isInstanceOf(TokenPlayerMismatchException.class);
      assertThat(seller.getQuantity()).isEqualTo(5);
      assertThat(otherPlayerBuyer.getQuantity()).isZero();
    }
  }
}
