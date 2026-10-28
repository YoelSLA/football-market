package footballmarket.integrations.whoscored.records;

/** Posición y motivo controlado de una fila descartada, sin contenido crudo. */
public record WhoScoredDiscardedIdentityRow(int rowPosition, String reasonCode) {}
