package footballmarket.models.records;

/** Presencia de equipo y relación recibida; un plantel indeterminable requiere protección. */
public record TeamCandidate(
    String externalId, String name, String leagueExternalId, boolean rosterKnown) {}
