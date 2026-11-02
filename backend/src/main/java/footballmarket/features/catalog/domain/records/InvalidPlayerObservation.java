package footballmarket.features.catalog.domain.records;

/** Jugador identificable presente pero no procesable; nunca implica ausencia. */
public record InvalidPlayerObservation(
    String externalId, String teamExternalId, String teamName, String cause) {}
