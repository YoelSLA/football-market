package footballmarket.features.ranking.application;

import static org.assertj.core.api.Assertions.assertThat;

import footballmarket.features.catalog.application.PlayerCatalogService;
import footballmarket.features.catalog.domain.models.player.Player;
import footballmarket.features.ranking.domain.records.ScoreRanking;
import footballmarket.features.ranking.domain.records.ScoreRankingEntry;
import footballmarket.features.ranking.domain.records.ScoreRankingPage;
import footballmarket.features.ranking.domain.records.ScoreRankingQuery;
import footballmarket.features.valuation.domain.enums.ValuationStrategy;
import footballmarket.support.IdentityCatalogueFixture;
import footballmarket.support.ResetPlayerCatalogListener;
import footballmarket.support.ScoreRankingTestData;
import footballmarket.support.TestcontainersConfiguration;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
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
  ScoreRankingTestData.class
})
@TestExecutionListeners(
    listeners = ResetPlayerCatalogListener.class,
    mergeMode = TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS)
@org.junit.jupiter.api.Tag("integration")
class ScoreRankingQueryServiceTest {
  @Autowired private ScoreRankingQueryService queryService;
  @Autowired private PlayerCatalogService playerCatalogue;
  @Autowired private IdentityCatalogueFixture catalogue;
  @Autowired private ScoreRankingTestData data;

  @Nested
  @DisplayName("Consulta del ranking posicional")
  class PositionalRanking {
    @Test
    @DisplayName("Normaliza los filtros y conserva las opciones globales")
    void normalizesFilters() {
      // Arrange
      catalogue.prepare();
      data.prepareCatalogueMetrics();
      Player player = playerCatalogue.getActivePlayers(0, 10).getContent().getFirst();
      LocalDate date = LocalDate.of(2026, 10, 20);
      data.preparePlayer(player, date, date, new BigDecimal("0.900000"));
      data.prepareExecution(date);
      ScoreRankingQuery query =
          new ScoreRankingQuery(0, 20, "  " + player.getName() + "  ", null, null);

      // Act
      ScoreRankingPage result = queryService.findScoreRanking(query);

      // Assert
      assertThat(result.content()).hasSize(1);
      assertThat(result.content().getFirst().playerId()).isEqualTo(player.getId());
      assertThat(result.totalElements()).isEqualTo(1);
      assertThat(result.filterOptions().positions()).isNotEmpty();
    }

    @Test
    @DisplayName("El podio devuelve las tres primeras posiciones y el total global")
    void returnsPodiumAndGlobalTotal() {
      // Arrange
      catalogue.prepare();
      data.prepareCatalogueMetrics();
      List<Player> players =
          playerCatalogue.getActivePlayers(0, 10).getContent().stream()
              .sorted(java.util.Comparator.comparing(Player::getId))
              .toList();
      LocalDate date = LocalDate.of(2026, 10, 20);
      for (Player player : players) {
        data.preparePlayer(player, date, date, new BigDecimal("0.800000"));
      }
      data.prepareExecution(date);
      ScoreRankingQuery query = new ScoreRankingQuery(0, 3, null, null, null);

      // Act
      ScoreRankingPage result = queryService.findScoreRanking(query);

      // Assert
      assertThat(result.totalElements()).isEqualTo(players.size());
      assertThat(result.content())
          .extracting(ScoreRankingPage.Entry::position)
          .containsExactly(1L, 2L, 3L);
      assertThat(result.content())
          .extracting(ScoreRankingPage.Entry::playerId)
          .containsExactly(players.get(0).getId(), players.get(1).getId(), players.get(2).getId());
      assertThat(queryService.findScoreRanking(query)).isEqualTo(result);
    }

    @Test
    @DisplayName("La página filtrada conserva la posición global y las opciones")
    void filteredPagePreservesGlobalPosition() {
      // Arrange
      catalogue.prepare();
      data.prepareCatalogueMetrics();
      List<Player> players =
          playerCatalogue.getActivePlayers(0, 10).getContent().stream()
              .sorted(java.util.Comparator.comparing(Player::getId))
              .toList();
      LocalDate date = LocalDate.of(2026, 10, 20);
      data.preparePlayer(players.get(0), date, date, new BigDecimal("0.900000"));
      data.preparePlayer(players.get(1), date, date, new BigDecimal("0.800000"));
      data.prepareExecution(date);
      ScoreRankingQuery query = new ScoreRankingQuery(1, 1, null, null, null);

      // Act
      ScoreRankingPage result = queryService.findScoreRanking(query);

      // Assert
      assertThat(result.totalElements()).isEqualTo(2);
      assertThat(result.totalPages()).isEqualTo(2);
      assertThat(result.content()).hasSize(1);
      assertThat(result.content().getFirst().position()).isEqualTo(2);
      assertThat(result.content().getFirst().playerId()).isEqualTo(players.get(1).getId());
      assertThat(result.filterOptions().positions()).isNotEmpty();
    }

    @Test
    @DisplayName("Conserva el orden persistido y asigna posiciones consecutivas")
    void ordersEntriesAndAssignsPositions() {
      // Arrange
      catalogue.prepare();
      data.prepareCatalogueMetrics();
      List<Player> players = playerCatalogue.getActivePlayers(0, 10).getContent();
      Player lowerIdentifier =
          players.stream().min(java.util.Comparator.comparing(Player::getId)).orElseThrow();
      Player higherIdentifier =
          players.stream().max(java.util.Comparator.comparing(Player::getId)).orElseThrow();
      LocalDate quoteDate = LocalDate.of(2026, 10, 20);
      data.preparePlayer(
          lowerIdentifier, quoteDate, LocalDate.of(2026, 10, 24), new BigDecimal("0.800000"));
      data.preparePlayer(
          higherIdentifier, quoteDate, LocalDate.of(2026, 10, 21), new BigDecimal("0.800000"));
      data.prepareExecution(quoteDate);

      // Act
      ScoreRanking ranking = queryService.findScoreRanking();

      // Assert
      assertThat(ranking.strategy()).isEqualTo(ValuationStrategy.POSITIONAL_PERFORMANCE);
      assertThat(ranking.quoteDate()).isEqualTo(quoteDate);
      assertThat(ranking.entries())
          .extracting(ScoreRankingEntry::playerId)
          .containsExactly(lowerIdentifier.getId(), higherIdentifier.getId());
      assertThat(ranking.entries()).extracting(ScoreRankingEntry::position).containsExactly(1, 2);
      assertThat(ranking.entries().get(0).score()).isEqualByComparingTo(new BigDecimal("0.800000"));
      assertThat(ranking.entries().get(1).score()).isEqualByComparingTo(new BigDecimal("0.800000"));
      assertThat(ranking.entries().getFirst().valuationDate()).isAfter(quoteDate);
      assertThat(queryService.findScoreRanking()).isEqualTo(ranking);
    }

    @Test
    @DisplayName("Mantiene el orden total para varios grupos de empate en consultas repetidas")
    void maintainsTotalOrderAcrossRepeatedQueries() {
      // Arrange
      catalogue.prepare();
      data.prepareCatalogueMetrics();
      List<Player> players =
          playerCatalogue.getActivePlayers(0, 10).getContent().stream()
              .sorted(java.util.Comparator.comparing(Player::getId))
              .toList();
      LocalDate quoteDate = LocalDate.of(2026, 10, 20);
      data.preparePlayer(players.get(0), quoteDate, quoteDate, new BigDecimal("0.900000"));
      data.preparePlayer(players.get(1), quoteDate, quoteDate, new BigDecimal("0.900000"));
      data.preparePlayer(players.get(2), quoteDate, quoteDate, new BigDecimal("0.900000"));
      data.preparePlayer(players.get(3), quoteDate, quoteDate, new BigDecimal("0.700000"));
      data.preparePlayer(players.get(4), quoteDate, quoteDate, new BigDecimal("0.700000"));
      data.prepareExecution(quoteDate);

      // Act
      ScoreRanking firstResult = queryService.findScoreRanking();
      ScoreRanking secondResult = queryService.findScoreRanking();

      // Assert
      assertThat(firstResult.entries())
          .extracting(ScoreRankingEntry::playerId)
          .containsExactlyElementsOf(players.stream().map(Player::getId).toList());
      assertThat(firstResult.entries())
          .extracting(ScoreRankingEntry::position)
          .containsExactly(1, 2, 3, 4, 5);
      assertThat(secondResult).isEqualTo(firstResult);
    }
  }

  @Nested
  @DisplayName("Consulta del ranking por atributos")
  class AttributeRanking {
    @Test
    @DisplayName("La página de atributos muestra el score final y su fecha")
    void returnsAttributePage() {
      // Arrange
      catalogue.prepare();
      data.prepareCatalogueMetrics();
      Player player = playerCatalogue.getActivePlayers(0, 10).getContent().getFirst();
      LocalDate date = LocalDate.of(2026, 10, 20);
      LocalDate valuationDate = date.plusDays(1);
      data.prepareAttributePlayer(
          player, date, valuationDate, new BigDecimal("0.990000"), new BigDecimal("0.840000"));
      data.prepareExecution(date, ValuationStrategy.GENERAL_ATTRIBUTES);
      ScoreRankingQuery query = new ScoreRankingQuery(0, 20, null, null, null);

      // Act
      ScoreRankingPage result = queryService.findScoreRanking(query);

      // Assert
      assertThat(result.strategy()).isEqualTo(ValuationStrategy.GENERAL_ATTRIBUTES);
      assertThat(result.totalElements()).isEqualTo(1);
      assertThat(result.content()).hasSize(1);
      assertThat(result.content().getFirst().score()).isEqualByComparingTo("0.840000");
      assertThat(result.content().getFirst().valuationDate()).isEqualTo(valuationDate);
    }

    @Test
    @DisplayName("Expone el resultado final de atributos y su fecha de valuación")
    void exposesAttributeFinalScore() {
      // Arrange
      catalogue.prepare();
      data.prepareCatalogueMetrics();
      List<Player> players =
          playerCatalogue.getActivePlayers(0, 10).getContent().stream()
              .sorted(java.util.Comparator.comparing(Player::getId))
              .toList();
      LocalDate quoteDate = LocalDate.of(2026, 10, 20);
      LocalDate lowerFinalDate = LocalDate.of(2026, 10, 21);
      LocalDate higherFinalDate = LocalDate.of(2026, 10, 22);
      data.prepareAttributePlayer(
          players.get(0),
          quoteDate,
          lowerFinalDate,
          new BigDecimal("0.990000"),
          new BigDecimal("0.840000"));
      data.prepareAttributePlayer(
          players.get(1),
          quoteDate,
          higherFinalDate,
          new BigDecimal("0.800000"),
          new BigDecimal("0.910000"));
      data.prepareExecution(quoteDate, ValuationStrategy.GENERAL_ATTRIBUTES);

      // Act
      ScoreRanking ranking = queryService.findScoreRanking();

      // Assert
      assertThat(ranking.strategy()).isEqualTo(ValuationStrategy.GENERAL_ATTRIBUTES);
      assertThat(ranking.quoteDate()).isEqualTo(quoteDate);
      assertThat(ranking.entries())
          .extracting(ScoreRankingEntry::playerId)
          .containsExactly(players.get(1).getId(), players.get(0).getId());
      assertThat(ranking.entries()).extracting(ScoreRankingEntry::position).containsExactly(1, 2);
      assertThat(ranking.entries().get(0).score()).isEqualByComparingTo(new BigDecimal("0.910000"));
      assertThat(ranking.entries().get(1).score()).isEqualByComparingTo(new BigDecimal("0.840000"));
      assertThat(ranking.entries().get(0).valuationDate()).isEqualTo(higherFinalDate);
      assertThat(ranking.entries().get(1).valuationDate()).isEqualTo(lowerFinalDate);
    }
  }

  @Nested
  @DisplayName("Ausencia de información para el ranking")
  class EmptyRanking {
    @Test
    @DisplayName("La página sin valuaciones conserva el encabezado finalizado")
    void returnsEmptyPageWithHeader() {
      // Arrange
      LocalDate date = LocalDate.of(2026, 10, 20);
      data.prepareExecution(date, ValuationStrategy.GENERAL_ATTRIBUTES);
      ScoreRankingQuery query = new ScoreRankingQuery(0, 20, null, null, null);

      // Act
      ScoreRankingPage result = queryService.findScoreRanking(query);

      // Assert
      assertThat(result.strategy()).isEqualTo(ValuationStrategy.GENERAL_ATTRIBUTES);
      assertThat(result.quoteDate()).isEqualTo(date);
      assertThat(result.content()).isEmpty();
      assertThat(result.totalElements()).isZero();
      assertThat(result.filterOptions().leagues()).isEmpty();
      assertThat(result.filterOptions().positions()).isEmpty();
    }

    @Test
    @DisplayName("Un filtro sin coincidencias conserva las opciones globales")
    void preservesOptionsWithoutMatches() {
      // Arrange
      catalogue.prepare();
      data.prepareCatalogueMetrics();
      Player player = playerCatalogue.getActivePlayers(0, 10).getContent().getFirst();
      LocalDate date = LocalDate.of(2026, 10, 20);
      data.preparePlayer(player, date, date, new BigDecimal("0.900000"));
      data.prepareExecution(date);
      ScoreRankingQuery query = new ScoreRankingQuery(0, 20, "no-such-player", null, null);

      // Act
      ScoreRankingPage result = queryService.findScoreRanking(query);

      // Assert
      assertThat(result.content()).isEmpty();
      assertThat(result.totalElements()).isZero();
      assertThat(result.filterOptions().positions()).isNotEmpty();
    }

    @Test
    @DisplayName("Devuelve página vacía y opciones vacías sin ejecución finalizada")
    void returnsEmptyPageWithoutExecution() {
      // Arrange
      ScoreRankingQuery query = new ScoreRankingQuery(2, 3, null, null, null);

      // Act
      ScoreRankingPage result = queryService.findScoreRanking(query);

      // Assert
      assertThat(result.strategy()).isNull();
      assertThat(result.quoteDate()).isNull();
      assertThat(result.content()).isEmpty();
      assertThat(result.totalElements()).isZero();
      assertThat(result.totalPages()).isZero();
      assertThat(result.filterOptions().leagues()).isEmpty();
      assertThat(result.filterOptions().positions()).isEmpty();
    }

    @Test
    @DisplayName("Conserva los totales y opciones al consultar una página fuera de rango")
    void preservesTotalsOutsideRange() {
      // Arrange
      catalogue.prepare();
      data.prepareCatalogueMetrics();
      Player player = playerCatalogue.getActivePlayers(0, 10).getContent().getFirst();
      LocalDate date = LocalDate.of(2026, 10, 20);
      data.preparePlayer(player, date, date, new BigDecimal("0.900000"));
      data.prepareExecution(date);
      ScoreRankingQuery query = new ScoreRankingQuery(3, 1, null, null, null);

      // Act
      ScoreRankingPage result = queryService.findScoreRanking(query);

      // Assert
      assertThat(result.content()).isEmpty();
      assertThat(result.totalElements()).isEqualTo(1);
      assertThat(result.totalPages()).isEqualTo(1);
      assertThat(result.filterOptions().positions()).isNotEmpty();
    }

    @Test
    @DisplayName("Devuelve encabezado nulo cuando todavía no hay ejecuciones")
    void returnsNullHeaderWithoutExecutions() {
      // Act
      ScoreRanking ranking = queryService.findScoreRanking();

      // Assert
      assertThat(ranking.strategy()).isNull();
      assertThat(ranking.quoteDate()).isNull();
      assertThat(ranking.entries()).isEmpty();
    }

    @Test
    @DisplayName("Conserva el encabezado cuando la estrategia no tiene valuaciones")
    void returnsHeaderWithoutValuations() {
      // Arrange
      LocalDate quoteDate = LocalDate.of(2026, 10, 20);
      data.prepareExecution(quoteDate, ValuationStrategy.GENERAL_ATTRIBUTES);

      // Act
      ScoreRanking ranking = queryService.findScoreRanking();

      // Assert
      assertThat(ranking.strategy()).isEqualTo(ValuationStrategy.GENERAL_ATTRIBUTES);
      assertThat(ranking.quoteDate()).isEqualTo(quoteDate);
      assertThat(ranking.entries()).isEmpty();
    }

    @Test
    @DisplayName("Devuelve ranking vacío cuando todos los jugadores carecen de valuación")
    void returnsEmptyRankingWhenAllPlayersWereCarriedForward() {
      // Arrange
      catalogue.prepare();
      LocalDate quoteDate = LocalDate.of(2026, 10, 20);
      data.prepareExecution(quoteDate);

      // Act
      ScoreRanking ranking = queryService.findScoreRanking();

      // Assert
      assertThat(ranking.strategy()).isEqualTo(ValuationStrategy.POSITIONAL_PERFORMANCE);
      assertThat(ranking.quoteDate()).isEqualTo(quoteDate);
      assertThat(ranking.entries()).isEmpty();
    }
  }
}
