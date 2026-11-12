package footballmarket.features.ranking.infraestructure;

import static org.assertj.core.api.Assertions.assertThat;

import footballmarket.features.catalog.application.PlayerCatalogService;
import footballmarket.features.catalog.domain.models.player.Player;
import footballmarket.features.catalog.infraestructure.PlayerRepository;
import footballmarket.features.quote.domain.models.QuoteExecution;
import footballmarket.features.quote.infraestructure.QuoteExecutionRepository;
import footballmarket.features.statistics.domain.enums.MetricSource;
import footballmarket.features.statistics.domain.enums.PlayerExtractionResult;
import footballmarket.features.valuation.domain.models.AttributeValuation;
import footballmarket.features.valuation.domain.models.PositionalValuation;
import footballmarket.features.valuation.infraestructure.AttributeValuationRepository;
import footballmarket.features.valuation.infraestructure.PositionalValuationRepository;
import footballmarket.support.IdentityCatalogueFixture;
import footballmarket.support.PlayerCatalogQueryData;
import footballmarket.support.PlayerStatisticsQueryData;
import footballmarket.support.ResetPlayerCatalogListener;
import footballmarket.support.ScoreRankingTestData;
import footballmarket.support.TestcontainersConfiguration;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestExecutionListeners;

@SpringBootTest
@ActiveProfiles("test")
@Import({
  TestcontainersConfiguration.class,
  IdentityCatalogueFixture.class,
  ScoreRankingTestData.class,
  PlayerCatalogQueryData.class,
  PlayerStatisticsQueryData.class
})
@TestExecutionListeners(
    listeners = ResetPlayerCatalogListener.class,
    mergeMode = TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS)
@org.junit.jupiter.api.Tag("integration")
class ScoreRankingQueryTest {
  @Autowired private PositionalValuationRepository valuations;
  @Autowired private AttributeValuationRepository attributeValuations;
  @Autowired private QuoteExecutionRepository executions;
  @Autowired private PlayerCatalogService playerCatalogue;
  @Autowired private IdentityCatalogueFixture catalogue;
  @Autowired private ScoreRankingTestData data;
  @Autowired private PlayerCatalogQueryData catalogData;
  @Autowired private PlayerStatisticsQueryData statistics;
  @Autowired private PlayerRepository players;

  private void prepareEligibleCatalogue() {
    catalogue.prepare();
    for (Player player : players.findAll()) {
      statistics.row(
          player.getId(),
          LocalDate.of(2026, 9, 1),
          MetricSource.JSON,
          "2026",
          "1",
          PlayerExtractionResult.COMPLETED,
          Map.of("goals_total", BigDecimal.ZERO));
    }
  }

  @Nested
  @DisplayName("Consultas persistentes del ranking por score")
  class Queries {
    @Test
    @DisplayName(
        "Ambas estrategias excluyen valoraciones históricas de jugadores sin métricas recientes")
    void excludesValuationsWithEmptyLatestMetrics() {
      // Arrange
      var excluded = catalogData.player("Excluido", "Forward", "France", null);
      var included = catalogData.player("Incluido", "Forward", "Spain", null);
      LocalDate old = LocalDate.of(2026, 10, 20);
      LocalDate latest = old.plusDays(1);
      statistics.row(
          excluded.playerId(),
          old,
          MetricSource.JSON,
          "2026",
          "1",
          PlayerExtractionResult.COMPLETED,
          Map.of("goals_total", BigDecimal.ONE));
      statistics.row(
          excluded.playerId(),
          latest,
          MetricSource.JSON,
          "2026",
          "1",
          PlayerExtractionResult.COMPLETED,
          Map.of());
      statistics.row(
          included.playerId(),
          latest,
          MetricSource.JSON,
          "2026",
          "1",
          PlayerExtractionResult.COMPLETED,
          Map.of("goals_total", BigDecimal.ZERO));
      Player excludedPlayer = players.findById(excluded.playerId()).orElseThrow();
      Player includedPlayer = players.findById(included.playerId()).orElseThrow();
      data.preparePlayer(excludedPlayer, old, old, new BigDecimal("0.900000"));
      data.preparePlayer(includedPlayer, old, old, new BigDecimal("0.700000"));
      data.prepareAttributeValuation(excludedPlayer, old, new BigDecimal("0.900000"));
      data.prepareAttributeValuation(includedPlayer, old, new BigDecimal("0.700000"));

      // Act / Assert
      assertThat(valuations.findScoreRanking())
          .extracting(row -> row.getPlayer().getId())
          .containsExactly(included.playerId());
      assertThat(attributeValuations.findScoreRanking())
          .extracting(row -> row.getPlayer().getId())
          .containsExactly(included.playerId());
    }

    @Test
    @DisplayName("Selecciona la valuación de mayor fecha y ordena los empates por jugador")
    void selectsLatestValuationAndOrdersTies() {
      // Arrange
      prepareEligibleCatalogue();
      List<Player> players = playerCatalogue.getActivePlayers(0, 10).getContent();
      Player first =
          players.stream().min(java.util.Comparator.comparing(Player::getId)).orElseThrow();
      Player second =
          players.stream().max(java.util.Comparator.comparing(Player::getId)).orElseThrow();
      LocalDate quoteDate = LocalDate.of(2026, 10, 20);
      data.preparePlayer(first, quoteDate, LocalDate.of(2026, 10, 24), new BigDecimal("0.700000"));
      data.preparePositionalValuation(
          first, LocalDate.of(2026, 10, 19), new BigDecimal("0.900000"));
      data.preparePlayer(second, quoteDate, LocalDate.of(2026, 10, 21), new BigDecimal("0.700000"));
      data.prepareExecution(quoteDate);

      // Act
      List<PositionalValuation> ranking = valuations.findScoreRanking();

      // Assert
      assertThat(executions.findTopByOrderByQuoteDateDesc()).isPresent();
      assertThat(ranking)
          .extracting(row -> row.getPlayer().getId())
          .containsExactly(first.getId(), second.getId());
      assertThat(ranking.getFirst().getValuationDate()).isAfter(quoteDate);
    }

    @Test
    @DisplayName("Ordena el resultado final de atributos y excluye jugadores sin cotización")
    void ordersAttributeScoresAndExcludesPlayersWithoutQuotes() {
      // Arrange
      prepareEligibleCatalogue();
      List<Player> players = playerCatalogue.getActivePlayers(0, 10).getContent();
      Player quoted = players.get(0);
      Player withoutQuote = players.get(1);
      LocalDate quoteDate = LocalDate.of(2026, 10, 20);
      data.prepareAttributePlayer(
          quoted, quoteDate, LocalDate.of(2026, 10, 21), new BigDecimal("0.700000"));
      data.prepareAttributeValuation(
          withoutQuote, LocalDate.of(2026, 10, 21), new BigDecimal("0.900000"));

      // Act
      List<AttributeValuation> ranking = attributeValuations.findScoreRanking();

      // Assert
      assertThat(ranking)
          .extracting(row -> row.getPlayer().getId())
          .containsExactly(quoted.getId());
    }

    @Test
    @DisplayName("Mantiene el orden total posicional ante múltiples grupos de empate")
    void maintainsPositionalOrderAcrossMultipleTieGroups() {
      // Arrange
      prepareEligibleCatalogue();
      List<Player> players =
          playerCatalogue.getActivePlayers(0, 10).getContent().stream()
              .sorted(java.util.Comparator.comparing(Player::getId))
              .toList();
      LocalDate date = LocalDate.of(2026, 10, 20);
      data.preparePlayer(players.get(0), date, date, new BigDecimal("0.900000"));
      data.preparePlayer(players.get(1), date, date, new BigDecimal("0.900000"));
      data.preparePlayer(players.get(2), date, date, new BigDecimal("0.900000"));
      data.preparePlayer(players.get(3), date, date, new BigDecimal("0.700000"));
      data.preparePlayer(players.get(4), date, date, new BigDecimal("0.700000"));

      // Act
      List<PositionalValuation> firstResult = valuations.findScoreRanking();
      List<PositionalValuation> secondResult = valuations.findScoreRanking();

      // Assert
      assertThat(firstResult)
          .extracting(row -> row.getPlayer().getId())
          .containsExactlyElementsOf(players.stream().map(Player::getId).toList());
      assertThat(secondResult)
          .extracting(row -> row.getPlayer().getId())
          .containsExactlyElementsOf(players.stream().map(Player::getId).toList());
    }

    @Test
    @DisplayName("Mantiene el orden total por atributos ante múltiples grupos de empate")
    void maintainsAttributeOrderAcrossMultipleTieGroups() {
      // Arrange
      prepareEligibleCatalogue();
      List<Player> players =
          playerCatalogue.getActivePlayers(0, 10).getContent().stream()
              .sorted(java.util.Comparator.comparing(Player::getId))
              .toList();
      LocalDate date = LocalDate.of(2026, 10, 20);
      data.prepareAttributePlayer(players.get(0), date, date, new BigDecimal("0.900000"));
      data.prepareAttributePlayer(players.get(1), date, date, new BigDecimal("0.900000"));
      data.prepareAttributePlayer(players.get(2), date, date, new BigDecimal("0.900000"));
      data.prepareAttributePlayer(players.get(3), date, date, new BigDecimal("0.700000"));
      data.prepareAttributePlayer(players.get(4), date, date, new BigDecimal("0.700000"));

      // Act
      List<AttributeValuation> firstResult = attributeValuations.findScoreRanking();
      List<AttributeValuation> secondResult = attributeValuations.findScoreRanking();

      // Assert
      assertThat(firstResult)
          .extracting(row -> row.getPlayer().getId())
          .containsExactlyElementsOf(players.stream().map(Player::getId).toList());
      assertThat(secondResult)
          .extracting(row -> row.getPlayer().getId())
          .containsExactlyElementsOf(players.stream().map(Player::getId).toList());
    }

    @Test
    @DisplayName("Selecciona la ejecución de mayor fecha entre varias persistidas")
    void selectsLatestExecutionByQuoteDate() {
      // Arrange
      LocalDate oldestDate = LocalDate.of(2026, 10, 14);
      LocalDate middleDate = LocalDate.of(2026, 10, 18);
      LocalDate latestDate = LocalDate.of(2026, 10, 21);
      data.prepareExecution(middleDate);
      data.prepareExecution(latestDate);
      data.prepareExecution(oldestDate);

      // Act
      QuoteExecution execution = executions.findTopByOrderByQuoteDateDesc().orElseThrow();

      // Assert
      assertThat(execution.getQuoteDate()).isEqualTo(latestDate);
    }
  }
}
