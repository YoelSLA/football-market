package footballmarket.integrations.whoscored.records;

/** Datos externos de identidad; no son un Model ni un DTO HTTP propio. */
public record WhoScoredPlayerIdentity(
    String externalId,
    String name,
    String teamId,
    String teamName,
    int tournamentId,
    String tournamentName,
    String seasonId,
    String seasonName) {}
