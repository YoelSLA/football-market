package footballmarket.features.statistics.application.internal;

import footballmarket.integrations.whoscored.WhoScoredStatisticsScrapingIntegration;
import org.springframework.stereotype.Component;

/** Comprueba la disponibilidad únicamente cuando el caso de uso abre un lote de scraping. */
@Component
public class HeadedBrowserAvailabilityChecker {
  private final WhoScoredStatisticsScrapingIntegration integration;

  public HeadedBrowserAvailabilityChecker(WhoScoredStatisticsScrapingIntegration integration) {
    this.integration = integration;
  }

  public WhoScoredStatisticsScrapingIntegration.Session open() {
    return this.integration.openSession();
  }
}
