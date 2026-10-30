package footballmarket.features.identity.domain.records;

/** Contexto íntegro de una observación; nunca se combina nombre y equipo de filas distintas. */
public record WhoScoredIdentityContext(
    String name,
    String teamId,
    String teamName,
    int tournamentId,
    String tournamentName,
    Long leagueId,
    String leagueName,
    String seasonId,
    String seasonName) {}
