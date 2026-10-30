package footballmarket.services;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import footballmarket.features.catalog.application.PlayerCatalogService;
import footballmarket.features.statistics.application.PlayerStatisticsExtractionService;
import footballmarket.integrations.whoscored.WhoScoredStatisticsScrapingIntegration;
import footballmarket.integrations.whoscored.exceptions.HeadedBrowserUnavailableException;
import footballmarket.support.TestcontainersConfiguration;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@org.junit.jupiter.api.Tag("integration")
class HeadedBrowserStartupTest {
  @Autowired private PlayerStatisticsExtractionService extraction;
  @Autowired private PlayerCatalogService catalog;
  @MockitoBean private WhoScoredStatisticsScrapingIntegration integration;

  @Nested
  @DisplayName("Arranque sin navegador y aislamiento de fallos")
  class Startup {
    @Test
    @DisplayName("Crear el contexto Spring no solicita navegador ni consulta al proveedor")
    void startsWithoutBrowser() {
      // Assert
      assertThat(extraction).isNotNull();
      verifyNoInteractions(integration);
    }

    @Test
    @DisplayName("Un fallo al abrir scraping no impide consultar el catálogo mediante su Service")
    void browserFailureDoesNotBlockCatalog() {
      // Arrange
      when(integration.openSession()).thenThrow(new HeadedBrowserUnavailableException());
      // Act / Assert
      assertThatThrownBy(() -> extraction.openBatch())
          .isInstanceOf(HeadedBrowserUnavailableException.class);
      // Verify
      assertThat(catalog.getActivePlayers(0, 1)).isNotNull();
      verify(integration).openSession();
      verifyNoMoreInteractions(integration);
    }
  }
}
