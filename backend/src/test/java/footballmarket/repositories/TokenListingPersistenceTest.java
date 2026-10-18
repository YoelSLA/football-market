package footballmarket.repositories;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import footballmarket.models.TokenListing;
import footballmarket.services.exceptions.InsufficientStockException;
import java.sql.SQLException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.dao.DataAccessException;

/** Restricciones PostgreSQL y reservas observables desde la categoría Repository. */
class TokenListingPersistenceTest extends ListingRepositorySupport {
  @Nested
  @DisplayName("Cancelación y liberación de reservas")
  class Cancellation {
    @Test
    @DisplayName("Cancelar conserva el registro y permite volver a publicar todos los tokens")
    void liberaReservaSinTransferir() {
      // Arrange
      TokenListing listing = publish(20);
      long sellerBefore = balance(seller);
      long audits = transactions.findByPlayerIdOrderByExecutedAtDescIdDesc(player.getId()).size();
      // Act
      footballmarket.models.records.TokenListingView result =
          listingService.cancel(seller.getEmail(), listing.getId());
      // Assert
      assertThat(result.status().name()).isEqualTo("CANCELLED");
      assertThat(result.closedAt()).isEqualTo(NOW);
      // Verify
      assertThat(reserved()).isZero();
      assertThat(quantity(seller)).isEqualTo(20);
      assertThat(balance(seller)).isEqualTo(sellerBefore);
      assertThat(transactions.findByPlayerIdOrderByExecutedAtDescIdDesc(player.getId()))
          .hasSize((int) audits);
      assertThat(listings.existsById(listing.getId())).isTrue();
      TokenListing republished = publish(20);
      assertThat(republished.getId()).isNotEqualTo(listing.getId());
      assertThat(reserved()).isEqualTo(20);
      assertThat(listings.findBySellerId(seller.getId())).hasSize(2);
    }
  }

  @Nested
  @DisplayName("Consultas por estado efectivo")
  class Queries {
    @Test
    @DisplayName(
        "El mercado excluye propias, vencidas y cerradas; las propias conservan todos los estados")
    void filtraEstadoEfectivo() {
      // Arrange
      TokenListing active = publish(5);
      TokenListing sold = publish(5);
      TokenListing cancelled = publish(5);
      TokenListing expired = publish(5);
      buyService.buyListing(
          buyer.getEmail(), new footballmarket.models.records.BuyListingRequest(sold.getId()));
      transaction()
          .executeWithoutResult(
              status -> listings.lockById(cancelled.getId()).orElseThrow().markCancelled(NOW));
      jdbc.update(
          "update token_listings set created_at = ?, expires_at = ? where id = ?",
          NOW.minusDays(2),
          NOW.minusDays(1),
          expired.getId());
      // Act
      java.util.List<footballmarket.models.records.TokenListingView> publicView =
          listingService.market(buyer.getEmail());
      java.util.List<footballmarket.models.records.TokenListingView> mine =
          listingService.mine(seller.getEmail(), null);
      // Assert
      assertThat(publicView)
          .filteredOn(view -> view.playerId().equals(player.getId()))
          .extracting(footballmarket.models.records.TokenListingView::listingId)
          .containsExactly(active.getId());
      assertThat(listingService.market(seller.getEmail()))
          .extracting(footballmarket.models.records.TokenListingView::listingId)
          .doesNotContain(active.getId());
      assertThat(mine).hasSize(4);
      assertThat(listingService.mine(seller.getEmail(), "EXPIRED"))
          .extracting(footballmarket.models.records.TokenListingView::listingId)
          .containsExactly(expired.getId());
      assertThat(listingService.mine(seller.getEmail(), "SOLD"))
          .extracting(footballmarket.models.records.TokenListingView::listingId)
          .containsExactly(sold.getId());
      assertThat(listingService.mine(seller.getEmail(), "CANCELLED"))
          .extracting(footballmarket.models.records.TokenListingView::listingId)
          .containsExactly(cancelled.getId());
      // Verify
      assertThat(reserved()).isEqualTo(5);
      assertThat(listings.findById(expired.getId()).orElseThrow().getStatus().name())
          .isEqualTo("ACTIVE");
    }

    @Test
    @DisplayName("El filtro EXPIRED propio sigue incluyendo jugadores no visibles")
    void filtraSinVisibilidad() {
      // Arrange
      TokenListing expired = publish(5);
      jdbc.update(
          "update token_listings set created_at = ?, expires_at = ? where id = ?",
          NOW.minusDays(2),
          NOW.minusDays(1),
          expired.getId());
      jdbc.update("update players set active = false where id = ?", player.getId());
      // Act
      java.util.List<footballmarket.models.records.TokenListingView> mine =
          listingService.mine(seller.getEmail(), "EXPIRED");
      // Assert
      assertThat(mine)
          .extracting(footballmarket.models.records.TokenListingView::listingId)
          .containsExactly(expired.getId());
      assertThat(listingService.detail(seller.getEmail(), expired.getId()).status().name())
          .isEqualTo("EXPIRED");
      assertThatThrownBy(() -> listingService.detail(buyer.getEmail(), expired.getId()))
          .isInstanceOf(footballmarket.services.exceptions.ListingNotFoundException.class);
    }
  }

  @Nested
  @DisplayName("Rechazos secundarios sin movimientos")
  class PurchaseRejections {
    @ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings = {"SOLD", "CANCELLED", "EXPIRED"})
    @DisplayName(
        "Vendida, cancelada o vencida producen el mismo rechazo sin movimientos adicionales")
    void indistinguibilidadDeCierres(String state) {
      // Arrange
      TokenListing listing = publish(5);
      if ("SOLD".equals(state)) {
        buyService.buyListing(
            buyer.getEmail(), new footballmarket.models.records.BuyListingRequest(listing.getId()));
      } else if ("CANCELLED".equals(state)) {
        listingService.cancel(seller.getEmail(), listing.getId());
      } else {
        org.mockito.Mockito.when(marketClock.instant())
            .thenReturn(listing.getExpiresAt().toInstant());
      }
      footballmarket.models.User other = market.registerBuyer();
      long sellerBefore = balance(seller);
      int tokensBefore = quantity(seller);
      long auditsBefore =
          jdbc.queryForObject(
              "select count(*) from market_transactions where listing_id = ?",
              Long.class,
              listing.getId());
      // Act / Assert
      assertThatThrownBy(
              () ->
                  buyService.buyListing(
                      other.getEmail(),
                      new footballmarket.models.records.BuyListingRequest(listing.getId())))
          .isInstanceOf(footballmarket.services.exceptions.ListingNotAvailableException.class)
          .hasMessage("La publicación no está disponible.");
      // Verify
      assertThat(balance(seller)).isEqualTo(sellerBefore);
      assertThat(balance(other)).isEqualTo(10000);
      assertThat(quantity(seller)).isEqualTo(tokensBefore);
      assertThat(holdings.findByPlayerIdAndOwnerId(player.getId(), other.getId())).isEmpty();
      assertThat(
              jdbc.queryForObject(
                  "select count(*) from market_transactions where listing_id = ?",
                  Long.class,
                  listing.getId()))
          .isEqualTo(auditsBefore);
      assertThat(listings.findById(listing.getId()).orElseThrow().getStatus().name())
          .isEqualTo("EXPIRED".equals(state) ? "ACTIVE" : state);
    }

    @Test
    @DisplayName(
        "El saldo insuficiente del comprador revierte también la primera tenencia preparada")
    void rechazaSaldoInsuficiente() {
      // Arrange
      TokenListing listing = publish(5);
      jdbc.update("update user_credits set available_credits = 1 where user_id = ?", buyer.getId());
      long sellerBefore = balance(seller);
      // Act / Assert
      assertThatThrownBy(
              () ->
                  buyService.buyListing(
                      buyer.getEmail(),
                      new footballmarket.models.records.BuyListingRequest(listing.getId())))
          .isInstanceOf(footballmarket.services.exceptions.InsufficientCreditsException.class);
      // Verify
      assertThat(balance(buyer)).isEqualTo(1);
      assertThat(balance(seller)).isEqualTo(sellerBefore);
      assertThat(quantity(seller)).isEqualTo(20);
      assertThat(holdings.findByPlayerIdAndOwnerId(player.getId(), buyer.getId())).isEmpty();
      assertThat(listings.findById(listing.getId()).orElseThrow().getStatus().name())
          .isEqualTo("ACTIVE");
      assertThat(
              jdbc.queryForObject(
                  "select count(*) from market_transactions where listing_id = ?",
                  Long.class,
                  listing.getId()))
          .isZero();
    }

    @Test
    @DisplayName("La ausencia de cuenta del vendedor produce LISTING_NOT_AVAILABLE sin liquidación")
    void rechazaCuentaAusente() {
      // Arrange
      TokenListing listing = publish(5);
      long buyerBefore = balance(buyer);
      long audits = transactions.findByPlayerIdOrderByExecutedAtDescIdDesc(player.getId()).size();
      jdbc.update("delete from user_credits where user_id = ?", seller.getId());
      // Act / Assert
      assertThatThrownBy(
              () ->
                  buyService.buyListing(
                      buyer.getEmail(),
                      new footballmarket.models.records.BuyListingRequest(listing.getId())))
          .isInstanceOf(footballmarket.services.exceptions.ListingNotAvailableException.class)
          .hasMessage("La publicación no está disponible.");
      // Verify
      assertThat(quantity(seller)).isEqualTo(20);
      assertThat(quantity(buyer)).isZero();
      assertThat(balance(buyer)).isEqualTo(buyerBefore);
      assertThat(transactions.findByPlayerIdOrderByExecutedAtDescIdDesc(player.getId()))
          .hasSize((int) audits);
      assertThat(listings.findById(listing.getId()).orElseThrow().getStatus().name())
          .isEqualTo("ACTIVE");
    }

    @Test
    @DisplayName("La tenencia excepcionalmente reducida no vende ni mueve recursos")
    void rechazaTenenciaInsuficiente() {
      // Arrange
      TokenListing listing = publish(5);
      jdbc.update(
          "update player_tokens set quantity = 4 where player_id = ? and owner_user_id = ?",
          player.getId(),
          seller.getId());
      long sellerBefore = balance(seller);
      long buyerBefore = balance(buyer);
      long audits = transactions.findByPlayerIdOrderByExecutedAtDescIdDesc(player.getId()).size();
      // Act / Assert
      assertThatThrownBy(
              () ->
                  buyService.buyListing(
                      buyer.getEmail(),
                      new footballmarket.models.records.BuyListingRequest(listing.getId())))
          .isInstanceOf(InsufficientStockException.class);
      // Verify
      assertThat(quantity(seller)).isEqualTo(4);
      assertThat(quantity(buyer)).isZero();
      assertThat(balance(seller)).isEqualTo(sellerBefore);
      assertThat(balance(buyer)).isEqualTo(buyerBefore);
      assertThat(listings.findById(listing.getId()).orElseThrow().getStatus().name())
          .isEqualTo("ACTIVE");
      assertThat(transactions.findByPlayerIdOrderByExecutedAtDescIdDesc(player.getId()))
          .hasSize((int) audits);
    }
  }

  @Nested
  @DisplayName("Reservas de publicaciones")
  class Reservations {
    @Test
    @DisplayName("Publicar congela precio e importe y no altera tenencias, créditos ni auditoría")
    void reservaSinTransferir() {
      // Arrange
      java.util.List<java.util.Map<String, Object>> allCreditsBefore =
          jdbc.queryForList("select * from user_credits order by user_id");
      long sellerBefore = balance(seller);
      long buyerBefore = balance(buyer);
      long auditsBefore =
          transactions.findByPlayerIdOrderByExecutedAtDescIdDesc(player.getId()).size();
      // Act
      TokenListing result = publish(5);
      // Assert
      assertThat(result.getTotalAmount()).isEqualTo(500);
      assertThat(result.getUnitPrice()).isEqualTo(100);
      assertThat(result.getExpiresAt()).isEqualTo(NOW.plusDays(1));
      // Verify
      assertThat(quantity(seller)).isEqualTo(20);
      assertThat(balance(seller)).isEqualTo(sellerBefore);
      assertThat(balance(buyer)).isEqualTo(buyerBefore);
      assertThat(jdbc.queryForList("select * from user_credits order by user_id"))
          .isEqualTo(allCreditsBefore);
      assertThat(reserved()).isEqualTo(5);
      assertThat(transactions.findByPlayerIdOrderByExecutedAtDescIdDesc(player.getId()))
          .hasSize((int) auditsBefore);
    }

    @Test
    @DisplayName("Varias publicaciones coexisten y el rechazo por exceso conserva las reservas")
    void respetaDisponibilidad() {
      // Arrange
      TokenListing first = publish(5);
      TokenListing second = publish(5);
      // Act / Assert
      assertThatThrownBy(() -> publish(11)).isInstanceOf(InsufficientStockException.class);
      // Verify
      assertThat(listings.findBySellerId(seller.getId()))
          .extracting(TokenListing::getId)
          .containsExactlyInAnyOrder(first.getId(), second.getId());
      assertThat(reserved()).isEqualTo(10);
      assertThat(quantity(seller)).isEqualTo(20);
    }

    @Test
    @DisplayName(
        "Una publicación vencida en la frontera exacta deja de reservar sin escribir su estado")
    void expiraReserva() {
      // Arrange
      TokenListing result = publish(20);
      // Act
      long remaining =
          listings.reservedQuantity(seller.getId(), player.getId(), result.getExpiresAt());
      // Assert
      assertThat(remaining).isZero();
      // Verify
      assertThat(listings.findById(result.getId()).orElseThrow().getStatus().name())
          .isEqualTo("ACTIVE");
    }
  }

  @Nested
  @DisplayName("Restricciones e historial sin cascadas")
  class Constraints {
    @ParameterizedTest
    @CsvSource({"quantity,0", "unit_price,0", "total_amount,1", "status,'EXPIRED'"})
    @DisplayName("El esquema rechaza datos incompatibles con las invariantes almacenadas")
    void rechazaRestricciones(String column, String value) {
      // Arrange
      TokenListing result = publish(5);
      Object parameter = "status".equals(column) ? value : Long.valueOf(value);
      // Act
      DataAccessException failure =
          org.assertj.core.api.Assertions.catchThrowableOfType(
              () ->
                  jdbc.update(
                      "update token_listings set " + column + " = ? where id = ?",
                      parameter,
                      result.getId()),
              DataAccessException.class);
      // Assert
      assertThat(failure).isNotNull();
      assertThat(failure.getMostSpecificCause()).isInstanceOf(SQLException.class);
      assertThat(((SQLException) failure.getMostSpecificCause()).getSQLState()).isEqualTo("23514");
    }

    @Test
    @DisplayName("Las claves foráneas de publicación no permiten borrar usuario ni jugador")
    void conservaReferencias() {
      // Arrange
      footballmarket.models.User isolatedSeller = market.registerBuyer();
      footballmarket.models.Player isolatedPlayer = market.createVisiblePlayer();
      jdbc.update("delete from user_credits where user_id = ?", isolatedSeller.getId());
      footballmarket.models.records.CurrentQuote quote =
          new footballmarket.models.records.CurrentQuote(
              isolatedPlayer.getId(), 100L, NOW, "fixture-v1");
      TokenListing result =
          listings.saveAndFlush(
              TokenListing.publish(isolatedSeller, isolatedPlayer, 5, quote, NOW, NOW.plusDays(1)));
      // Act / Assert
      assertThatThrownBy(
              () -> jdbc.update("delete from users where id = ?", isolatedSeller.getId()))
          .isInstanceOf(DataAccessException.class);
      assertThatThrownBy(
              () -> jdbc.update("delete from players where id = ?", isolatedPlayer.getId()))
          .isInstanceOf(DataAccessException.class);
      // Verify
      assertThat(listings.existsById(result.getId())).isTrue();
      assertThat(
              jdbc.queryForObject(
                  "select count(*) from pg_constraint where conrelid = 'token_listings'::regclass and contype = 'f' and confdeltype = 'a'",
                  Long.class))
          .isEqualTo(2L);
    }

    @Test
    @DisplayName("PostgreSQL exige un vencimiento posterior a la creación")
    void rechazaVencimientoInvalido() {
      // Arrange
      TokenListing result = publish(5);
      // Act
      DataAccessException failure =
          org.assertj.core.api.Assertions.catchThrowableOfType(
              () ->
                  jdbc.update(
                      "update token_listings set expires_at = created_at where id = ?",
                      result.getId()),
              DataAccessException.class);
      // Assert
      assertThat(failure).isNotNull();
      assertThat(failure.getMostSpecificCause()).isInstanceOf(SQLException.class);
      assertThat(((SQLException) failure.getMostSpecificCause()).getSQLState()).isEqualTo("23514");
    }
  }
}
