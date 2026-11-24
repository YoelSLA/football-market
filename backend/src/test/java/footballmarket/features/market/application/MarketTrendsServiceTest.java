package footballmarket.features.market.application;

import static org.assertj.core.api.Assertions.assertThat;

import footballmarket.features.auth.domain.User;
import footballmarket.features.catalog.domain.models.player.Player;
import footballmarket.features.market.domain.models.MarketTransaction;
import footballmarket.features.market.domain.records.MarketTrends;
import footballmarket.features.market.infraestructure.MarketTransactionRepository;
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

@SpringBootTest
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
@Tag("integration")
@Transactional
class MarketTrendsServiceTest {
  @Autowired private MarketTrendsService marketTrendsService;
  @Autowired private MarketTransactionRepository transactions;
  @Autowired private MarketTestData testData;

  @Nested
  @DisplayName("Consulta de métricas agregadas")
  class AggregatedTrends {
    @Test
    @DisplayName("Distingue ausencia de actividad y calcula el porcentaje por conteo")
    void distinguishesNoActivityFromBalancedTransactions() {
      // Arrange
      MarketTrends empty = marketTrendsService.getMarketTrends();
      User firstBuyer = testData.registerBuyer();
      User secondBuyer = testData.registerBuyer();
      User superuser = testData.registerSuperuser();
      Player player = testData.createVisiblePlayer();
      OffsetDateTime executedAt = OffsetDateTime.now(ZoneOffset.UTC).minusMinutes(1);
      transactions.save(transaction(firstBuyer, superuser, player, 10, 1000, executedAt));
      transactions.save(
          MarketTransaction.recordBuyback(
              superuser,
              secondBuyer,
              player,
              2,
              100,
              10,
              90,
              180,
              0,
              180,
              0,
              180,
              10,
              8,
              2,
              4,
              executedAt,
              "strategy-v1",
              executedAt));

      // Act
      MarketTrends active = marketTrendsService.getMarketTrends();

      // Assert
      assertThat(empty.topHotPlayers()).isEmpty();
      assertThat(empty.totalTransactions()).isZero();
      assertThat(empty.buyTransactionsPercentage()).isNull();
      assertThat(empty.sellTransactionsPercentage()).isNull();
      assertThat(empty.marketBias()).isNull();
      assertThat(active.buyTransactionsPercentage()).isEqualTo(50.0);
      assertThat(active.sellTransactionsPercentage()).isEqualTo(50.0);
      assertThat(active.marketBias()).isEqualTo("NEUTRAL");
      assertThat(active.totalTradedTokens()).isEqualTo(12L);
      assertThat(active.topHotPlayers()).hasSize(1);
      assertThat(active.startsAt()).isEqualTo(active.endsAt().minusHours(24));
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
