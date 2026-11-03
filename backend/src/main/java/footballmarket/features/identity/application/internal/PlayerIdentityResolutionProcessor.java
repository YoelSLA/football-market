package footballmarket.features.identity.application.internal;

import footballmarket.features.identity.domain.enums.PlayerIdentityItemResult;
import footballmarket.features.identity.domain.models.PlayerIdentitySyncItem;
import footballmarket.features.identity.domain.models.WhoScoredIdentityMatcher;
import footballmarket.features.identity.domain.models.WhoScoredTeamAliases;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Component;

/** Decide globalmente antes de confirmar referencias aisladas, sin almacenar decisiones. */
@Component
public class PlayerIdentityResolutionProcessor {
  private final PlayerIdentityReferenceConfirmation confirmation;

  public PlayerIdentityResolutionProcessor(PlayerIdentityReferenceConfirmation confirmation) {
    this.confirmation = confirmation;
  }

  /** Resultados exclusivos por ID local y señal de interrupción que restaura la fachada. */
  public record Resolution(Map<Long, PlayerIdentityItemResult> results, boolean interrupted) {
    public Resolution {
      results = Map.copyOf(results);
    }
  }

  public Resolution resolve(
      PlayerIdentitySnapshotCapture.Capture capture,
      PlayerIdentityLeagueDiscoveryProcessor.Discovery discovery) {
    Map<Long, WhoScoredIdentityMatcher.Decision> decisions =
        new WhoScoredIdentityMatcher(WhoScoredTeamAliases.normative())
            .match(capture.items(), discovery.candidates(), discovery.completedLeagues());
    Map<Long, PlayerIdentityItemResult> results = new LinkedHashMap<>();
    boolean interrupted = discovery.interrupted();
    for (PlayerIdentitySyncItem item : capture.items()) {
      interrupted |= Thread.interrupted();
      WhoScoredIdentityMatcher.Decision decision = decisions.get(item.getPlayerId());
      if (interrupted && item.getInitialExternalIds().isEmpty()) {
        results.put(item.getPlayerId(), PlayerIdentityItemResult.INTERRUPTED);
      } else {
        results.put(item.getPlayerId(), this.confirmation.confirm(item.getPlayerId(), decision));
      }
    }
    interrupted |= Thread.interrupted();
    return new Resolution(results, interrupted);
  }
}
