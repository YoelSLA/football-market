package footballmarket.models;

import static org.assertj.core.api.Assertions.*;

import footballmarket.features.catalog.domain.enums.ExternalProvider;
import footballmarket.features.catalog.domain.exceptions.InvalidPlayerException;
import footballmarket.features.catalog.domain.models.league.League;
import footballmarket.features.catalog.domain.models.player.Player;
import footballmarket.features.catalog.domain.models.player.PlayerExternalReference;
import footballmarket.features.catalog.domain.models.team.Team;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

@org.junit.jupiter.api.Tag("unit")
class PlayerExternalReferenceTest {

  private static Team team() {
    return new Team("T", new League("L"), true);
  }

  @Nested
  @DisplayName("Identidades externas del jugador")
  class References {
    @Test
    @DisplayName("Canonicaliza WhoScored y rechaza otra identidad sin cambios parciales")
    void protectsWhoScoredIdentity() {
      // Arrange
      Player player = new Player("N", team(), "P");
      player.addExternalReference(ExternalProvider.WHOSCORED, "00123");
      // Act
      player.addExternalReference(ExternalProvider.WHOSCORED, "123");
      // Assert
      assertThat(player.getExternalReferences())
          .extracting(PlayerExternalReference::getExternalId)
          .containsExactly("123");
      // Act / Assert
      assertThatThrownBy(() -> player.addExternalReference(ExternalProvider.WHOSCORED, "124"))
          .isInstanceOf(InvalidPlayerException.class);
      // Verify
      assertThat(player.getExternalReferences())
          .extracting(PlayerExternalReference::getExternalId)
          .containsExactly("123");
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "-1", "1.0", "+1", "abc", "9223372036854775808"})
    @DisplayName("Rechaza IDs WhoScored no representables como entero positivo")
    void rejectsInvalidWhoScoredId(String externalId) {
      // Arrange
      Player player = new Player("N", team(), "P");
      // Act / Assert
      assertThatThrownBy(() -> player.addExternalReference(ExternalProvider.WHOSCORED, externalId))
          .isInstanceOf(InvalidPlayerException.class);
      // Verify
      assertThat(player.getExternalReferences()).isEmpty();
    }

    @Test
    @DisplayName("Preserva varias referencias distintas de los demás proveedores")
    void preservesOtherProviderCardinality() {
      // Arrange
      Player player = new Player("N", team(), "P");
      // Act
      player.addExternalReference(ExternalProvider.FOOTBALL_DATA, "1");
      player.addExternalReference(ExternalProvider.FOOTBALL_DATA, "2");
      player.addExternalReference(ExternalProvider.THE_SPORTS_DB, "3");
      player.addExternalReference(ExternalProvider.THE_SPORTS_DB, "4");
      // Assert
      assertThat(player.getExternalReferences()).hasSize(4);
    }

    @Test
    @DisplayName("La identidad WhoScored no restringe la multiplicidad de los demás proveedores")
    void preservesOtherProviderCardinalityAlongsideWhoScored() {
      // Arrange
      Player player = new Player("N", team(), "P");
      player.addExternalReference(ExternalProvider.WHOSCORED, "00123");
      // Act
      player.addExternalReference(ExternalProvider.FOOTBALL_DATA, "1");
      player.addExternalReference(ExternalProvider.FOOTBALL_DATA, "2");
      player.addExternalReference(ExternalProvider.THE_SPORTS_DB, "3");
      player.addExternalReference(ExternalProvider.THE_SPORTS_DB, "4");
      // Assert
      assertThat(player.getExternalReferences()).hasSize(5);
      // Act: la WhoScored repetida sigue siendo idempotente.
      player.addExternalReference(ExternalProvider.WHOSCORED, "123");
      // Assert
      assertThat(player.getExternalReferences())
          .filteredOn(reference -> reference.getProvider() == ExternalProvider.WHOSCORED)
          .extracting(PlayerExternalReference::getExternalId)
          .containsExactly("123");
      assertThat(player.getExternalReferences()).hasSize(5);
    }

    @Test
    @DisplayName("El mismo identificador en dos proveedores no cambia la identidad interna")
    void distingueProveedoresSinDuplicarReferencia() {
      Player player = new Player("N", team(), "P");
      player.addExternalReference(ExternalProvider.FOOTBALL_DATA, "44");
      player.addExternalReference(ExternalProvider.FOOTBALL_DATA, "44");
      player.addExternalReference(ExternalProvider.THE_SPORTS_DB, "44");
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
      Player player = new Player("N", team(), "P");
      assertThatThrownBy(
              () -> player.addExternalReference(ExternalProvider.FOOTBALL_DATA, externalId))
          .isInstanceOf(InvalidPlayerException.class);
      assertThat(player.getExternalReferences()).isEmpty();
    }

    @Test
    @DisplayName("Rechaza una referencia sin proveedor o jugador")
    void requiereJugadorYProveedor() {
      Player player = new Player("N", team(), "P");
      assertThatThrownBy(() -> player.addExternalReference(null, "44"))
          .isInstanceOf(InvalidPlayerException.class);
      assertThatThrownBy(
              () -> new PlayerExternalReference(null, ExternalProvider.FOOTBALL_DATA, "44"))
          .isInstanceOf(InvalidPlayerException.class);
    }
  }
}
