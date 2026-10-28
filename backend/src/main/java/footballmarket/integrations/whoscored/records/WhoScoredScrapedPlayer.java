package footballmarket.integrations.whoscored.records;

import java.util.List;

/** Resultado completo del recorrido externo; no contiene entidades ni DTO HTTP. */
public record WhoScoredScrapedPlayer(
    String providerPlayerId,
    List<WhoScoredScrapedContextRow> rows,
    WhoScoredStructuredDataBlock structured,
    long providerCalls,
    List<String> unknownColumns) {
  public WhoScoredScrapedPlayer {
    rows = List.copyOf(rows);
    unknownColumns = List.copyOf(unknownColumns);
  }
}
