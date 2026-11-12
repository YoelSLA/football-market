package footballmarket.features.market.infraestructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import footballmarket.features.catalog.application.exceptions.PlayerNotVisibleException;
import footballmarket.features.catalog.domain.models.player.Player;
import footballmarket.features.market.application.exceptions.ListingNotFoundException;
import footballmarket.features.market.domain.models.TokenListing;
import footballmarket.features.market.domain.records.BuyListingRequest;
import footballmarket.features.market.domain.records.BuyOrderRequest;
import footballmarket.features.market.domain.records.SellOrderRequest;
import footballmarket.features.market.domain.records.TokenListingView;
import footballmarket.features.statistics.domain.enums.MetricSource;
import footballmarket.features.statistics.domain.enums.PlayerExtractionResult;
import footballmarket.support.PlayerStatisticsQueryData;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/** La pérdida de visibilidad no borra ni cierra publicaciones ni las revela a terceros. */
@org.junit.jupiter.api.Tag("integration")
class TokenListingVisibilityPersistenceTest extends ListingRepositorySupport {
  @Autowired private PlayerStatisticsQueryData statistics;

  @Nested
  @DisplayName("Pérdida de visibilidad posterior a publicar")
  class Visibility {
    @Test
    @DisplayName("Una última fecha sin métricas oculta el mercado y preserva la publicación propia")
    void hidesListingWhenLatestMetricsAreEmpty() {
      // Arrange
      TokenListing listing = publish(5);
      statistics.row(
          player.getId(),
          LocalDate.of(2026, 10, 18),
          MetricSource.JSON,
          "2026",
          "1",
          PlayerExtractionResult.COMPLETED,
          Map.of());
      long buyerBalance = balance(buyer);
      long sellerBalance = balance(seller);
      long operationCount = transactions.count();

      // Act / Assert
      assertThat(listingService.market(buyer.getEmail()))
          .extracting(TokenListingView::listingId)
          .doesNotContain(listing.getId());
      assertThat(listingService.mine(seller.getEmail(), "ACTIVE"))
          .extracting(TokenListingView::listingId)
          .contains(listing.getId());
      assertThat(listingService.detail(seller.getEmail(), listing.getId()).listingId())
          .isEqualTo(listing.getId());
      assertThatThrownBy(() -> listingService.detail(buyer.getEmail(), listing.getId()))
          .isInstanceOf(ListingNotFoundException.class);
      assertThatThrownBy(
              () -> buyService.buyListing(buyer.getEmail(), new BuyListingRequest(listing.getId())))
          .isInstanceOf(ListingNotFoundException.class);
      assertThatThrownBy(
              () -> buyService.buy(buyer.getEmail(), new BuyOrderRequest(player.getId(), 1)))
          .isInstanceOf(PlayerNotVisibleException.class);
      assertThatThrownBy(
              () ->
                  listingService.publish(
                      seller.getEmail(), new SellOrderRequest(player.getId(), 1)))
          .isInstanceOf(PlayerNotVisibleException.class);
      assertThat(reserved()).isEqualTo(5);
      assertThat(quantity(seller)).isEqualTo(20);
      assertThat(quantity(buyer)).isZero();
      assertThat(balance(buyer)).isEqualTo(buyerBalance);
      assertThat(balance(seller)).isEqualTo(sellerBalance);
      assertThat(transactions.count()).isEqualTo(operationCount);
      assertThat(listingService.cancel(seller.getEmail(), listing.getId()).status().name())
          .isEqualTo("CANCELLED");
      assertThat(reserved()).isZero();
    }

    @Test
    @DisplayName(
        "Desactivar el jugador oculta su publicación a terceros pero conserva el acceso del propietario")
    void conservaHistorialPropio() {
      // Arrange
      TokenListing listing = publish(5);
      transaction()
          .executeWithoutResult(
              status -> {
                Player stored = players.findById(player.getId()).orElseThrow();
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
