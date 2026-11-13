package footballmarket.features.ranking.infraestructure;

import static org.assertj.core.api.Assertions.assertThat;

import footballmarket.features.catalog.application.PlayerCatalogService;
import footballmarket.features.catalog.domain.models.player.Player;
import footballmarket.features.catalog.infraestructure.PlayerRepository;
import footballmarket.features.quote.domain.enums.QuoteExecutionOrigin;
import footballmarket.features.quote.domain.models.QuoteExecution;
import footballmarket.features.quote.infraestructure.QuoteExecutionRepository;
import footballmarket.features.ranking.domain.records.ScoreRankingRow;
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
import java.time.Instant;
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
    @DisplayName("Combina nombre, liga y posición sin alterar las opciones globales")
    void combinesFiltersAndPreservesGlobalOptions() {
      // Arrange
      PlayerCatalogQueryData.Fixture first = catalogData.player("Alpha", "Forward", "France", null);
      PlayerCatalogQueryData.Fixture second = catalogData.player("Beta", "Midfield", "Spain", null);
      LocalDate date = LocalDate.of(2026, 10, 20);
      statistics.row(
          first.playerId(),
          date,
          MetricSource.JSON,
          "2026",
          "1",
          PlayerExtractionResult.COMPLETED,
          Map.of("goals_total", BigDecimal.ONE));
      statistics.row(
          second.playerId(),
          date,
          MetricSource.JSON,
          "2026",
          "1",
          PlayerExtractionResult.COMPLETED,
          Map.of("goals_total", BigDecimal.ONE));
      Player firstPlayer = players.findById(first.playerId()).orElseThrow();
      Player secondPlayer = players.findById(second.playerId()).orElseThrow();
      data.preparePlayer(firstPlayer, date, date, new BigDecimal("0.700000"));
      data.preparePlayer(secondPlayer, date, date, new BigDecimal("0.900000"));
      data.prepareAttributeValuation(firstPlayer, date, new BigDecimal("0.700000"));
      data.prepareAttributeValuation(secondPlayer, date, new BigDecimal("0.900000"));

      // Act
      List<ScoreRankingRow> positional =
          valuations.findRankingPage("ALP", first.leagueId(), "forward", 20, 0);
      List<ScoreRankingRow> attributes =
          attributeValuations.findRankingPage("ALP", first.leagueId(), "forward", 20, 0);

      // Assert
      assertThat(positional)
          .extracting(ScoreRankingRow::getPlayerId)
          .containsExactly(first.playerId());
      assertThat(attributes)
          .extracting(ScoreRankingRow::getPlayerId)
          .containsExactly(first.playerId());
      assertThat(positional.getFirst().getPosition()).isEqualTo(2);
      assertThat(attributes.getFirst().getPosition()).isEqualTo(2);
      assertThat(valuations.countRanking("ALP", second.leagueId(), "forward")).isZero();
      assertThat(attributeValuations.countRanking("ALP", second.leagueId(), "forward")).isZero();
      assertThat(valuations.findRankingLeagues()).hasSize(2);
      assertThat(attributeValuations.findRankingLeagues()).hasSize(2);
      assertThat(valuations.findRankingPositions())
          .containsExactlyInAnyOrder("Forward", "Midfield");
      assertThat(attributeValuations.findRankingPositions())
          .containsExactlyInAnyOrder("Forward", "Midfield");
    }

    @Test
    @DisplayName("La última ejecución finalizada ignora un contexto abierto más reciente")
    void ignoresNewerOpenExecution() {
      // Arrange
      LocalDate date = LocalDate.of(2026, 10, 20);
      data.prepareExecution(date);
      Instant startedAt = date.plusDays(1).atStartOfDay().toInstant(java.time.ZoneOffset.UTC);
      QuoteExecution open =
          new QuoteExecution(
              date.plusDays(1),
              QuoteExecutionOrigin.MANUAL,
              footballmarket.features.valuation.domain.enums.ValuationStrategy
                  .POSITIONAL_PERFORMANCE,
              "v1",
              new BigDecimal("0.10"),
              new BigDecimal("0.10"),
              1L,
              0,
              0,
              0,
              0,
              0,
              startedAt,
              null);
      executions.saveAndFlush(open);

      // Act
      QuoteExecution result =
          executions.findTopByFinishedAtIsNotNullOrderByStartedAtDescIdDesc().orElseThrow();

      // Assert
      assertThat(result.getQuoteDate()).isEqualTo(date);
      assertThat(result.getFinishedAt()).isNotNull();
    }

    @Test
    @DisplayName("Incluye al jugador si una fuente de la última fecha tiene métricas")
    void includesPlayerWithMetricsFromOneLatestSource() {
      // Arrange
      PlayerCatalogQueryData.Fixture fixture =
          catalogData.player("Jugador", "Forward", "France", null);
      LocalDate date = LocalDate.of(2026, 10, 20);
      statistics.row(
          fixture.playerId(),
          date,
          MetricSource.JSON,
          "2026",
          "1",
          PlayerExtractionResult.COMPLETED,
          Map.of());
      statistics.row(
          fixture.playerId(),
          date,
          MetricSource.SCRAPING,
          "2026",
          "1",
          PlayerExtractionResult.COMPLETED,
          Map.of("goals_total", BigDecimal.ONE));
      Player player = players.findById(fixture.playerId()).orElseThrow();
      data.preparePlayer(player, date, date, new BigDecimal("0.900000"));
      data.prepareAttributeValuation(player, date, new BigDecimal("0.800000"));

      // Act
      List<ScoreRankingRow> positional = valuations.findRankingPage(null, null, null, 20, 0);
      List<ScoreRankingRow> attributes =
          attributeValuations.findRankingPage(null, null, null, 20, 0);

      // Assert
      assertThat(positional)
          .extracting(ScoreRankingRow::getPlayerId)
          .containsExactly(fixture.playerId());
      assertThat(attributes)
          .extracting(ScoreRankingRow::getPlayerId)
          .containsExactly(fixture.playerId());
    }

    @Test
    @DisplayName("Pagina ambas estrategias sin renumerar las posiciones globales")
    void paginatesBothStrategiesWithGlobalPositions() {
      // Arrange
      prepareEligibleCatalogue();
      List<Player> rankedPlayers =
          playerCatalogue.getActivePlayers(0, 10).getContent().stream()
              .sorted(java.util.Comparator.comparing(Player::getId))
              .toList();
      LocalDate date = LocalDate.of(2026, 10, 20);
      data.preparePlayer(rankedPlayers.get(0), date, date, new BigDecimal("0.900000"));
      data.preparePlayer(rankedPlayers.get(1), date, date, new BigDecimal("0.800000"));
      data.preparePositionalValuation(
          rankedPlayers.get(1), date.minusDays(1), new BigDecimal("0.950000"));
      data.prepareAttributeValuation(rankedPlayers.get(0), date, new BigDecimal("0.900000"));
      data.prepareAttributeValuation(rankedPlayers.get(1), date, new BigDecimal("0.800000"));
      data.prepareAttributeValuation(
          rankedPlayers.get(1), date.minusDays(1), new BigDecimal("0.950000"));

      // Act
      List<ScoreRankingRow> positional = valuations.findRankingPage(null, null, null, 1, 1);
      List<ScoreRankingRow> attributes =
          attributeValuations.findRankingPage(null, null, null, 1, 1);

      // Assert
      assertThat(positional).hasSize(1);
      assertThat(attributes).hasSize(1);
      assertThat(positional.getFirst().getPosition()).isEqualTo(2);
      assertThat(attributes.getFirst().getPosition()).isEqualTo(2);
      assertThat(positional.getFirst().getPlayerId()).isEqualTo(rankedPlayers.get(1).getId());
      assertThat(attributes.getFirst().getPlayerId()).isEqualTo(rankedPlayers.get(1).getId());
      assertThat(positional.getFirst().getScore()).isEqualByComparingTo("0.800000");
      assertThat(attributes.getFirst().getScore()).isEqualByComparingTo("0.800000");
      assertThat(valuations.countRanking(null, null, null)).isEqualTo(2);
      assertThat(attributeValuations.countRanking(null, null, null)).isEqualTo(2);
    }

    @Test
    @DisplayName("Los comodines del nombre son literales y el filtro conserva la posición global")
    void treatsWildcardsAsLiteral() {
      // Arrange
      PlayerCatalogQueryData.Fixture literal =
          catalogData.player("Jugador % Especial", "Forward", "France", null);
      PlayerCatalogQueryData.Fixture other =
          catalogData.player("Jugador Normal", "Forward", "Spain", null);
      LocalDate date = LocalDate.of(2026, 10, 20);
      statistics.row(
          literal.playerId(),
          date,
          MetricSource.JSON,
          "2026",
          "1",
          PlayerExtractionResult.COMPLETED,
          Map.of("goals_total", BigDecimal.ONE));
      statistics.row(
          other.playerId(),
          date,
          MetricSource.JSON,
          "2026",
          "1",
          PlayerExtractionResult.COMPLETED,
          Map.of("goals_total", BigDecimal.ONE));
      Player literalPlayer = players.findById(literal.playerId()).orElseThrow();
      Player otherPlayer = players.findById(other.playerId()).orElseThrow();
      data.preparePlayer(literalPlayer, date, date, new BigDecimal("0.700000"));
      data.preparePlayer(otherPlayer, date, date, new BigDecimal("0.900000"));

      // Act
      List<ScoreRankingRow> result = valuations.findRankingPage("%", null, null, 20, 0);

      // Assert
      assertThat(result).hasSize(1);
      assertThat(result.getFirst().getPlayerId()).isEqualTo(literal.playerId());
      assertThat(result.getFirst().getPosition()).isEqualTo(2);
      assertThat(valuations.countRanking("%", null, null)).isEqualTo(1);
      assertThat(valuations.countRanking("_", null, null)).isZero();
    }

    @Test
    @DisplayName(
        "Ambas estrategias excluyen valoraciones históricas de jugadores sin métricas recientes")
    void excludesValuationsWithEmptyLatestMetrics() {
      // Arrange
      PlayerCatalogQueryData.Fixture excluded =
          catalogData.player("Excluido", "Forward", "France", null);
      PlayerCatalogQueryData.Fixture included =
          catalogData.player("Incluido", "Forward", "Spain", null);
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
      assertThat(valuations.findRankingPage(null, null, null, 20, 0))
          .extracting(ScoreRankingRow::getPlayerId)
          .containsExactly(included.playerId());
      assertThat(attributeValuations.findRankingPage(null, null, null, 20, 0))
          .extracting(ScoreRankingRow::getPlayerId)
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
      assertThat(valuations.findRankingPage(null, null, null, 20, 0))
          .extracting(ScoreRankingRow::getPlayerId)
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
      assertThat(attributeValuations.findRankingPage(null, null, null, 20, 0))
          .extracting(ScoreRankingRow::getPlayerId)
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
