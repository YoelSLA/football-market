package footballmarket.integrations.whoscored.records;

/** Metadatos de filas originales validados por el adaptador. */
public record WhoScoredPaging(
    int currentPage,
    int totalPages,
    int resultsPerPage,
    long totalResults,
    long firstRecordIndex,
    long lastRecordIndex) {}
