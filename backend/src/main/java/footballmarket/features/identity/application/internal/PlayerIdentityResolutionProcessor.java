package footballmarket.features.identity.application.internal;

import footballmarket.features.identity.domain.enums.*;
import footballmarket.features.identity.domain.enums.PlayerIdentityItemResult;
import footballmarket.features.identity.domain.enums.PlayerIdentityLeagueStatus;
import footballmarket.features.identity.domain.models.*;
import footballmarket.features.identity.domain.records.*;
import footballmarket.features.identity.infraestructure.*;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/** Matching global fuera de transacción, luego confirmaciones aisladas por jugador. */
@Component
public class PlayerIdentityResolutionProcessor {
  private final WhoScoredIdentityCandidateRepository candidates;
  private final PlayerIdentityLeagueDiscoveryRepository leagues;
  private final PlayerIdentityReferenceConfirmation confirmation;

  public PlayerIdentityResolutionProcessor(
      WhoScoredIdentityCandidateRepository candidates,
      PlayerIdentityLeagueDiscoveryRepository leagues,
      PlayerIdentityReferenceConfirmation confirmation) {
    this.candidates = candidates;
    this.leagues = leagues;
    this.confirmation = confirmation;
  }

  public boolean resolve(PlayerIdentitySnapshotCapture.Capture capture, boolean interrupted) {
    List<WhoScoredIdentityCandidate> observed =
        this.candidates.findByRunIdOrderById(capture.runId());
    Set<Long> completed =
        this.leagues.findByRunIdOrderById(capture.runId()).stream()
            .filter(league -> league.getStatus() == PlayerIdentityLeagueStatus.COMPLETED)
            .map(PlayerIdentityLeagueDiscovery::getLeagueId)
            .collect(Collectors.toSet());
    Map<Long, WhoScoredIdentityMatcher.Decision> decisions =
        new WhoScoredIdentityMatcher(WhoScoredTeamAliases.normative())
            .match(capture.items(), observed, completed);
    for (PlayerIdentitySyncItem item : capture.items()) {
      WhoScoredIdentityMatcher.Decision decision = decisions.get(item.getPlayerId());
      if (Thread.interrupted()) {
        interrupted = true;
      }
      if (interrupted) {
        decision =
            new WhoScoredIdentityMatcher.Decision(
                PlayerIdentityItemResult.INTERRUPTED,
                "EXECUTION_INTERRUPTED",
                decision.candidates(),
                null);
      }
      this.confirmation.confirm(item.getId(), decision);
    }
    return interrupted;
  }
}
