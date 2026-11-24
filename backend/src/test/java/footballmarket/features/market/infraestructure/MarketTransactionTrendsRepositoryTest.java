package footballmarket.features.market.infraestructure;

import static org.assertj.core.api.Assertions.assertThat;

import footballmarket.features.auth.domain.User;
import footballmarket.features.catalog.domain.models.player.Player;
import footballmarket.features.market.domain.models.MarketTransaction;
import footballmarket.features.market.infraestructure.projections.MarketTrendProjection;
import footballmarket.features.market.infraestructure.projections.PlayerMarketDemandProjection;
import footballmarket.support.MarketTestData;
import footballmarket.support.TestcontainersConfiguration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
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
class MarketTransactionTrendsRepositoryTest {
  @Autowired private MarketTransactionRepository transactions;
  @Autowired private MarketTestData testData;

  @Nested
  @DisplayName("Agregaciones de tendencias y demanda")
  class TrendAggregation {
    @Test
    @DisplayName("Cuenta P2P como compra una sola vez y excluye los límites de la ventana")
    void countsPurchasesAndSalesInsideHalfOpenWindow() {
      // Arrange
      User buyer = testData.registerBuyer();
      User seller = testData.registerBuyer();
      User superuser = testData.registerSuperuser();
      Player player = testData.createVisiblePlayer();
      OffsetDateTime endsAt = OffsetDateTime.now(ZoneOffset.UTC);
      OffsetDateTime startsAt = endsAt.minusHours(24);
      OffsetDateTime inside = startsAt;
      OffsetDateTime atEnd = endsAt;
      transactions.save(transaction(buyer, seller, player, 7, 700, inside)); // compra P2P
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
              inside,
              "strategy-v1",
              inside));
      transactions.save(transaction(buyer, superuser, player, 99, 9900, atEnd));
      transactions.flush();

      // Act
      MarketTrendProjection totals = transactions.aggregateTrends(startsAt, endsAt);
      List<PlayerMarketDemandProjection> topPlayers =
          transactions.findTopBoughtPlayers(startsAt, endsAt);

      // Assert
      assertThat(totals.getTotalTransactions()).isEqualTo(2L);
      assertThat(totals.getBuyTransactions()).isEqualTo(1L);
      assertThat(totals.getSellTransactions()).isEqualTo(1L);
      assertThat(totals.getTotalTradedTokens()).isEqualTo(10L);
      assertThat(totals.getTotalTradedCredits()).isEqualTo(970L);
      assertThat(topPlayers)
          .singleElement()
          .satisfies(
              result -> {
                assertThat(result.getPlayerId()).isEqualTo(player.getId());
                assertThat(result.getBoughtTokens()).isEqualTo(7L);
                assertThat(result.getBuyVolume()).isEqualTo(700L);
              });
    }

    @Test
    @DisplayName("Limita a cinco jugadores y desempata de forma estable por identificador")
    void returnsFivePlayersWithDeterministicTieBreak() {
      // Arrange
      User buyer = testData.registerBuyer();
      User superuser = testData.registerSuperuser();
      OffsetDateTime endsAt = OffsetDateTime.now(ZoneOffset.UTC);
      OffsetDateTime startsAt = endsAt.minusHours(24);
      ArrayList<Player> players = new ArrayList<>();
      for (int index = 0; index < 6; index++) {
        Player player = testData.createVisiblePlayer();
        players.add(player);
        transactions.save(transaction(buyer, superuser, player, 5, 500, startsAt.plusHours(1)));
      }
      transactions.flush();

      // Act
      List<PlayerMarketDemandProjection> topPlayers =
          transactions.findTopBoughtPlayers(startsAt, endsAt);

      // Assert
      assertThat(topPlayers).hasSize(5);
      assertThat(topPlayers)
          .extracting(PlayerMarketDemandProjection::getPlayerId)
          .containsExactly(
              players.get(0).getId(),
              players.get(1).getId(),
              players.get(2).getId(),
              players.get(3).getId(),
              players.get(4).getId());
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
