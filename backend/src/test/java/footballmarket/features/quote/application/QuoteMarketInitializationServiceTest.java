package footballmarket.features.quote.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import footballmarket.features.market.application.exceptions.MarketAlreadyInitializedException;
import footballmarket.features.market.domain.records.MarketInitializationResult;
import footballmarket.support.IdentityCatalogueFixture;
import footballmarket.support.ResetPlayerCatalogListener;
import footballmarket.support.TestcontainersConfiguration;
import footballmarket.support.ValuationTestData;
import java.time.LocalDate;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
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
  ValuationTestData.class
})
@TestExecutionListeners(
    listeners = ResetPlayerCatalogListener.class,
    mergeMode = TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS)
@org.junit.jupiter.api.Tag("integration")
class QuoteMarketInitializationServiceTest {
  private static final LocalDate MARKET_DATE = LocalDate.of(2026, 10, 20);

  @Autowired private QuoteMarketInitializationService service;
  @Autowired private QuoteRecalculationService recalculation;
  @Autowired private IdentityCatalogueFixture catalogue;
  @Autowired private ValuationTestData data;

  @Nested
  @DisplayName("Inicialización del mercado")
  class Initialization {

    @Test
    @DisplayName(
        "La inicialización idempotente concurrente con recálculo conserva un ancla por jugador")
    void initializationAlongsidePublicationKeepsAnchor() throws Exception {
      // Arrange
      catalogue.prepare();
      data.prepareConfirmedJsonCampaign(LocalDate.of(2026, 10, 18));
      service.initialize(MARKET_DATE);
      CountDownLatch start = new CountDownLatch(1);
      ExecutorService executor = Executors.newFixedThreadPool(2);
      try {
        Future<MarketInitializationResult> initialization =
            executor.submit(
                () -> {
                  if (!start.await(10, TimeUnit.SECONDS)) {
                    throw new AssertionError("No se liberó la inicialización");
                  }
                  return service.initialize(MARKET_DATE);
                });
        Future<footballmarket.features.quote.domain.models.QuoteExecution> publication =
            executor.submit(
                () -> {
                  if (!start.await(10, TimeUnit.SECONDS)) {
                    throw new AssertionError("No se liberó la publicación");
                  }
                  return recalculation.recalculateAutomatic();
                });
        // Act
        start.countDown();
        MarketInitializationResult result = initialization.get(30, TimeUnit.SECONDS);
        publication.get(30, TimeUnit.SECONDS);
        // Assert
        assertThat(result.alreadyInitialized()).isTrue();
        assertThat(result.initializedPlayers()).isEqualTo(5);
        assertThat(service.initialize(MARKET_DATE).initializedPlayers()).isEqualTo(5);
      } finally {
        executor.shutdownNow();
      }
    }

    @Test
    @DisplayName("Inicializaciones concurrentes conservan un ancla única e idempotencia")
    void concurrentInitializationIsIdempotent() throws Exception {
      // Arrange
      catalogue.prepare();
      data.prepareConfirmedJsonCampaign(LocalDate.of(2026, 10, 18));
      CountDownLatch start = new CountDownLatch(1);
      ExecutorService executor = Executors.newFixedThreadPool(2);
      try {
        java.util.concurrent.Callable<MarketInitializationResult> initialize =
            () -> {
              if (!start.await(10, TimeUnit.SECONDS)) {
                throw new AssertionError("No se liberó el inicio concurrente");
              }
              return service.initialize(MARKET_DATE);
            };
        Future<MarketInitializationResult> first = executor.submit(initialize);
        Future<MarketInitializationResult> second = executor.submit(initialize);
        // Act
        start.countDown();
        MarketInitializationResult one = first.get(30, TimeUnit.SECONDS);
        MarketInitializationResult two = second.get(30, TimeUnit.SECONDS);
        // Assert
        assertThat(one.initializedPlayers()).isEqualTo(5);
        assertThat(two.initializedPlayers()).isEqualTo(5);
        assertThat(one.alreadyInitialized()).isNotEqualTo(two.alreadyInitialized());
        assertThat(service.initialize(MARKET_DATE).initializedPlayers()).isEqualTo(5);
      } finally {
        executor.shutdownNow();
      }
    }

    @Test
    @DisplayName("Deja una cotización inicial por cada jugador con métricas informadas")
    void initializesEveryPricablePlayer() {
      // Arrange
      catalogue.prepare();
      data.prepareConfirmedJsonCampaign(LocalDate.of(2026, 10, 18));

      // Act
      MarketInitializationResult result = service.initialize(MARKET_DATE);

      // Assert
      assertThat(result.marketDate()).isEqualTo(MARKET_DATE);
      assertThat(result.initializedPlayers()).isEqualTo(5);
      assertThat(result.alreadyInitialized()).isFalse();
    }

    @Test
    @DisplayName("No inicializa a los jugadores sin métricas ni con métricas sin informar")
    void skipsPlayersWithoutReportedMetrics() {
      // Arrange
      catalogue.prepare();
      data.preparePartialJsonCampaign(LocalDate.of(2026, 10, 18));

      // Act
      MarketInitializationResult result = service.initialize(MARKET_DATE);

      // Assert
      assertThat(result.initializedPlayers()).isEqualTo(3);
      assertThat(result.alreadyInitialized()).isFalse();
    }

    @Test
    @DisplayName("Repite la inicialización en la misma fecha de mercado sin duplicar filas")
    void repeatsInitializationOnTheSameMarketDate() {
      // Arrange
      catalogue.prepare();
      data.prepareConfirmedJsonCampaign(LocalDate.of(2026, 10, 18));
      service.initialize(MARKET_DATE);

      // Act
      MarketInitializationResult result = service.initialize(MARKET_DATE);

      // Assert
      assertThat(result.marketDate()).isEqualTo(MARKET_DATE);
      assertThat(result.initializedPlayers()).isEqualTo(5);
      assertThat(result.alreadyInitialized()).isTrue();
    }

    @Test
    @DisplayName("Rechaza la inicialización en una fecha de mercado distinta de la vigente")
    void rejectsAnotherMarketDate() {
      // Arrange
      catalogue.prepare();
      data.prepareConfirmedJsonCampaign(LocalDate.of(2026, 10, 18));
      service.initialize(MARKET_DATE);

      // Act & Assert
      assertThatThrownBy(() -> service.initialize(MARKET_DATE.plusDays(1)))
          .isInstanceOf(MarketAlreadyInitializedException.class);
    }
  }
}
