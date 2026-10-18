package footballmarket.repositories;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import footballmarket.models.MarketTransaction;
import footballmarket.models.TokenListing;
import footballmarket.models.records.BuyListingRequest;
import footballmarket.services.exceptions.ListingNotAvailableException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** Vencimiento determinista al validar la publicación bloqueada; no espera al reloj real. */
class TokenListingExpirationPersistenceTest extends ListingRepositorySupport {
  @Nested
  @DisplayName("Instante decisivo bajo bloqueo")
  class Expiration {
    @Test
    @DisplayName(
        "La igualdad con expiresAt rechaza la compra sin tocar cuentas, tenencias ni auditoría")
    void rechazaFrontera() {
      // Arrange
      TokenListing listing = publish(5);
      long sellerBefore = balance(seller);
      long buyerBefore = balance(buyer);
      long audits = transactions.findByPlayerIdOrderByExecutedAtDescIdDesc(player.getId()).size();
      when(marketClock.instant()).thenReturn(listing.getExpiresAt().toInstant());
      // Act / Assert
      assertThatThrownBy(
              () -> buyService.buyListing(buyer.getEmail(), new BuyListingRequest(listing.getId())))
          .isInstanceOf(ListingNotAvailableException.class);
      // Verify
      assertThat(balance(seller)).isEqualTo(sellerBefore);
      assertThat(balance(buyer)).isEqualTo(buyerBefore);
      assertThat(quantity(seller)).isEqualTo(20);
      assertThat(quantity(buyer)).isZero();
      assertThat(holdings.findByPlayerIdAndOwnerId(player.getId(), buyer.getId())).isEmpty();
      assertThat(transactions.findByPlayerIdOrderByExecutedAtDescIdDesc(player.getId()))
          .hasSize((int) audits);
      assertThat(listings.findById(listing.getId()).orElseThrow().getStatus().name())
          .isEqualTo("ACTIVE");
    }

    @Test
    @DisplayName("Una validación vigente se liquida sin una segunda lectura temporal al terminar")
    void noRevalidaAlFinal() {
      // Arrange
      TokenListing listing = publish(5);
      when(marketClock.instant())
          .thenReturn(
              listing.getExpiresAt().minusSeconds(1).toInstant(),
              listing.getExpiresAt().toInstant());
      clearInvocations(marketClock);
      // Act
      MarketTransaction result =
          buyService.buyListing(buyer.getEmail(), new BuyListingRequest(listing.getId()));
      // Assert
      assertThat(result.getListing().getId()).isEqualTo(listing.getId());
      // Verify
      assertThat(listings.findById(listing.getId()).orElseThrow().getStatus().name())
          .isEqualTo("SOLD");
      assertThat(quantity(buyer)).isEqualTo(5);
      assertThat(quantity(seller)).isEqualTo(15);
      assertThat(listings.findById(listing.getId()).orElseThrow().getClosedAt())
          .isEqualTo(listing.getExpiresAt().minusSeconds(1));
      assertThat(
              jdbc.queryForObject(
                  "select count(*) from market_transactions where listing_id = ?",
                  Long.class,
                  listing.getId()))
          .isEqualTo(1);
      verify(marketClock, times(1)).instant();
    }
  }
}
