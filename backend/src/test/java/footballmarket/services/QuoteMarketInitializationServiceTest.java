package footballmarket.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import footballmarket.models.records.MarketInitializationResult;
import footballmarket.services.exceptions.MarketAlreadyInitializedException;
import footballmarket.support.IdentityCatalogueFixture;
import footballmarket.support.ResetPlayerCatalogListener;
import footballmarket.support.TestcontainersConfiguration;
import footballmarket.support.ValuationTestData;
import java.time.LocalDate;
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
  @Autowired private IdentityCatalogueFixture catalogue;
  @Autowired private ValuationTestData data;

  @Nested
  @DisplayName("Inicialización del mercado")
  class Initialization {

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
