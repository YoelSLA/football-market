package footballmarket.features.market.infraestructure;

import static org.assertj.core.api.Assertions.assertThat;

import footballmarket.features.auth.domain.User;
import footballmarket.features.catalog.domain.models.player.Player;
import footballmarket.features.market.domain.enums.TokenListingStatus;
import footballmarket.features.market.domain.models.MarketTransaction;
import footballmarket.features.market.domain.models.TokenListing;
import footballmarket.features.market.infraestructure.projections.BusinessTransactionTotalsProjection;
import footballmarket.features.quote.domain.records.CurrentQuote;
import footballmarket.support.MarketTestData;
import footballmarket.support.TestcontainersConfiguration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/** Comprueba las agregaciones históricas contra la persistencia PostgreSQL real. */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
@Tag("integration")
@Transactional
class BusinessMetricsRepositoryTest {
  @Autowired private MarketTransactionRepository transactions;
  @Autowired private TokenListingRepository listings;
  @Autowired private MarketTestData testData;

  @Nested
  @DisplayName("Agregación de transacciones históricas")
  class FinancialTotals {
    @Test
    @DisplayName("Clasifica compras P2P y recompras sin duplicar volúmenes")
    void classifiesFinancialRowsAndAddsEachVolumeOnce() {
      // Arrange
      User buyer = testData.registerBuyer();
      User seller = testData.registerBuyer();
      User superuser = testData.registerSuperuser();
      Player player = testData.createVisiblePlayer();
      OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
      transactions.save(transaction(buyer, seller, player, 7, 700, now));
      transactions.save(
          MarketTransaction.recordBuyback(
              superuser,
              seller,
              player,
              3,
              100,
              10,
              90,
              270,
              0,
              270,
              0,
              270,
              10,
              7,
              3,
              6,
              now,
              "strategy-v1",
              now));
      transactions.flush();

      // Act
      BusinessTransactionTotalsProjection totals = transactions.aggregateBusinessMetrics();

      // Assert
      assertThat(totals.getPurchases()).isEqualTo(1L);
      assertThat(totals.getSales()).isEqualTo(1L);
      assertThat(totals.getTokensTransferred()).isEqualTo(10L);
      assertThat(totals.getCreditsTradedInCents()).isEqualTo(970L);
    }

    @Test
    @DisplayName("Devuelve ceros cuando no hay transacciones financieras")
    void returnsZeroTotalsWhenHistoryIsEmpty() {
      // Act
      BusinessTransactionTotalsProjection totals = transactions.aggregateBusinessMetrics();

      // Assert
      assertThat(totals.getPurchases()).isZero();
      assertThat(totals.getSales()).isZero();
      assertThat(totals.getTokensTransferred()).isZero();
      assertThat(totals.getCreditsTradedInCents()).isZero();
    }
  }

  @Nested
  @DisplayName("Publicaciones canceladas")
  class CancelledListings {
    @Test
    @DisplayName("Cuenta canceladas y excluye publicaciones activas ya vencidas")
    void countsCancelledListingsAndExcludesExpiredOnes() {
      // Arrange
      User seller = testData.registerBuyer();
      Player player = testData.createVisiblePlayer();
      OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
      CurrentQuote quote = new CurrentQuote(player.getId(), 100L, now, "strategy-v1");
      TokenListing cancelled =
          TokenListing.publish(seller, player, 2, quote, now.minusDays(2), now.plusDays(1));
      cancelled.markCancelled(now.minusDays(1));
      listings.save(cancelled);
      TokenListing expired =
          TokenListing.publish(seller, player, 2, quote, now.minusDays(2), now.minusDays(1));
      listings.save(expired);
      listings.flush();

      // Act
      long cancelledCount = listings.countByStatus(TokenListingStatus.CANCELLED);

      // Assert
      assertThat(cancelledCount).isEqualTo(1L);
    }
  }

  private static MarketTransaction transaction(
      User buyer, User seller, Player player, int quantity, long total, OffsetDateTime executedAt) {
    return MarketTransaction.record(
        buyer,
        seller,
        player,
        quantity,
        total / quantity,
        total,
        10000,
        10000 - total,
        0,
        total,
        0,
        quantity,
        100,
        100 - quantity,
        executedAt,
        "strategy-v1",
        executedAt);
  }
}
