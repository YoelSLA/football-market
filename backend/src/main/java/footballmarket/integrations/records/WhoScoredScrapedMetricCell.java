package footballmarket.integrations.records;

/** Celda publicada, conservada como texto decimal hasta su normalización de dominio. */
public record WhoScoredScrapedMetricCell(String providerField, String text) {}
