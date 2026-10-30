package footballmarket.features.identity.presentation;

import footballmarket.features.identity.domain.models.PlayerIdentityLeagueDiscovery;
import footballmarket.features.identity.domain.records.PlayerIdentitySyncCounters;
import footballmarket.features.identity.domain.records.PlayerIdentitySyncSummary;
import footballmarket.features.identity.presentation.dtos.response.*;

/** Transformación estructural del resumen terminado al contrato HTTP. */
public final class PlayerIdentityMapper {
  private PlayerIdentityMapper() {}

  public static PlayerIdentitySyncRunResponseDTO toResponse(PlayerIdentitySyncSummary summary) {
    PlayerIdentitySyncCounters counters = summary.counters();
    return new PlayerIdentitySyncRunResponseDTO(
        summary.id(),
        summary.passType().name(),
        summary.sourceRunId(),
        summary.startedAt(),
        summary.finishedAt(),
        summary.status().name(),
        summary.failureReason(),
        new PlayerIdentitySyncCountersResponseDTO(
            counters.selectedPlayers(),
            counters.discoveredUnique(),
            counters.resolved(),
            counters.alreadyReferenced(),
            counters.conflicts(),
            counters.noMatch(),
            counters.ambiguous(),
            counters.localDataErrors(),
            counters.discoveryErrors(),
            counters.persistenceErrors(),
            counters.interruptedPlayers(),
            counters.pendingPlayers(),
            counters.providerErrors(),
            counters.discardedRows()),
        summary.leagues().stream().map(PlayerIdentityMapper::toLeague).toList());
  }

  private static PlayerIdentityLeagueDiscoveryResponseDTO toLeague(
      PlayerIdentityLeagueDiscovery league) {
    return new PlayerIdentityLeagueDiscoveryResponseDTO(
        league.getLeagueId(),
        league.getLeagueName(),
        league.getTournamentId(),
        league.getStatus().name(),
        league.getStartedAt(),
        league.getFinishedAt(),
        league.getPagesRead(),
        league.getLastPage(),
        league.getTotalPages(),
        league.getTotalResults(),
        league.getRowsRead(),
        league.getDiscardedRows(),
        league.getErrorCode(),
        league.getErrorMessage(),
        league.getFailedPage(),
        league.getHttpStatus());
  }
}
