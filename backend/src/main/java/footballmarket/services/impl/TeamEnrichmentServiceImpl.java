package footballmarket.services.impl;

import footballmarket.integrations.TheSportsDbIntegration;
import footballmarket.integrations.exceptions.InvalidTheSportsDbResponseException;
import footballmarket.integrations.exceptions.TheSportsDbRateLimitException;
import footballmarket.integrations.exceptions.TheSportsDbServerException;
import footballmarket.integrations.exceptions.TheSportsDbUnavailableException;
import footballmarket.models.PendingReviewCase;
import footballmarket.models.Team;
import footballmarket.models.TeamIdentityMatcher;
import footballmarket.models.TeamNameNormalizer;
import footballmarket.models.TeamResolutionAttempt;
import footballmarket.models.enums.ExternalProvider;
import footballmarket.models.enums.ReviewCategory;
import footballmarket.models.enums.ReviewCause;
import footballmarket.models.enums.ReviewSubjectType;
import footballmarket.models.enums.TeamResolutionFailure;
import footballmarket.models.enums.TeamResolutionResult;
import footballmarket.models.exceptions.InvalidCatalogEntityException;
import footballmarket.repositories.PendingReviewCaseRepository;
import footballmarket.repositories.TeamExternalReferenceRepository;
import footballmarket.repositories.TeamRepository;
import footballmarket.repositories.TeamResolutionAttemptRepository;
import footballmarket.services.TeamEnrichmentService;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.postgresql.util.PSQLException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

/** Red fuera de escritura y confirmación independiente, revalidada, por equipo. */
@Service
public class TeamEnrichmentServiceImpl implements TeamEnrichmentService {
  private static final Logger LOG = LoggerFactory.getLogger(TeamEnrichmentServiceImpl.class);
  private final TeamRepository teams;
  private final TeamExternalReferenceRepository references;
  private final TeamResolutionAttemptRepository attempts;
  private final PendingReviewCaseRepository cases;
  private final TheSportsDbIntegration integration;
  private final TransactionTemplate read;
  private final TransactionTemplate write;

  public TeamEnrichmentServiceImpl(
      TeamRepository teams,
      TeamExternalReferenceRepository references,
      TeamResolutionAttemptRepository attempts,
      PendingReviewCaseRepository cases,
      TheSportsDbIntegration integration,
      PlatformTransactionManager transactionManager) {
    this.teams = teams;
    this.references = references;
    this.attempts = attempts;
    this.cases = cases;
    this.integration = integration;
    this.read = new TransactionTemplate(transactionManager);
    this.read.setReadOnly(true);
    this.read.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    this.write = new TransactionTemplate(transactionManager);
    this.write.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
  }

  private record Target(Long id, String name) {}

  @Override
  public void enrichCurrentTeams() {
    List<Target> targets =
        this.read.execute(
            status ->
                this.teams.findByCurrentTrueOrderByIdAsc().stream()
                    .filter(team -> this.eligible(team, Instant.now()))
                    .map(team -> new Target(team.getId(), team.getName()))
                    .toList());
    for (Target target : targets) {
      try {
        this.enrich(target);
      } catch (RuntimeException exception) {
        LOG.warn("No se confirmó el enriquecimiento independiente del equipo {}", target.id());
      }
    }
  }

  private boolean eligible(Team team, Instant now) {
    return team.isCurrent()
        && team.getExternalReferences().stream()
            .noneMatch(reference -> reference.getProvider() == ExternalProvider.THE_SPORTS_DB)
        && this.attempts
            .findById(team.getId())
            .map(attempt -> attempt.eligible(team.getName(), now))
            .orElse(true);
  }

  private void enrich(Target target) {
    Instant calledAt = Instant.now();
    TeamIdentityMatcher.Evaluation evaluation = null;
    TeamResolutionFailure failure = null;
    Instant retryAt = null;
    try {
      TheSportsDbIntegration.TeamSearch search = this.integration.searchTeams(target.name());
      List<TeamIdentityMatcher.Candidate> candidates =
          search.candidates().stream()
              .map(
                  candidate ->
                      new TeamIdentityMatcher.Candidate(
                          candidate.externalId(), candidate.name(), candidate.sport()))
              .toList();
      evaluation = TeamIdentityMatcher.evaluate(target.name(), candidates, search.complete());
    } catch (TheSportsDbRateLimitException exception) {
      failure = TeamResolutionFailure.RATE_LIMITED;
      retryAt = exception.getRetryAt();
    } catch (TheSportsDbServerException exception) {
      failure = TeamResolutionFailure.SERVER_ERROR;
    } catch (TheSportsDbUnavailableException exception) {
      failure = TeamResolutionFailure.TIMEOUT_OR_TRANSPORT;
    } catch (InvalidTheSportsDbResponseException | InvalidCatalogEntityException exception) {
      failure = TeamResolutionFailure.INVALID_RESPONSE;
    }
    TeamIdentityMatcher.Evaluation result = evaluation;
    TeamResolutionFailure technicalFailure = failure;
    Instant waitUntil = retryAt;
    try {
      this.persist(target, calledAt, result, technicalFailure, waitUntil);
    } catch (RuntimeException exception) {
      // Solo la identidad externa disputada permite reaplicar el resultado ya obtenido.
      // Nunca repetir red ni continuar una transacción abortada.
      if (result == null
          || result.result() != TeamResolutionResult.MATCH
          || !isExternalIdentityConflict(exception)) {
        throw exception;
      }
      this.persist(target, calledAt, result, technicalFailure, waitUntil);
    }
  }

  /** Revalida y confirma un resultado escalar sin mantener la llamada externa en escritura. */
  private void persist(
      Target target,
      Instant calledAt,
      TeamIdentityMatcher.Evaluation result,
      TeamResolutionFailure technicalFailure,
      Instant waitUntil) {
    this.write.executeWithoutResult(
        status -> {
          Team team = this.teams.findForEnrichment(target.id()).orElse(null);
          if (team == null
              || !TeamNameNormalizer.normalize(team.getName())
                  .equals(TeamNameNormalizer.normalize(target.name()))
              || !this.eligible(team, calledAt)) {
            return;
          }
          TeamResolutionAttempt attempt =
              this.attempts
                  .findById(team.getId())
                  .orElseGet(() -> new TeamResolutionAttempt(team, calledAt, target.name()));
          if (result != null) {
            if (result.result() == TeamResolutionResult.MATCH) {
              Long owner =
                  this.references
                      .findByProviderAndExternalId(
                          ExternalProvider.THE_SPORTS_DB, result.externalId())
                      .map(reference -> reference.getTeam().getId())
                      .orElse(null);
              if (owner != null && !owner.equals(team.getId())) {
                this.recordConflict(team, owner, result.externalId(), calledAt);
              } else {
                team.addExternalReference(ExternalProvider.THE_SPORTS_DB, result.externalId());
              }
            }
            attempt.evaluated(calledAt, target.name(), result.result());
          } else {
            attempt.failed(calledAt, target.name(), technicalFailure, waitUntil);
          }
          this.attempts.saveAndFlush(attempt);
          this.teams.flush();
        });
  }

  private static boolean isExternalIdentityConflict(Throwable failure) {
    java.util.Set<Throwable> visited = new java.util.HashSet<>();
    for (Throwable cause = failure; cause != null && visited.add(cause); cause = cause.getCause()) {
      if (cause instanceof PSQLException postgres
          && "23505".equals(postgres.getSQLState())
          && postgres.getServerErrorMessage() != null
          && "team_external_references".equals(postgres.getServerErrorMessage().getTable())
          && "uk_team_external_references_provider_external_id"
              .equals(postgres.getServerErrorMessage().getConstraint())) {
        return true;
      }
    }
    return false;
  }

  private void recordConflict(Team team, Long owner, String externalId, Instant detectedAt) {
    String key =
        "EXTERNAL_IDENTITY_CONFLICT:TEAM:THE_SPORTS_DB:"
            + externalId.length()
            + ":"
            + externalId
            + ":"
            + team.getId();
    Map<String, Object> evidence =
        Map.of("currentOwnerId", owner, "intendedOwnerId", team.getId(), "externalId", externalId);
    PendingReviewCase review = this.cases.findByCaseKey(key).orElse(null);
    if (review == null) {
      this.cases.save(
          new PendingReviewCase(
              ReviewCategory.EXTERNAL_IDENTITY_CONFLICT,
              ReviewCause.EXTERNAL_IDENTITY_OWNED,
              ReviewSubjectType.TEAM,
              team.getId(),
              ExternalProvider.THE_SPORTS_DB,
              externalId,
              key,
              detectedAt,
              evidence));
    } else {
      review.observe(ReviewCause.EXTERNAL_IDENTITY_OWNED, detectedAt, evidence);
    }
  }
}
