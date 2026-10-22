package footballmarket.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import footballmarket.models.Player;
import footballmarket.models.PlayerQuote;
import footballmarket.models.enums.QuoteOrigin;
import footballmarket.models.enums.ValuationStrategy;
import footballmarket.models.records.PlayerRankingEntry;
import footballmarket.services.exceptions.QuoteNotFoundException;
import footballmarket.services.internal.QuotePlayerPublisher;
import footballmarket.support.IdentityCatalogueFixture;
import footballmarket.support.QuoteTestData;
import footballmarket.support.ResetPlayerCatalogListener;
import footballmarket.support.TestcontainersConfiguration;
import footballmarket.support.ValuationTestData;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.bean.override.convention.TestBean;

@SpringBootTest
@ActiveProfiles("test")
@Import({
  TestcontainersConfiguration.class,
  IdentityCatalogueFixture.class,
  ValuationTestData.class,
  QuoteTestData.class
})
@TestExecutionListeners(
    listeners = ResetPlayerCatalogListener.class,
    mergeMode = TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS)
@org.junit.jupiter.api.Tag("integration")
class QuoteQueryServiceTest {
  @Autowired private QuoteQueryService queryService;
  @Autowired private QuoteMarketInitializationService initialization;
  @Autowired private QuoteRecalculationService recalculation;
  @Autowired private ValuationEvaluationService valuation;
  @Autowired private PlayerCatalogService playerCatalogue;
  @Autowired private QuotePlayerPublisher publisher;
  @Autowired private IdentityCatalogueFixture catalogue;
  @Autowired private ValuationTestData data;
  @Autowired private QuoteTestData quoteData;

  /**
   * Reloj de las campañas de estadísticas fijado a la fecha de la campaña de las pruebas.
   *
   * <p>La fecha de vigencia de una fila recalculada es la fecha de la valuación utilizada, y la
   * valuación se fecha con el día en que corre. Fijar el reloj hace que el historial, la cotización
   * vigente y la consulta por fecha no dependan del día en que se ejecuta la suite.
   */
  @TestBean(name = "statisticsClock", methodName = "campaignClock")
  private Clock statisticsClock;

  /**
   * Reloj de mercado fijado a la misma fecha de la campaña.
   *
   * <p>La fecha de cotización de una ejecución es el día en que corre, de modo que fijarlo mantiene
   * la serie de cotizaciones estable entre ejecuciones de la suite.
   */
  @TestBean(name = "marketClock", methodName = "quoteClock")
  private Clock marketClock;

  static Clock campaignClock() {
    return Clock.fixed(Instant.parse("2026-10-18T09:00:00Z"), ZoneOffset.UTC);
  }

  static Clock quoteClock() {
    return Clock.fixed(Instant.parse("2026-10-18T09:00:00Z"), ZoneOffset.UTC);
  }

  @Nested
  @DisplayName("Historial de cotizaciones")
  class History {

    @Test
    @DisplayName("Devuelve las filas del jugador de la más reciente a la más antigua")
    void returnsHistoryOrdered() {
      Player player = prepareRecalculatedMarket();

      List<PlayerQuote> history = queryService.findHistory(player.getId());

      assertThat(history).hasSize(2);
      assertThat(history.get(0).getEffectiveDate()).isEqualTo(LocalDate.of(2026, 10, 18));
      assertThat(history.get(0).getOrigin()).isEqualTo(QuoteOrigin.RECOMPUTED);
      assertThat(history.get(1).getEffectiveDate()).isEqualTo(LocalDate.of(2026, 10, 17));
      assertThat(history.get(1).getOrigin()).isEqualTo(QuoteOrigin.INITIAL);
    }

    @Test
    @DisplayName("Devuelve un historial vacío cuando el jugador nunca fue cotizado")
    void returnsEmptyHistoryWithoutQuotes() {
      catalogue.prepare();
      Player player = firstPlayer();

      assertThat(queryService.findHistory(player.getId())).isEmpty();
    }
  }

  @Nested
  @DisplayName("Cotización vigente")
  class Current {

    @Test
    @DisplayName("Devuelve la última cotización persistida del jugador")
    void returnsCurrentQuote() {
      Player player = prepareRecalculatedMarket();

      PlayerQuote current = queryService.findCurrent(player.getId());

      assertThat(current.getEffectiveDate()).isEqualTo(LocalDate.of(2026, 10, 18));
      assertThat(current.getOrigin()).isEqualTo(QuoteOrigin.RECOMPUTED);
      assertThat(current.getScore()).isNotNull();
      assertThat(current.getStrategyName()).isEqualTo(ValuationStrategy.POSITIONAL_PERFORMANCE);
    }

    @Test
    @DisplayName("Informa la ausencia de cotización cuando el jugador nunca fue cotizado")
    void reportsMissingCurrentQuote() {
      catalogue.prepare();
      Player player = firstPlayer();

      assertThatThrownBy(() -> queryService.findCurrent(player.getId()))
          .isInstanceOf(QuoteNotFoundException.class);
    }
  }

  @Nested
  @DisplayName("Cotización a una fecha dada")
  class OnDate {

    @Test
    @DisplayName("Devuelve la última cotización cuya fecha de vigencia no supera la consultada")
    void returnsQuoteOnDate() {
      Player player = prepareRecalculatedMarket();

      PlayerQuote onMarketDate =
          queryService.findOnDate(player.getId(), LocalDate.of(2026, 10, 17));
      PlayerQuote afterRecalculation =
          queryService.findOnDate(player.getId(), LocalDate.of(2026, 10, 20));

      assertThat(onMarketDate.getOrigin()).isEqualTo(QuoteOrigin.INITIAL);
      assertThat(onMarketDate.getPrice()).isEqualTo(100L);
      assertThat(afterRecalculation.getOrigin()).isEqualTo(QuoteOrigin.RECOMPUTED);
    }

    @Test
    @DisplayName("Informa la ausencia de cotización cuando la fecha es anterior a toda cotización")
    void reportsMissingQuoteBeforeFirstDate() {
      Player player = prepareRecalculatedMarket();

      assertThatThrownBy(() -> queryService.findOnDate(player.getId(), LocalDate.of(2026, 10, 16)))
          .isInstanceOf(QuoteNotFoundException.class);
    }
  }

  @Nested
  @DisplayName("Prevalencia a igualdad de fecha de vigencia")
  class Precedence {

    @Test
    @DisplayName("La fila recalculada prevalece sobre la arrastrada de la misma fecha")
    void recomputedPrecedesCarriedForward() {
      Player player = prepareMarketWithBothOrigins();
      List<PlayerQuote> history = queryService.findHistory(player.getId());

      assertThat(history).hasSize(3);
      assertThat(history.get(0).getOrigin()).isEqualTo(QuoteOrigin.RECOMPUTED);
      assertThat(history.get(1).getOrigin()).isEqualTo(QuoteOrigin.CARRIED_FORWARD);
      assertThat(queryService.findCurrent(player.getId()).getOrigin())
          .isEqualTo(QuoteOrigin.RECOMPUTED);
    }
  }

  @Nested
  @DisplayName("Ranking por precio vigente")
  class Ranking {

    @Test
    @DisplayName(
        "Ordena a los jugadores cotizados por su precio vigente con una sola entrada por jugador")
    void ordersPlayersByCurrentPrice() {
      // Arrange
      catalogue.prepare();
      List<Player> players = playerCatalogue.getActivePlayers(0, 10).getContent();
      Player mostExpensive = players.get(0);
      Player cheapest = players.get(1);
      Player carriedForward = players.get(2);
      LocalDate marketDate = LocalDate.of(2026, 10, 17);
      LocalDate quoteDate = LocalDate.of(2026, 10, 20);
      for (Player player : List.of(mostExpensive, cheapest, carriedForward)) {
        quoteData.prepareMetrics(player, marketDate);
        quoteData.persistInitial(player, marketDate, Instant.parse("2026-10-17T09:00:00Z"));
      }
      quoteData.persistRecomputed(
          mostExpensive, quoteDate, 130L, Instant.parse("2026-10-20T09:00:00Z"));
      quoteData.persistRecomputed(cheapest, quoteDate, 110L, Instant.parse("2026-10-20T09:00:00Z"));
      quoteData.persistCarriedForward(
          carriedForward, quoteDate, 100L, Instant.parse("2026-10-20T09:00:00Z"));

      // Act
      List<PlayerRankingEntry> ranking = queryService.findRanking();

      // Assert
      assertThat(ranking)
          .extracting(PlayerRankingEntry::playerId)
          .containsExactly(mostExpensive.getId(), cheapest.getId(), carriedForward.getId());
      assertThat(ranking).extracting(PlayerRankingEntry::price).containsExactly(130L, 110L, 100L);
    }

    @Test
    @DisplayName("Muestra al jugador con precio arrastrado con su precio repetido")
    void showsCarriedForwardPrice() {
      // Arrange
      catalogue.prepare();
      Player player = playerCatalogue.getActivePlayers(0, 10).getContent().getFirst();
      LocalDate marketDate = LocalDate.of(2026, 10, 17);
      LocalDate quoteDate = LocalDate.of(2026, 10, 20);
      quoteData.prepareMetrics(player, marketDate);
      quoteData.persistInitial(player, marketDate, Instant.parse("2026-10-17T09:00:00Z"));
      quoteData.persistCarriedForward(
          player, quoteDate, 100L, Instant.parse("2026-10-20T09:00:00Z"));

      // Act
      List<PlayerRankingEntry> ranking = queryService.findRanking();

      // Assert
      assertThat(ranking).hasSize(1);
      assertThat(ranking.getFirst().origin()).isEqualTo(QuoteOrigin.CARRIED_FORWARD);
      assertThat(ranking.getFirst().price()).isEqualTo(100L);
    }

    @Test
    @DisplayName("Excluye a los jugadores que no son cotizables por no tener métricas")
    void excludesPlayersWithoutMetrics() {
      // Arrange
      catalogue.prepare();
      List<Player> players = playerCatalogue.getActivePlayers(0, 10).getContent();
      Player cotizable = players.get(0);
      Player withoutMetrics = players.get(1);
      LocalDate marketDate = LocalDate.of(2026, 10, 17);
      quoteData.prepareMetrics(cotizable, marketDate);
      quoteData.persistInitial(cotizable, marketDate, Instant.parse("2026-10-17T09:00:00Z"));
      quoteData.persistRecomputed(
          cotizable, LocalDate.of(2026, 10, 20), 130L, Instant.parse("2026-10-20T09:00:00Z"));
      quoteData.persistInitial(withoutMetrics, marketDate, Instant.parse("2026-10-17T09:00:00Z"));

      // Act
      List<PlayerRankingEntry> ranking = queryService.findRanking();

      // Assert
      assertThat(ranking)
          .extracting(PlayerRankingEntry::playerId)
          .containsExactly(cotizable.getId());
    }
  }

  @Nested
  @DisplayName("Desempate ante empate de fecha de vigencia e instante de cálculo")
  class TieBreak {

    /**
     * El desempate entre dos filas del mismo jugador que coinciden en fecha de vigencia e instante
     * de cálculo queda cerrado por el identificador de la fila, de modo que el jugador aparece una
     * sola vez en el ranking y su cotización vigente es siempre la misma fila.
     */
    @Test
    @DisplayName(
        "Muestra una sola entrada por jugador cuando dos filas coinciden en fecha e instante")
    void showsSingleEntryWhenRowsTie() {
      // Arrange
      catalogue.prepare();
      Player player = playerCatalogue.getActivePlayers(0, 10).getContent().getFirst();
      LocalDate marketDate = LocalDate.of(2026, 10, 17);
      Instant calculatedAt = Instant.parse("2026-10-17T09:00:00Z");
      quoteData.prepareMetrics(player, marketDate);
      quoteData.persistInitial(player, marketDate, calculatedAt);
      quoteData.persistCarriedForward(player, marketDate, 100L, calculatedAt);

      // Act
      List<PlayerRankingEntry> ranking = queryService.findRanking();

      // Assert
      assertThat(ranking).extracting(PlayerRankingEntry::playerId).containsExactly(player.getId());
      assertThat(ranking.getFirst().origin()).isEqualTo(QuoteOrigin.CARRIED_FORWARD);
      assertThat(queryService.findCurrent(player.getId()).getOrigin())
          .isEqualTo(QuoteOrigin.CARRIED_FORWARD);
      assertThat(queryService.findHistory(player.getId())).hasSize(2);
    }

    @Test
    @DisplayName("Conserva una sola entrada por jugador cuando conviven los tres orígenes")
    void showsSingleEntryWithEveryOrigin() {
      // Arrange
      catalogue.prepare();
      Player player = playerCatalogue.getActivePlayers(0, 10).getContent().getFirst();
      LocalDate marketDate = LocalDate.of(2026, 10, 17);
      quoteData.prepareMetrics(player, marketDate);
      quoteData.persistInitial(player, marketDate, Instant.parse("2026-10-17T09:00:00Z"));
      quoteData.persistCarriedForward(
          player, LocalDate.of(2026, 10, 18), 100L, Instant.parse("2026-10-18T09:00:00Z"));
      quoteData.persistRecomputed(
          player, LocalDate.of(2026, 10, 19), 105L, Instant.parse("2026-10-19T09:00:00Z"));

      // Act
      List<PlayerRankingEntry> ranking = queryService.findRanking();

      // Assert
      assertThat(ranking).extracting(PlayerRankingEntry::playerId).containsExactly(player.getId());
      assertThat(ranking.getFirst().origin()).isEqualTo(QuoteOrigin.RECOMPUTED);
      assertThat(ranking.getFirst().price()).isEqualTo(105L);
    }
  }

  private Player prepareRecalculatedMarket() {
    catalogue.prepare();
    data.prepareConfirmedJsonCampaign(LocalDate.of(2026, 10, 18));
    valuation.evaluate();
    initialization.initialize(LocalDate.of(2026, 10, 17));
    recalculation.recalculate("POSITIONAL_PERFORMANCE", null, null, null);
    return firstPlayer();
  }

  private Player prepareMarketWithBothOrigins() {
    catalogue.prepare();
    data.prepareConfirmedJsonCampaign(LocalDate.of(2026, 10, 18));
    initialization.initialize(LocalDate.of(2026, 10, 17));
    Player player = firstPlayer();
    LocalDate quoteDate = LocalDate.of(2026, 10, 18);
    publisher.publish(
        player,
        quoteDate,
        ValuationStrategy.POSITIONAL_PERFORMANCE,
        new BigDecimal("0.10"),
        new BigDecimal("0.10"),
        1L,
        Instant.parse("2026-10-18T09:00:00Z"));
    valuation.evaluate();
    publisher.publish(
        player,
        quoteDate,
        ValuationStrategy.POSITIONAL_PERFORMANCE,
        new BigDecimal("0.10"),
        new BigDecimal("0.10"),
        1L,
        Instant.parse("2026-10-18T10:00:00Z"));
    return player;
  }

  private Player firstPlayer() {
    return playerCatalogue.getActivePlayers(0, 10).getContent().getFirst();
  }
}
