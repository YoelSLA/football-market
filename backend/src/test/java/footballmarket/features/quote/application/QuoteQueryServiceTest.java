package footballmarket.features.quote.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import footballmarket.features.catalog.application.PlayerCatalogService;
import footballmarket.features.catalog.domain.models.player.Player;
import footballmarket.features.quote.application.exceptions.QuoteNotFoundException;
import footballmarket.features.quote.domain.enums.QuoteOrigin;
import footballmarket.features.quote.domain.models.PlayerQuote;
import footballmarket.features.quote.domain.models.QuoteAdjustment;
import footballmarket.features.quote.domain.records.PlayerRankingEntry;
import footballmarket.features.quote.domain.records.QuoteRankingPage;
import footballmarket.features.quote.domain.records.QuoteRankingQuery;
import footballmarket.features.valuation.application.ValuationEvaluationService;
import footballmarket.features.valuation.domain.enums.ValuationStrategy;
import footballmarket.support.IdentityCatalogueFixture;
import footballmarket.support.QuoteTestData;
import footballmarket.support.ResetPlayerCatalogListener;
import footballmarket.support.TestcontainersConfiguration;
import footballmarket.support.ValuationTestData;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.bean.override.convention.TestBean;

@SpringBootTest(
    properties = {
      "market.quote.strategy=POSITIONAL_PERFORMANCE",
      "market.quote.adjustment-factor=0.10",
      "market.quote.variation-cap=0.10",
      "market.quote.min-price-floor=1"
    })
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
    return new QueryClock();
  }

  @BeforeEach
  void resetMarketClock() {
    QueryClock clock = (QueryClock) marketClock;
    clock.time.set(Instant.parse("2026-10-18T09:00:00Z"));
    clock.pauseThread = null;
    clock.captured = new CountDownLatch(1);
    clock.resume = new CountDownLatch(1);
  }

  @Nested
  @DisplayName("Historial de cotizaciones")
  class History {

    @Test
    @DisplayName("El score antiguo se aplica por día y conserva su fecha de origen en el historial")
    void oldScoreHasSeparateDailyValidity() {
      // Arrange
      Player player = prepareRecalculatedMarket();
      ((QueryClock) marketClock).time.set(Instant.parse("2026-10-19T09:00:00Z"));
      recalculation.recalculate("POSITIONAL_PERFORMANCE", null, null, null);
      // Act
      List<PlayerQuote> history = queryService.findHistory(player.getId());
      // Assert
      assertThat(history).hasSize(3);
      PlayerQuote today = history.get(0);
      PlayerQuote yesterday = history.get(1);
      assertThat(today.getEffectiveDate()).isEqualTo(LocalDate.of(2026, 10, 19));
      assertThat(today.getSourceValuationDate()).isEqualTo(yesterday.getSourceValuationDate());
      assertThat(today.getScore()).isEqualByComparingTo(yesterday.getScore());
      assertThat(today.getPreviousPrice()).isEqualTo(yesterday.getPrice());
      QuoteAdjustment expected =
          QuoteAdjustment.calculate(
              yesterday.getPrice(),
              today.getScore(),
              new BigDecimal("0.10"),
              new BigDecimal("0.10"),
              1L);
      assertThat(today.getPrice()).isEqualTo(expected.price());
      assertThat(queryService.findOnDate(player.getId(), LocalDate.of(2026, 10, 18)).getId())
          .isEqualTo(yesterday.getId());
    }

    @Test
    @DisplayName("Una corrida atrasada no inserta historia ni cambia el precio ya publicado en D+1")
    void delayedRunCannotRewriteFutureQuote() throws Exception {
      // Arrange
      catalogue.prepare();
      data.prepareConfirmedJsonCampaign(LocalDate.of(2026, 10, 17));
      valuation.evaluate();
      initialization.initialize(LocalDate.of(2026, 10, 16));
      Player player = firstPlayer();
      QueryClock clock = (QueryClock) marketClock;
      clock.pauseThread = "delayed-quote";
      ExecutorService executor =
          Executors.newSingleThreadExecutor(task -> new Thread(task, "delayed-quote"));
      try {
        Future<footballmarket.features.quote.domain.models.QuoteExecution> delayed =
            executor.submit(
                () -> recalculation.recalculate("POSITIONAL_PERFORMANCE", null, null, null));
        if (!clock.captured.await(10, TimeUnit.SECONDS)) {
          throw new AssertionError("La corrida atrasada no capturó su fecha");
        }
        clock.time.set(Instant.parse("2026-10-19T09:00:00Z"));
        recalculation.recalculateAutomatic();
        clock.resume.countDown();
        delayed.get(30, TimeUnit.SECONDS);
        // Act
        List<PlayerQuote> history = queryService.findHistory(player.getId());
        PlayerQuote current = queryService.findCurrent(player.getId());
        // Assert
        assertThat(history).hasSize(2);
        assertThat(history)
            .extracting(PlayerQuote::getEffectiveDate)
            .containsExactly(LocalDate.of(2026, 10, 19), LocalDate.of(2026, 10, 16));
        assertThat(current.getId()).isEqualTo(history.getFirst().getId());
        assertThat(current.getPreviousPrice()).isEqualTo(100L);
        QuoteAdjustment expected =
            QuoteAdjustment.calculate(
                100L, current.getScore(), new BigDecimal("0.10"), new BigDecimal("0.10"), 1L);
        assertThat(current.getPrice()).isEqualTo(expected.price());
      } finally {
        clock.resume.countDown();
        executor.shutdownNow();
      }
    }

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

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    @DisplayName(
        "Las entradas manual y automática publican los mismos precios para los mismos insumos")
    void bothEntriesPublishEquivalentPrices(boolean automatic) {
      // Arrange: el recálculo prepara el estado; el caso bajo prueba es su lectura pública.
      catalogue.prepare();
      data.prepareConfirmedJsonCampaign(LocalDate.of(2026, 10, 18));
      valuation.evaluate();
      initialization.initialize(LocalDate.of(2026, 10, 17));
      List<Player> players = playerCatalogue.getActivePlayers(0, 10).getContent();
      if (automatic) {
        recalculation.recalculateAutomatic();
      } else {
        recalculation.recalculate(
            "POSITIONAL_PERFORMANCE", new BigDecimal("0.10"), new BigDecimal("0.10"), 1L);
      }
      // Act / Assert
      for (Player player : players) {
        PlayerQuote current = queryService.findCurrent(player.getId());
        assertThat(current.getOrigin()).isEqualTo(QuoteOrigin.RECOMPUTED);
        assertThat(current.getStrategyName()).isEqualTo(ValuationStrategy.POSITIONAL_PERFORMANCE);
        assertThat(current.getScore()).isNotNull();
        QuoteAdjustment expected =
            QuoteAdjustment.calculate(
                100L, current.getScore(), new BigDecimal("0.10"), new BigDecimal("0.10"), 1L);
        assertThat(current.getPrice()).isEqualTo(expected.price());
        assertThat(current.getPreviousPrice()).isEqualTo(100L);
        assertThat(queryService.findHistory(player.getId())).hasSize(2);
      }
    }

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
  @DisplayName("Vigencia diaria sin coexistencia de orígenes")
  class Precedence {

    @Test
    @DisplayName("Una cotización del día siguiente es vigente sin sustituir la arrastrada anterior")
    void recomputedPrecedesCarriedForward() {
      Player player = prepareMarketWithBothOrigins();
      List<PlayerQuote> history = queryService.findHistory(player.getId());

      assertThat(history).hasSize(3);
      assertThat(history.get(0).getOrigin()).isEqualTo(QuoteOrigin.RECOMPUTED);
      assertThat(history.get(1).getOrigin()).isEqualTo(QuoteOrigin.CARRIED_FORWARD);
      assertThat(history.get(2).getOrigin()).isEqualTo(QuoteOrigin.INITIAL);
      assertThat(history.get(0).getCalculatedAt()).isEqualTo(Instant.parse("2026-10-19T09:00:00Z"));
      assertThat(queryService.findCurrent(player.getId()).getOrigin())
          .isEqualTo(QuoteOrigin.RECOMPUTED);
    }
  }

  @Nested
  @DisplayName("Ranking por precio vigente")
  class Ranking {

    @Test
    @DisplayName("La página filtrada conserva la posición global y los totales fuera de rango")
    void filteredPagePreservesGlobalPosition() {
      // Arrange
      catalogue.prepare();
      List<Player> players = playerCatalogue.getActivePlayers(0, 10).getContent();
      Player first = players.get(0);
      Player second = players.get(1);
      LocalDate day = LocalDate.of(2026, 10, 20);
      quoteData.prepareMetrics(first, day);
      quoteData.prepareMetrics(second, day);
      quoteData.persistInitial(first, day, Instant.parse("2026-10-20T09:00:00Z"));
      quoteData.persistInitial(second, day, Instant.parse("2026-10-20T09:00:00Z"));
      QuoteRankingQuery query = new QuoteRankingQuery(0, 1, second.getName(), null, null);
      QuoteRankingQuery outside = new QuoteRankingQuery(10, 1, second.getName(), null, null);
      // Act
      QuoteRankingPage page = queryService.findRanking(query);
      QuoteRankingPage empty = queryService.findRanking(outside);
      // Assert
      assertThat(page.content())
          .singleElement()
          .satisfies(
              entry -> {
                assertThat(entry.playerId()).isEqualTo(second.getId());
                assertThat(entry.position()).isEqualTo(first.getId() < second.getId() ? 2 : 1);
              });
      assertThat(page.totalElements()).isEqualTo(1);
      assertThat(page.totalPages()).isEqualTo(1);
      assertThat(empty.content()).isEmpty();
      assertThat(empty.totalElements()).isEqualTo(1);
      assertThat(empty.leagues()).isEqualTo(page.leagues());
    }

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
  @DisplayName("Selección de la cotización diaria vigente")
  class TieBreak {

    /**
     * El ranking conserva una sola entrada por jugador aunque varias fechas compartan instante de
     * cálculo: la fecha de vigencia más reciente determina la fila vigente.
     */
    @Test
    @DisplayName(
        "Muestra una sola entrada por jugador cuando dos fechas comparten instante de cálculo")
    void showsSingleEntryWhenRowsTie() {
      // Arrange
      catalogue.prepare();
      Player player = playerCatalogue.getActivePlayers(0, 10).getContent().getFirst();
      LocalDate marketDate = LocalDate.of(2026, 10, 17);
      Instant calculatedAt = Instant.parse("2026-10-17T09:00:00Z");
      quoteData.prepareMetrics(player, marketDate);
      quoteData.persistInitial(player, marketDate, calculatedAt);
      quoteData.persistCarriedForward(player, marketDate.plusDays(1), 100L, calculatedAt);

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
    quoteData.persistCarriedForward(
        player, LocalDate.of(2026, 10, 18), 100L, Instant.parse("2026-10-18T09:00:00Z"));
    quoteData.persistRecomputed(
        player, LocalDate.of(2026, 10, 19), 105L, Instant.parse("2026-10-19T09:00:00Z"));
    return player;
  }

  private Player firstPlayer() {
    return playerCatalogue.getActivePlayers(0, 10).getContent().getFirst();
  }

  static class QueryClock extends Clock {
    final AtomicReference<Instant> time =
        new AtomicReference<>(Instant.parse("2026-10-18T09:00:00Z"));
    volatile String pauseThread;
    CountDownLatch captured = new CountDownLatch(1);
    CountDownLatch resume = new CountDownLatch(1);

    @Override
    public ZoneId getZone() {
      return ZoneOffset.UTC;
    }

    @Override
    public Clock withZone(ZoneId zone) {
      return Clock.fixed(time.get(), zone);
    }

    @Override
    public Instant instant() {
      Instant instant = time.get();
      if (Thread.currentThread().getName().equals(pauseThread)) {
        pauseThread = null;
        captured.countDown();
        try {
          if (!resume.await(10, TimeUnit.SECONDS)) {
            throw new AssertionError("No se liberó la corrida atrasada");
          }
        } catch (InterruptedException exception) {
          Thread.currentThread().interrupt();
          throw new AssertionError("Se interrumpió la barrera", exception);
        }
      }
      return instant;
    }
  }
}
