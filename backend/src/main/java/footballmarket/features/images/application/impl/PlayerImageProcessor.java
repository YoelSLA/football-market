package footballmarket.features.images.application.impl;

import footballmarket.features.catalog.domain.enums.ExternalProvider;
import footballmarket.features.catalog.domain.models.player.Player;
import footballmarket.features.catalog.domain.models.player.PlayerExternalReference;
import footballmarket.features.catalog.infraestructure.PlayerExternalReferenceRepository;
import footballmarket.features.catalog.infraestructure.PlayerRepository;
import footballmarket.features.identity.domain.models.PlayerIdentityMatcher;
import footballmarket.features.identity.domain.models.PlayerIdentityMatcher.Candidate;
import footballmarket.features.identity.domain.records.PlayerIdentityAliases;
import footballmarket.features.images.domain.enums.PlayerImageResolutionStatus;
import footballmarket.features.images.domain.enums.PlayerImageSkipReason;
import footballmarket.features.images.domain.enums.PlayerImageSyncRunItemResult;
import footballmarket.features.images.domain.models.PlayerImageResolution;
import footballmarket.features.images.domain.models.PlayerImageSyncRun;
import footballmarket.features.images.domain.models.PlayerImageSyncRunItem;
import footballmarket.features.images.infraestructure.PlayerImageResolutionRepository;
import footballmarket.features.images.infraestructure.PlayerImageSyncRunItemRepository;
import footballmarket.features.images.infraestructure.PlayerImageSyncRunRepository;
import footballmarket.integrations.thesportsdb.TheSportsDbIntegration;
import footballmarket.integrations.thesportsdb.TheSportsDbIntegration.PlayerData;
import footballmarket.integrations.thesportsdb.config.TheSportsDbProperties;
import footballmarket.integrations.thesportsdb.exceptions.InvalidTheSportsDbResponseException;
import footballmarket.integrations.thesportsdb.exceptions.TheSportsDbRateLimitException;
import footballmarket.integrations.thesportsdb.exceptions.TheSportsDbUnavailableException;
import java.time.Clock;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.postgresql.util.PSQLException;
import org.postgresql.util.ServerErrorMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

/** Resuelve y persiste la identidad e imágenes de un único jugador de forma atómica. */
@Component
public class PlayerImageProcessor {

  private static final Logger LOG = LoggerFactory.getLogger(PlayerImageProcessor.class);
  private static final String REFERENCE_UNIQUE_CONSTRAINT =
      "uk_player_external_references_provider_external_id";

  private final PlayerRepository players;
  private final PlayerExternalReferenceRepository references;
  private final PlayerImageResolutionRepository resolutions;
  private final PlayerImageSyncRunRepository runs;
  private final PlayerImageSyncRunItemRepository items;
  private final TheSportsDbIntegration integration;
  private final TheSportsDbProperties properties;
  private final TransactionTemplate transaction;
  private final PlayerIdentityMatcher matcher =
      new PlayerIdentityMatcher(PlayerIdentityAliases.controlled());
  private final Clock clock = Clock.systemUTC();

  public PlayerImageProcessor(
      PlayerRepository players,
      PlayerExternalReferenceRepository references,
      PlayerImageResolutionRepository resolutions,
      PlayerImageSyncRunRepository runs,
      PlayerImageSyncRunItemRepository items,
      TheSportsDbIntegration integration,
      TheSportsDbProperties properties,
      PlatformTransactionManager manager) {
    this.players = players;
    this.references = references;
    this.resolutions = resolutions;
    this.runs = runs;
    this.items = items;
    this.integration = integration;
    this.properties = properties;
    this.transaction = new TransactionTemplate(manager);
    this.transaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
  }

  /** Evalúa y procesa un jugador. Retorna true si requirió llamada remota, false si se omitió. */
  public boolean processPlayer(Long runId, Long playerId, boolean force) {
    Evaluation evaluation =
        this.transaction.execute(status -> this.evaluate(runId, playerId, force));
    if (evaluation == null) {
      return false;
    }
    this.processRemote(runId, evaluation);
    return true;
  }

  private Evaluation evaluate(Long runId, Long playerId, boolean force) {
    Player player = this.players.findById(playerId).orElseThrow();
    if (!player.isActive() || player.getTeam() == null) {
      return null;
    }

    PlayerImageResolution resolution =
        this.resolutions
            .findById(playerId)
            .orElseGet(() -> this.resolutions.save(new PlayerImageResolution(player)));

    PlayerImageSyncRun run = this.runs.findById(runId).orElseThrow();
    PlayerImageSyncRunItem item =
        this.items.save(new PlayerImageSyncRunItem(run, player, resolution.getStatus()));
    run.evaluate();

    PlayerImageSkipReason reason = this.skip(resolution, force);
    if (reason != null) {
      item.finish(
          resolution.getStatus(),
          PlayerImageSyncRunItemResult.SKIPPED,
          reason,
          false,
          false,
          false,
          false,
          "Omitido por elegibilidad",
          this.clock.instant());
      run.result(PlayerImageSyncRunItemResult.SKIPPED, reason, false, false);
      return null;
    }

    Optional<PlayerExternalReference> reference =
        this.references.findByPlayerIdAndProvider(playerId, ExternalProvider.THE_SPORTS_DB);

    return new Evaluation(
        playerId,
        item.getId(),
        player.getName(),
        reference.map(PlayerExternalReference::getExternalId).orElse(null));
  }

  private PlayerImageSkipReason skip(PlayerImageResolution resolution, boolean force) {
    if (force) {
      return null;
    }
    return switch (resolution.getStatus()) {
      case FOUND -> PlayerImageSkipReason.FOUND;
      case FAILED -> PlayerImageSkipReason.FAILED;
      case NOT_FOUND ->
          resolution.getLastAttemptAt() != null
                  && resolution
                      .getLastAttemptAt()
                      .plusSeconds(86400L * this.properties.imageRetryDays())
                      .isAfter(this.clock.instant())
              ? PlayerImageSkipReason.RETRY_WINDOW
              : null;
      default -> null;
    };
  }

  private void processRemote(Long runId, Evaluation evaluation) {
    List<PlayerData> candidates = null;
    boolean transientFailure = false;
    boolean permanentFailure = false;

    for (int attempt = 0; attempt <= this.properties.maxRetries(); attempt++) {
      try {
        candidates =
            evaluation.externalId() == null
                ? this.integration.search(evaluation.name())
                : this.integration.lookup(evaluation.externalId());

        if (candidates == null) {
          permanentFailure = true;
        }
        break;
      } catch (TheSportsDbRateLimitException | TheSportsDbUnavailableException exception) {
        transientFailure = true;
        LOG.warn(
            "Fallo transitorio TheSportsDB: playerId={}, intento={}/{}",
            evaluation.playerId(),
            attempt + 1,
            this.properties.maxRetries() + 1);
      } catch (InvalidTheSportsDbResponseException exception) {
        permanentFailure = true;
        LOG.warn("Respuesta inválida TheSportsDB: playerId={}", evaluation.playerId());
        break;
      } finally {
        this.transaction.executeWithoutResult(
            status ->
                this.resolutions
                    .findById(evaluation.playerId())
                    .orElseThrow()
                    .markAttempt(this.clock.instant()));
      }
    }

    if (candidates == null) {
      PlayerImageResolutionStatus result =
          permanentFailure
              ? PlayerImageResolutionStatus.FAILED
              : PlayerImageResolutionStatus.RETRYABLE_ERROR;

      this.finish(
          runId,
          evaluation,
          result,
          false,
          false,
          false,
          "El proveedor no devolvió datos utilizables",
          true);
      return;
    }

    try {
      List<PlayerData> response = candidates;
      this.transaction.executeWithoutResult(status -> this.apply(runId, evaluation, response));
    } catch (DataIntegrityViolationException exception) {
      if (!this.isReferenceConflict(exception)) {
        throw exception;
      }
      this.finish(
          runId,
          evaluation,
          PlayerImageResolutionStatus.FAILED,
          false,
          false,
          true,
          "Identidad externa ya vinculada a otro jugador",
          true);
    }
  }

  private void apply(Long runId, Evaluation evaluation, List<PlayerData> response) {
    Player player = this.players.findById(evaluation.playerId()).orElseThrow();
    PlayerData match = null;

    if (evaluation.externalId() == null) {
      List<PlayerData> matches =
          response.stream()
              .filter(candidate -> this.matcher.matches(player, this.identity(candidate)))
              .toList();

      if (matches.size() == 1) {
        match = matches.getFirst();
      }
    } else if (response.size() == 1) {
      PlayerData candidate = response.getFirst();
      boolean sameExternalId = evaluation.externalId().equals(candidate.externalId());
      boolean identityMatches = this.matcher.matches(player, this.identity(candidate));

      if (!sameExternalId || !identityMatches) {
        this.finishInTransaction(
            runId,
            evaluation,
            PlayerImageResolutionStatus.FAILED,
            false,
            false,
            false,
            "Identidad incompatible con el jugador",
            true);
        return;
      }
      match = candidate;
    } else if (response.size() > 1) {
      this.finishInTransaction(
          runId,
          evaluation,
          PlayerImageResolutionStatus.FAILED,
          false,
          false,
          false,
          "Consulta de identidad inválida",
          true);
      return;
    }

    if (match == null) {
      this.finishInTransaction(
          runId,
          evaluation,
          PlayerImageResolutionStatus.NOT_FOUND,
          false,
          false,
          false,
          "Sin imagen o identidad confiable",
          true);
      return;
    }

    boolean newIdentity = evaluation.externalId() == null;
    if (newIdentity) {
      Optional<PlayerExternalReference> owner =
          this.references.findByProviderAndExternalId(
              ExternalProvider.THE_SPORTS_DB, match.externalId());

      if (owner.isPresent() && !owner.get().getPlayer().getId().equals(player.getId())) {
        this.finishInTransaction(
            runId,
            evaluation,
            PlayerImageResolutionStatus.FAILED,
            false,
            false,
            true,
            "Identidad externa ya vinculada a otro jugador",
            true);
        return;
      }
      player.addExternalReference(ExternalProvider.THE_SPORTS_DB, match.externalId());
    }

    String previousImage = player.getImageUrl();
    String previousFallback = player.getFallbackImageUrl();

    player.applyImages(match.cutout(), match.thumbnail());
    this.players.saveAndFlush(player);

    boolean updated =
        !Objects.equals(previousImage, player.getImageUrl())
            || !Objects.equals(previousFallback, player.getFallbackImageUrl());

    PlayerImageResolutionStatus outcome =
        player.getImageUrl() != null
                && (evaluation.externalId() != null
                    || match.cutout() != null
                    || match.thumbnail() != null)
            ? PlayerImageResolutionStatus.FOUND
            : PlayerImageResolutionStatus.NOT_FOUND;

    this.finishInTransaction(
        runId,
        evaluation,
        outcome,
        newIdentity,
        updated,
        false,
        updated ? "Imagen asociada" : "Sin sustitución de imagen",
        true);
  }

  private Candidate identity(PlayerData data) {
    return new Candidate(
        data.name(),
        data.alternateName(),
        data.team(),
        data.sport(),
        data.dateOfBirth(),
        data.nationality());
  }

  private void finish(
      Long runId,
      Evaluation evaluation,
      PlayerImageResolutionStatus result,
      boolean identityResolved,
      boolean imageUpdated,
      boolean conflict,
      String detail,
      boolean attempted) {
    this.transaction.executeWithoutResult(
        status ->
            this.finishInTransaction(
                runId,
                evaluation,
                result,
                identityResolved,
                imageUpdated,
                conflict,
                detail,
                attempted));
  }

  private void finishInTransaction(
      Long runId,
      Evaluation evaluation,
      PlayerImageResolutionStatus result,
      boolean identityResolved,
      boolean imageUpdated,
      boolean conflict,
      String detail,
      boolean attempted) {
    PlayerImageResolution resolution =
        this.resolutions.findById(evaluation.playerId()).orElseThrow();
    PlayerImageSyncRunItem item = this.items.findById(evaluation.itemId()).orElseThrow();
    PlayerImageSyncRun run = this.runs.findById(runId).orElseThrow();

    resolution.recordAttempt(result, this.clock.instant());
    PlayerImageSyncRunItemResult itemResult = PlayerImageSyncRunItemResult.valueOf(result.name());

    item.finish(
        result,
        itemResult,
        null,
        identityResolved,
        imageUpdated,
        conflict,
        result == PlayerImageResolutionStatus.FAILED
            || result == PlayerImageResolutionStatus.RETRYABLE_ERROR,
        detail,
        this.clock.instant());

    run.result(itemResult, null, attempted, conflict);
  }

  private boolean isReferenceConflict(Throwable failure) {
    Throwable cause = failure;
    while (cause != null) {
      if (cause instanceof PSQLException postgres) {
        ServerErrorMessage error = postgres.getServerErrorMessage();
        if ("23505".equals(postgres.getSQLState())
            && error != null
            && "player_external_references".equals(error.getTable())
            && REFERENCE_UNIQUE_CONSTRAINT.equals(error.getConstraint())) {
          return true;
        }
      }
      cause = cause.getCause();
    }
    return false;
  }

  private record Evaluation(Long playerId, Long itemId, String name, String externalId) {}
}
