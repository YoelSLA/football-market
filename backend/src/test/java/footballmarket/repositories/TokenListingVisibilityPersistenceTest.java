package footballmarket.repositories;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import footballmarket.models.TokenListing;
import footballmarket.models.records.BuyListingRequest;
import footballmarket.models.records.TokenListingView;
import footballmarket.services.exceptions.ListingNotFoundException;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** La pérdida de visibilidad no borra ni cierra publicaciones ni las revela a terceros. */
@org.junit.jupiter.api.Tag("integration")
class TokenListingVisibilityPersistenceTest extends ListingRepositorySupport {
  @Nested
  @DisplayName("Pérdida de visibilidad posterior a publicar")
  class Visibility {
    @Test
    @DisplayName(
        "Desactivar el jugador oculta su publicación a terceros pero conserva el acceso del propietario")
    void conservaHistorialPropio() {
      // Arrange
      TokenListing listing = publish(5);
      transaction()
          .executeWithoutResult(
              status -> {
                footballmarket.models.Player stored =
                    players.findById(player.getId()).orElseThrow();
                stored.deactivate();
                players.saveAndFlush(stored);
              });
      // Act
      List<TokenListingView> marketView = listingService.market(buyer.getEmail());
      List<TokenListingView> mine = listingService.mine(seller.getEmail(), "ACTIVE");
      TokenListingView detail = listingService.detail(seller.getEmail(), listing.getId());
      // Assert
      assertThat(marketView)
          .extracting(TokenListingView::listingId)
          .doesNotContain(listing.getId());
      assertThat(mine).extracting(TokenListingView::listingId).contains(listing.getId());
      assertThat(detail.listingId()).isEqualTo(listing.getId());
      assertThatThrownBy(() -> listingService.detail(buyer.getEmail(), listing.getId()))
          .isInstanceOf(ListingNotFoundException.class);
      assertThatThrownBy(
              () -> buyService.buyListing(buyer.getEmail(), new BuyListingRequest(listing.getId())))
          .isInstanceOf(ListingNotFoundException.class);
      // Verify
      assertThat(listings.findById(listing.getId()).orElseThrow().getStatus().name())
          .isEqualTo("ACTIVE");
      assertThat(reserved()).isEqualTo(5);
      assertThat(quantity(seller)).isEqualTo(20);
      assertThat(quantity(buyer)).isZero();
    }
  }
}
