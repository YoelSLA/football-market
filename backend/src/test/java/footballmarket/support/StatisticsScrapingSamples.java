package footballmarket.support;

import footballmarket.integrations.records.*;
import java.time.Instant;
import java.util.List;

/** Respuestas controladas coherentes con IdentityCatalogueFixture e IdentityControlledProvider. */
public final class StatisticsScrapingSamples {
  private StatisticsScrapingSamples() {}

  public static String tournamentOf(String externalId) {
    return List.of("2", "3", "4", "5", "22").get(Integer.parseInt(externalId) - 1001);
  }

  public static WhoScoredScrapedPlayer player(String externalId) {
    WhoScoredScrapedMetricCell apps = new WhoScoredScrapedMetricCell("apps", "6");
    WhoScoredScrapedMetricCell goal = new WhoScoredScrapedMetricCell("goal", "-");
    return player(externalId, List.of(apps, goal));
  }

  public static WhoScoredScrapedPlayer player(
      String externalId, List<WhoScoredScrapedMetricCell> cells) {
    WhoScoredScrapedContextRow row =
        new WhoScoredScrapedContextRow(
            "initial",
            tournamentOf(externalId),
            "200",
            "11",
            "2025/2026",
            null,
            cells,
            Instant.parse("2026-10-15T12:00:00Z"));
    return new WhoScoredScrapedPlayer(
        externalId, List.of(row), new WhoScoredStructuredDataBlock(List.of()), 2, List.of());
  }
}
