package footballmarket.features.quote.infraestructure;

import static org.assertj.core.api.Assertions.assertThat;

import footballmarket.features.catalog.domain.models.player.Player;
import footballmarket.features.catalog.infraestructure.PlayerRepository;
import footballmarket.features.quote.domain.records.QuoteRankingRow;
import footballmarket.features.statistics.domain.enums.MetricSource;
import footballmarket.features.statistics.domain.enums.PlayerExtractionResult;
import footballmarket.support.IdentityCatalogueFixture;
import footballmarket.support.PlayerStatisticsQueryData;
import footballmarket.support.QuoteTestData;
import footballmarket.support.ResetPlayerCatalogListener;
import footballmarket.support.TestcontainersConfiguration;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestExecutionListeners;

@SpringBootTest
@ActiveProfiles("test")
@Import({TestcontainersConfiguration.class, IdentityCatalogueFixture.class, QuoteTestData.class})
@TestExecutionListeners(
    listeners = ResetPlayerCatalogListener.class,
    mergeMode = TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS)
@Tag("integration")
class QuoteRankingPersistenceTest {
  private static final LocalDate DATE = LocalDate.of(2026, 11, 10);
  private static final Instant NOW = Instant.parse("2026-11-10T09:00:00Z");
  @Autowired private PlayerQuoteRepository quotes;
  @Autowired private PlayerRepository players;
  @Autowired private IdentityCatalogueFixture catalogue;
  @Autowired private QuoteTestData data;
  @Autowired private PlayerStatisticsQueryData statistics;

  @Nested
  @DisplayName("Ranking SQL con posiciones globales")
  class RankingQueries {
    @Test
    @DisplayName(
        "Recalcula posiciones al excluir la última fecha vacía y admite cero de otra fuente")
    void ranksLatestEligibleMetrics() {
      // Arrange
      List<Player> population = prepareRanking();
      Player excluded = population.get(0);
      Player included = population.get(1);
      statistics.row(
          excluded.getId(),
          DATE,
          MetricSource.JSON,
          "2026",
          "1",
          PlayerExtractionResult.COMPLETED,
          Map.of());
      statistics.row(
          included.getId(),
          DATE,
          MetricSource.JSON,
          "2026",
          "1",
          PlayerExtractionResult.COMPLETED,
          Map.of());
      statistics.row(
          included.getId(),
          DATE,
          MetricSource.SCRAPING,
          "2026",
          "1",
          PlayerExtractionResult.COMPLETED,
          Map.of("goals_total", BigDecimal.ZERO));

      // Act
      List<QuoteRankingRow> ranking = quotes.findRankingPage(null, null, null, 20, 0);

      // Assert
      assertThat(ranking)
          .extracting(QuoteRankingRow::getPlayerId)
          .containsExactly(included.getId(), population.get(2).getId());
      assertThat(ranking.getFirst().getPosition()).isEqualTo(1);
      assertThat(quotes.countRanking(null, null, null)).isEqualTo(2);
      assertThat(quotes.countRanking(included.getName(), null, null)).isEqualTo(1);
      assertThat(quotes.findRankingLeagues())
          .extracting(option -> option.id())
          .doesNotContain(excluded.getLeague().getId());
    }

    @Test
    @DisplayName("Pagina por precio e ID usando solo la última cotización elegible")
    void selectsLatestAndPaginates() {
      // Arrange
      List<Player> population = prepareRanking();
      Player first = population.get(0);
      Player second = population.get(1);
      // Act
      List<QuoteRankingRow> page = quotes.findRankingPage(null, null, null, 1, 1);
      // Assert
      assertThat(page)
          .singleElement()
          .satisfies(
              row -> {
                assertThat(row.getPlayerId()).isEqualTo(second.getId());
                assertThat(row.getPosition()).isEqualTo(2);
                assertThat(row.getPrice()).isEqualTo(150);
                assertThat(row.getName()).isEqualTo(second.getName());
              });
      assertThat(quotes.findRankingPage(null, null, null, 100, 0))
          .extracting(QuoteRankingRow::getPlayerId)
          .containsExactly(first.getId(), second.getId(), population.get(2).getId());
      assertThat(quotes.countRanking(null, null, null)).isEqualTo(3);
    }

    @Test
    @DisplayName("Filtra después de numerar, escapa comodines y conserva opciones globales")
    void filtersAfterNumbering() {
      // Arrange
      List<Player> population = prepareRanking();
      Player last = population.get(2);
      last.update("Literal %_ Player", last.getTeam(), "Offence");
      players.saveAndFlush(last);
      Long league = last.getLeague().getId();
      // Act
      List<QuoteRankingRow> filtered = quotes.findRankingPage("%_", league, "offence", 20, 0);
      // Assert
      assertThat(filtered)
          .singleElement()
          .satisfies(
              row -> {
                assertThat(row.getPosition()).isEqualTo(3);
                assertThat(row.getPlayerId()).isEqualTo(last.getId());
              });
      assertThat(quotes.countRanking("%_", league, "offence")).isEqualTo(1);
      assertThat(quotes.findRankingPage("no-match", null, null, 20, 0)).isEmpty();
      assertThat(quotes.countRanking("no-match", null, null)).isZero();
      assertThat(quotes.findRankingPage(null, null, null, 20, 100)).isEmpty();
      assertThat(quotes.countRanking(null, null, null)).isEqualTo(3);
      assertThat(quotes.findRankingLeagues()).isNotEmpty();
      assertThat(quotes.findRankingPositions()).contains("Offence");
    }
  }

  private List<Player> prepareRanking() {
    catalogue.prepare();
    List<Long> ids = players.findAll().stream().map(Player::getId).toList();
    List<Player> population =
        players.findHistoricalIdentitySnapshot(ids).stream()
            .sorted(Comparator.comparing(Player::getId))
            .toList();
    for (int index = 0; index < population.size(); index++) {
      Player player = population.get(index);
      data.persistInitial(player, DATE.minusDays(1), NOW.minusSeconds(86400));
      if (index < 3) {
        data.prepareMetrics(player, DATE.minusDays(1));
        data.persistRecomputed(player, DATE, index < 2 ? 150 : 100, NOW);
      }
    }
    return population;
  }
}
