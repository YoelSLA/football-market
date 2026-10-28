package footballmarket.integrations.whoscored.records;

import java.util.List;

/** Página adaptada con huella de filas originales para controlar progreso. */
public record WhoScoredCatalogPage(
    List<WhoScoredPlayerIdentity> identities,
    List<WhoScoredDiscardedIdentityRow> discardedRows,
    WhoScoredPaging paging,
    String fingerprint) {
  public WhoScoredCatalogPage {
    identities = List.copyOf(identities);
    discardedRows = List.copyOf(discardedRows);
  }
}
