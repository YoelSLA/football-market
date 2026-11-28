package footballmarket.features.statistics.application.scraping;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import footballmarket.features.statistics.application.scraping.internal.ScrapingMetricAssembler;
import footballmarket.integrations.whoscored.exceptions.WhoScoredStatisticsException;
import footballmarket.integrations.whoscored.records.WhoScoredScrapedContextRow;
import footballmarket.integrations.whoscored.records.WhoScoredScrapedMetricCell;
import footballmarket.integrations.whoscored.records.WhoScoredScrapedPlayer;
import footballmarket.integrations.whoscored.records.WhoScoredStructuredDataBlock;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@org.junit.jupiter.api.Tag("unit")
@DisplayName("Ensamblado de métricas de scraping")
class ScrapingMetricAssemblerTest {
  private final ScrapingMetricAssembler assembler = new ScrapingMetricAssembler();

  @Nested
  @DisplayName("Contador de apariciones")
  class AppearanceCounter {

    @Test
    @DisplayName("Conserva el contador principal cuando incluye suplencias")
    void extractsAppearanceCountWithSubstitutions() {
      // Act
      List<footballmarket.features.statistics.application.shared.internal.PlayerMetricsContext>
          contexts = assembler.assemble(scraped("6 (1)"));

      // Assert
      assertThat(contexts.getFirst().metrics().get("appearances_total").value())
          .isEqualByComparingTo("6");
    }

    @Test
    @DisplayName("Rechaza un contador mal formado de longitud extrema")
    void rejectsLongMalformedAppearanceCount() {
      // Arrange
      String appearances = "6".repeat(10_000) + "(x)";

      // Act / Assert
      assertThatThrownBy(() -> assembler.assemble(scraped(appearances)))
          .isInstanceOf(WhoScoredStatisticsException.class);
    }
  }

  private WhoScoredScrapedPlayer scraped(String appearances) {
    WhoScoredScrapedContextRow row =
        new WhoScoredScrapedContextRow(
            "initial",
            "22",
            "11",
            "2025",
            "2024/2025",
            "2024/2025",
            List.of(new WhoScoredScrapedMetricCell("apps", appearances)),
            Instant.parse("2026-11-27T00:00:00Z"));
    return new WhoScoredScrapedPlayer(
        "player-1", List.of(row), new WhoScoredStructuredDataBlock(List.of()), 1, List.of());
  }
}
