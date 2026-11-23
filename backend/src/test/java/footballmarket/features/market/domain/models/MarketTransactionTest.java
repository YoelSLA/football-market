package footballmarket.features.market.domain.models;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import footballmarket.features.auth.domain.User;
import footballmarket.features.catalog.domain.models.player.Player;
import footballmarket.features.market.domain.exceptions.InvalidBuybackTransactionException;
import footballmarket.support.TestFixtures;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Invariantes del registro financiero de una venta oficial, sin Spring ni persistencia.
 *
 * <p>Comprueba que el comprobante conserva la cotización de referencia y la comisión aplicada y que
 * rechaza combinaciones económicas imposibles sin construir un registro parcial.
 */
@Tag("unit")
class MarketTransactionTest {

  private static final OffsetDateTime NOW = OffsetDateTime.parse("2026-11-22T10:30:00Z");
  private static final long QUOTE_PRICE = 100L;

  private User buyer() {
    return TestFixtures.superuser(1L);
  }

  private User seller() {
    return TestFixtures.user(42L);
  }

  private Player player() {
    return TestFixtures.player(7821L);
  }

  private MarketTransaction buyback(
      int quantity, long quotePrice, int discountPercentage, long unitPrice, long totalAmount) {
    return MarketTransaction.recordBuyback(
        buyer(),
        seller(),
        player(),
        quantity,
        quotePrice,
        discountPercentage,
        unitPrice,
        totalAmount,
        10_000L,
        9_550L,
        0L,
        450L,
        0,
        quantity,
        quantity + 5,
        5,
        NOW,
        "strategy-v1",
        NOW);
  }

  @Nested
  @DisplayName("Registro de una venta oficial")
  class BuybackRecord {

    @Test
    @DisplayName("Conserva partes, jugador, cotización, comisión, neto y estados financieros")
    void conservaLaAuditoriaCompleta() {
      // Act
      MarketTransaction transaction = buyback(5, QUOTE_PRICE, 10, 90L, 450L);

      // Assert
      assertThat(transaction.getBuyer().getId()).isEqualTo(buyer().getId());
      assertThat(transaction.getSeller().getId()).isEqualTo(seller().getId());
      assertThat(transaction.getPlayer().getId()).isEqualTo(player().getId());
      assertThat(transaction.getQuantity()).isEqualTo(5);
      assertThat(transaction.getQuotePrice()).isEqualTo(QUOTE_PRICE);
      assertThat(transaction.getDiscountPercentage()).isEqualTo((short) 10);
      assertThat(transaction.getUnitPrice()).isEqualTo(90L);
      assertThat(transaction.getTotalAmount()).isEqualTo(450L);
      assertThat(transaction.getBuyerCreditsBefore()).isEqualTo(10_000L);
      assertThat(transaction.getBuyerCreditsAfter()).isEqualTo(9_550L);
      assertThat(transaction.getSellerCreditsBefore()).isZero();
      assertThat(transaction.getSellerCreditsAfter()).isEqualTo(450L);
      assertThat(transaction.getBuyerTokensBefore()).isZero();
      assertThat(transaction.getBuyerTokensAfter()).isEqualTo(5);
      assertThat(transaction.getSellerTokensBefore()).isEqualTo(10);
      assertThat(transaction.getSellerTokensAfter()).isEqualTo(5);
      assertThat(transaction.getQuoteCalculatedAt()).isEqualTo(NOW);
      assertThat(transaction.getQuoteStrategyVersion()).isEqualTo("strategy-v1");
      assertThat(transaction.getExecutedAt()).isEqualTo(NOW);
    }

    @Test
    @DisplayName("Acepta la comisión mínima del 10%")
    void aceptaComisionMinima() {
      // Act
      MarketTransaction transaction = buyback(1, QUOTE_PRICE, 10, 90L, 90L);

      // Assert
      assertThat(transaction.getDiscountPercentage()).isEqualTo((short) 10);
      assertThat(transaction.getUnitPrice()).isEqualTo(90L);
    }

    @Test
    @DisplayName("Acepta la comisión máxima del 99%")
    void aceptaComisionMaxima() {
      // Act
      MarketTransaction transaction = buyback(1, 10_000L, 99, 100L, 100L);

      // Assert
      assertThat(transaction.getDiscountPercentage()).isEqualTo((short) 99);
      assertThat(transaction.getUnitPrice()).isEqualTo(100L);
    }

    @Test
    @DisplayName("Rechaza el precio neto redondeado a cero")
    void rechazaPrecioNetoRedondeadoACero() {
      // Act / Assert
      assertThatThrownBy(() -> buyback(1, 1L, 99, 0L, 0L))
          .isInstanceOf(InvalidBuybackTransactionException.class);
    }
  }

  @Nested
  @DisplayName("Invariantes económicas de la venta oficial")
  class BuybackInvariants {

    @Test
    @DisplayName("Rechaza una cantidad no positiva")
    void rechazaCantidadNoPositiva() {
      // Act / Assert
      assertThatThrownBy(() -> buyback(0, QUOTE_PRICE, 10, 90L, 0L))
          .isInstanceOf(InvalidBuybackTransactionException.class);
    }

    @Test
    @DisplayName("Rechaza una cotización de referencia no positiva")
    void rechazaCotizacionNoPositiva() {
      // Act / Assert
      assertThatThrownBy(() -> buyback(1, 0L, 10, 90L, 90L))
          .isInstanceOf(InvalidBuybackTransactionException.class);
    }

    @Test
    @DisplayName("Rechaza una comisión fuera del rango inclusivo 10–99")
    void rechazaComisionFueraDeRango() {
      // Act / Assert
      assertThatThrownBy(() -> buyback(1, QUOTE_PRICE, 9, 90L, 90L))
          .isInstanceOf(InvalidBuybackTransactionException.class);
      assertThatThrownBy(() -> buyback(1, QUOTE_PRICE, 100, 90L, 90L))
          .isInstanceOf(InvalidBuybackTransactionException.class);
    }

    @Test
    @DisplayName("Rechaza un precio neto no positivo")
    void rechazaPrecioNetoNoPositivo() {
      // Act / Assert
      assertThatThrownBy(() -> buyback(1, QUOTE_PRICE, 10, 0L, 0L))
          .isInstanceOf(InvalidBuybackTransactionException.class);
    }

    @Test
    @DisplayName("Rechaza un importe que no es exactamente cantidad por precio neto")
    void rechazaImporteIncoherente() {
      // Act / Assert
      assertThatThrownBy(() -> buyback(5, QUOTE_PRICE, 10, 90L, 449L))
          .isInstanceOf(InvalidBuybackTransactionException.class);
    }

    @Test
    @DisplayName("Rechaza un importe que desborda el rango representable")
    void rechazaImporteDesbordado() {
      // Act / Assert
      assertThatThrownBy(() -> buyback(100, QUOTE_PRICE, 0, Long.MAX_VALUE, Long.MAX_VALUE))
          .isInstanceOf(InvalidBuybackTransactionException.class);
    }
  }

  @Nested
  @DisplayName("Compatibilidad con compras existentes")
  class ExistingPurchaseCompatibility {

    @Test
    @DisplayName("Una compra no persiste cotización de referencia ni descuento")
    void laCompraNoAplicaCamposDeVentaOficial() {
      // Act
      MarketTransaction transaction =
          MarketTransaction.record(
              buyer(),
              seller(),
              player(),
              10,
              100L,
              1_000L,
              10_000L,
              9_000L,
              0L,
              1_000L,
              0,
              10,
              100,
              90,
              NOW,
              "strategy-v1",
              NOW);

      // Assert
      assertThat(transaction.getQuotePrice()).isNull();
      assertThat(transaction.getDiscountPercentage()).isNull();
      assertThat(transaction.getUnitPrice()).isEqualTo(100L);
      assertThat(transaction.getTotalAmount()).isEqualTo(1_000L);
    }
  }
}
