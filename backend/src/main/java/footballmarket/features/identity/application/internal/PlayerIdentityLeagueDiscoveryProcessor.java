package footballmarket.features.identity.application.internal;

import footballmarket.features.identity.domain.enums.PlayerIdentityPassType;
import footballmarket.features.identity.domain.models.WhoScoredIdentityCandidate;
import footballmarket.features.identity.domain.records.WhoScoredIdentityContext;
import footballmarket.integrations.whoscored.WhoScoredIntegration;
import footballmarket.integrations.whoscored.config.WhoScoredProperties;
import footballmarket.integrations.whoscored.exceptions.WhoScoredIntegrationException;
import footballmarket.integrations.whoscored.records.WhoScoredCatalogPage;
import footballmarket.integrations.whoscored.records.WhoScoredPaging;
import footballmarket.integrations.whoscored.records.WhoScoredPlayerIdentity;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;

/** Reúne en memoria candidatos de ligas completas; nunca publica páginas parciales. */
@Component
public class PlayerIdentityLeagueDiscoveryProcessor {
  private final WhoScoredIntegration integration;

  public PlayerIdentityLeagueDiscoveryProcessor(WhoScoredIntegration integration) {
    this.integration = integration;
  }

  /** Solo conserva la completitud necesaria para decidir y calcular el estado del pase. */
  public record Discovery(
      List<WhoScoredIdentityCandidate> candidates,
      Set<Long> completedLeagues,
      boolean interrupted) {
    public Discovery {
      candidates = List.copyOf(candidates);
      completedLeagues = Set.copyOf(completedLeagues);
    }
  }

  public Discovery discover(PlayerIdentitySnapshotCapture.Capture capture) {
    Map<String, WhoScoredIdentityCandidate> candidates = new LinkedHashMap<>();
    Set<Long> completed = new HashSet<>();
    boolean interrupted = Thread.interrupted();
    boolean requiresDiscovery =
        capture.items().stream().anyMatch(item -> item.getInitialExternalIds().isEmpty());
    if (!requiresDiscovery) {
      return new Discovery(List.of(), Set.of(), interrupted);
    }
    for (WhoScoredProperties.Competition binding : capture.competitions()) {
      if (interrupted || Thread.interrupted()) {
        interrupted = true;
        continue;
      }
      List<WhoScoredPlayerIdentity> rows = new ArrayList<>();
      Set<String> fingerprints = new HashSet<>();
      WhoScoredPaging previous = null;
      int page = 1;
      try {
        while (true) {
          WhoScoredCatalogPage response =
              capture.passType() == PlayerIdentityPassType.CURRENT
                  ? this.integration.currentPage(binding.tournamentId(), page)
                  : this.integration.historicalPage(binding.tournamentId(), page);
          WhoScoredPaging paging = response.paging();
          if (previous != null
              && (previous.totalPages() != paging.totalPages()
                  || previous.totalResults() != paging.totalResults()
                  || previous.resultsPerPage() != paging.resultsPerPage())) {
            throw new WhoScoredIntegrationException("PROVIDER_INVALID_RESPONSE", null, false);
          }
          if (!fingerprints.addAll(Arrays.asList(response.fingerprint().split(",")))) {
            throw new WhoScoredIntegrationException("PROVIDER_NO_PROGRESS", null, false);
          }
          rows.addAll(response.identities());
          previous = paging;
          if (paging.totalResults() == 0 || page == paging.totalPages()) {
            break;
          }
          page++;
        }
        for (WhoScoredPlayerIdentity row : rows) {
          candidates
              .computeIfAbsent(row.externalId(), WhoScoredIdentityCandidate::new)
              .observe(new WhoScoredIdentityContext(row.name(), row.teamName()));
        }
        completed.add(binding.leagueId());
      } catch (WhoScoredIntegrationException failure) {
        if (failure.isInterrupted()) {
          interrupted = true;
          Thread.interrupted();
        }
      }
    }
    return new Discovery(List.copyOf(candidates.values()), completed, interrupted);
  }
}
