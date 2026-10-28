package footballmarket.features.identity.application.internal;

import footballmarket.features.identity.application.exceptions.PlayerIdentitySynchronizationException;
import footballmarket.features.identity.domain.enums.*;
import footballmarket.features.identity.domain.models.*;
import footballmarket.features.identity.domain.records.*;
import footballmarket.features.identity.domain.records.WhoScoredIdentityContext;
import footballmarket.features.identity.infraestructure.*;
import footballmarket.integrations.whoscored.WhoScoredIntegration;
import footballmarket.integrations.whoscored.exceptions.WhoScoredIntegrationException;
import footballmarket.integrations.whoscored.records.*;
import java.time.Instant;
import java.util.*;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

/** Descubrimiento secuencial: solo publica candidatos después del recorrido íntegro de una liga. */
@Component
public class PlayerIdentityLeagueDiscoveryProcessor {
  private final WhoScoredIntegration integration;
  private final PlayerIdentityLeagueDiscoveryRepository leagues;
  private final PlayerIdentityDiscardedRowRepository discarded;
  private final WhoScoredIdentityCandidateRepository candidates;
  private final TransactionTemplate transaction;

  public PlayerIdentityLeagueDiscoveryProcessor(
      WhoScoredIntegration integration,
      PlayerIdentityLeagueDiscoveryRepository leagues,
      PlayerIdentityDiscardedRowRepository discarded,
      WhoScoredIdentityCandidateRepository candidates,
      PlatformTransactionManager manager) {
    this.integration = integration;
    this.leagues = leagues;
    this.discarded = discarded;
    this.candidates = candidates;
    this.transaction = new TransactionTemplate(manager);
    this.transaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
  }

  public boolean discover(PlayerIdentitySnapshotCapture.Capture capture) {
    boolean interrupted = false;
    for (PlayerIdentityLeagueDiscovery scope : capture.leagues()) {
      if (capture.items().isEmpty()) {
        this.persist(() -> this.leagues.findById(scope.getId()).orElseThrow().skip(Instant.now()));
        continue;
      }
      if (interrupted || Thread.currentThread().isInterrupted()) {
        interrupted = true;
        Thread.interrupted();
        this.fail(scope, "EXECUTION_INTERRUPTED", null, null);
        continue;
      }
      this.persist(() -> this.leagues.findById(scope.getId()).orElseThrow().start(Instant.now()));
      List<WhoScoredPlayerIdentity> completeRows = new ArrayList<>();
      Set<String> fingerprints = new HashSet<>();
      WhoScoredPaging previous = null;
      int page = 1;
      try {
        while (true) {
          WhoScoredCatalogPage response =
              capture.passType()
                      == footballmarket.features.identity.domain.enums.PlayerIdentityPassType
                          .CURRENT
                  ? this.integration.currentPage(scope.getTournamentId(), page)
                  : this.integration.historicalPage(scope.getTournamentId(), page);
          WhoScoredPaging paging = response.paging();
          if (previous != null
              && (previous.totalPages() != paging.totalPages()
                  || previous.totalResults() != paging.totalResults()
                  || previous.resultsPerPage() != paging.resultsPerPage())) {
            throw new WhoScoredIntegrationException("PROVIDER_INVALID_RESPONSE", null, false);
          }
          if (!fingerprints.addAll(java.util.Arrays.asList(response.fingerprint().split(",")))) {
            throw new WhoScoredIntegrationException("PROVIDER_NO_PROGRESS", null, false);
          }
          this.persist(
              () -> {
                PlayerIdentityLeagueDiscovery managed =
                    this.leagues.findById(scope.getId()).orElseThrow();
                managed.recordPage(
                    paging.currentPage(),
                    paging.totalPages(),
                    paging.totalResults(),
                    response.identities().size() + response.discardedRows().size(),
                    response.discardedRows().size());
                for (WhoScoredDiscardedIdentityRow row : response.discardedRows()) {
                  this.discarded.save(
                      new PlayerIdentityDiscardedRow(
                          managed, paging.currentPage(), row.rowPosition(), row.reasonCode()));
                }
              });
          completeRows.addAll(response.identities());
          previous = paging;
          if (paging.totalResults() == 0 || page == paging.totalPages()) {
            break;
          }
          page++;
        }
        this.publish(scope, completeRows);
      } catch (WhoScoredIntegrationException failure) {
        interrupted = failure.isInterrupted();
        if (interrupted) {
          Thread.interrupted();
        }
        // Un bloqueo o un límite por liga no impide continuar: las demás ligas se recorren.
        this.fail(
            scope,
            failure.getCode(),
            failure.getPage() != null ? failure.getPage() : page,
            failure.getHttpStatus());
      }
    }
    return interrupted;
  }

  private void publish(PlayerIdentityLeagueDiscovery scope, List<WhoScoredPlayerIdentity> rows) {
    this.persist(
        () -> {
          Map<String, WhoScoredIdentityCandidate> byId = new HashMap<>();
          for (WhoScoredIdentityCandidate candidate :
              this.candidates.findByRunIdOrderById(scope.getRunId())) {
            byId.put(candidate.getExternalId(), candidate);
          }
          for (WhoScoredPlayerIdentity row : rows) {
            WhoScoredIdentityCandidate candidate =
                byId.computeIfAbsent(
                    row.externalId(), id -> new WhoScoredIdentityCandidate(scope.getRunId(), id));
            candidate.observe(
                new WhoScoredIdentityContext(
                    row.name(),
                    row.teamId(),
                    row.teamName(),
                    row.tournamentId(),
                    row.tournamentName(),
                    scope.getLeagueId(),
                    scope.getLeagueName(),
                    row.seasonId(),
                    row.seasonName()));
          }
          this.candidates.saveAll(byId.values());
          this.leagues.findById(scope.getId()).orElseThrow().complete(Instant.now());
        });
  }

  private void fail(
      PlayerIdentityLeagueDiscovery scope, String code, Integer page, Integer httpStatus) {
    this.persist(
        () ->
            this.leagues
                .findById(scope.getId())
                .orElseThrow()
                .fail(
                    code,
                    "El descubrimiento del ámbito no se completó",
                    page,
                    httpStatus,
                    Instant.now()));
  }

  private void persist(Runnable action) {
    try {
      this.transaction.executeWithoutResult(status -> action.run());
    } catch (RuntimeException failure) {
      throw new PlayerIdentitySynchronizationException();
    }
  }
}
