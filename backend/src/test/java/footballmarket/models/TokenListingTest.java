package footballmarket.models;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import footballmarket.features.auth.domain.User;
import footballmarket.features.catalog.domain.models.player.Player;
import footballmarket.features.market.domain.enums.TokenListingStatus;
import footballmarket.features.market.domain.exceptions.InvalidTokenListingException;
import footballmarket.features.market.domain.models.TokenListing;
import footballmarket.features.quote.domain.records.CurrentQuote;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Invariantes puras de publicación, sin Spring ni persistencia. */
@org.junit.jupiter.api.Tag("unit")
class TokenListingTest {
  private static final OffsetDateTime NOW = OffsetDateTime.parse("2026-10-17T12:00:00Z");

  private TokenListing listing(int quantity, long unitPrice) {
    User seller = TestFixtures.user(2L);
    Player player = TestFixtures.player(1L);
    CurrentQuote quote = new CurrentQuote(1L, unitPrice, NOW, "v1");
    return TokenListing.publish(seller, player, quantity, quote, NOW, NOW.plusDays(1));
  }

  @Nested
  @DisplayName("Publicación y precio congelado")
  class Publication {
    @Test
    @DisplayName("Congela el precio y calcula el importe exacto sin transferir recursos")
    void congelaPrecio() {
      // Act
      TokenListing result = listing(7, 250);
      // Assert
      assertThat(result.getUnitPrice()).isEqualTo(250);
      assertThat(result.getTotalAmount()).isEqualTo(1750);
      assertThat(result.getStatus()).isEqualTo(TokenListingStatus.ACTIVE);
      assertThat(result.getClosedAt()).isNull();
      assertThat(result.getExpiresAt()).isEqualTo(NOW.plusDays(1));
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1, 101})
    @DisplayName("Rechaza cantidades fuera del rango de publicación")
    void rechazaCantidad(int quantity) {
      // Act / Assert
      assertThatThrownBy(() -> listing(quantity, 100))
          .isInstanceOf(InvalidTokenListingException.class);
    }

    @ParameterizedTest
    @ValueSource(longs = {0, -100})
    @DisplayName("Rechaza precios no positivos")
    void rechazaPrecio(long price) {
      // Act / Assert
      assertThatThrownBy(() -> listing(1, price)).isInstanceOf(InvalidTokenListingException.class);
    }

    @Test
    @DisplayName("No trunca un importe que excede el rango de Long")
    void detectaDesbordamiento() {
      // Act / Assert
      assertThatThrownBy(() -> listing(100, Long.MAX_VALUE))
          .isInstanceOf(ArithmeticException.class);
    }
  }

  @Nested
  @DisplayName("Vencimiento y transiciones")
  class Transitions {
    @Test
    @DisplayName("La igualdad con expiresAt ya es vencimiento, sin modificar el estado almacenado")
    void calculaFronteraExacta() {
      // Arrange
      TokenListing result = listing(5, 100);
      // Act / Assert
      assertThat(result.isActiveAndValid(NOW.plusDays(1).minusNanos(1))).isTrue();
      assertThat(result.effectiveStatus(NOW.plusDays(1))).isEqualTo(TokenListingStatus.EXPIRED);
      assertThat(result.effectiveStatus(NOW.plusDays(2))).isEqualTo(TokenListingStatus.EXPIRED);
      assertThat(result.getStatus()).isEqualTo(TokenListingStatus.ACTIVE);
      assertThat(result.getClosedAt()).isNull();
    }

    @Test
    @DisplayName("Una venta registra el cierre y no cambia a expirada posteriormente")
    void vendeUnaVez() {
      // Arrange
      TokenListing result = listing(5, 100);
      // Act
      result.markSold(NOW.plusHours(1));
      // Assert
      assertThat(result.effectiveStatus(NOW.plusDays(2))).isEqualTo(TokenListingStatus.SOLD);
      assertThat(result.getClosedAt()).isEqualTo(NOW.plusHours(1));
      assertThatThrownBy(() -> result.markSold(NOW.plusHours(2)))
          .isInstanceOf(InvalidTokenListingException.class);
      assertThatThrownBy(() -> result.markCancelled(NOW.plusHours(2)))
          .isInstanceOf(InvalidTokenListingException.class);
    }

    @Test
    @DisplayName("Cancelar es terminal y conserva el instante de cierre original")
    void cancelaUnaVez() {
      // Arrange
      TokenListing result = listing(5, 100);
      // Act
      result.markCancelled(NOW.plusHours(1));
      // Assert
      assertThat(result.getStatus()).isEqualTo(TokenListingStatus.CANCELLED);
      assertThatThrownBy(() -> result.markCancelled(NOW.plusHours(2)))
          .isInstanceOf(InvalidTokenListingException.class);
      assertThat(result.getClosedAt()).isEqualTo(NOW.plusHours(1));
    }

    @Test
    @DisplayName("Una publicación vencida no se vende ni cancela")
    void rechazaVencida() {
      // Arrange
      TokenListing result = listing(5, 100);
      // Act / Assert
      assertThatThrownBy(() -> result.markSold(NOW.plusDays(1)))
          .isInstanceOf(InvalidTokenListingException.class);
      assertThatThrownBy(() -> result.markCancelled(NOW.plusDays(1)))
          .isInstanceOf(InvalidTokenListingException.class);
      assertThat(result.getStatus()).isEqualTo(TokenListingStatus.ACTIVE);
      assertThat(result.getClosedAt()).isNull();
    }
  }
}
