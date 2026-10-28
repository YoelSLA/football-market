package footballmarket.features.statistics.infraestructure.config;

import footballmarket.features.statistics.application.exceptions.PlayerStatisticsSynchronizationException;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Configuración validada de los dos canales de estadísticas WhoScored. */
@ConfigurationProperties(prefix = "whoscored.statistics")
public record PlayerStatisticsProperties(
    int scrapingWorkers,
    int jsonWorkers,
    long requestIntervalMs,
    long scrapingBatchTimeoutMs,
    long navigationTimeoutMs,
    long queryTimeoutMs,
    boolean filterThirdPartyResources,
    boolean headed) {

  public PlayerStatisticsProperties {
    if (scrapingWorkers < 1
        || jsonWorkers < 1
        || requestIntervalMs < 1
        || scrapingBatchTimeoutMs < 1
        || navigationTimeoutMs < 1
        || queryTimeoutMs < 1
        || !headed) {
      throw new PlayerStatisticsSynchronizationException();
    }
  }
}
