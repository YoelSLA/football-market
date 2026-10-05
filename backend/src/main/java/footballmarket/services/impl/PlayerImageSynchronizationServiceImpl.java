package footballmarket.services.impl;

import footballmarket.config.TheSportsDbProperties;
import footballmarket.integrations.TheSportsDbIntegration;
import footballmarket.integrations.TheSportsDbIntegration.PlayerData;
import footballmarket.integrations.exceptions.InvalidTheSportsDbResponseException;
import footballmarket.integrations.exceptions.TheSportsDbRateLimitException;
import footballmarket.integrations.exceptions.TheSportsDbUnavailableException;
import footballmarket.models.Player;
import footballmarket.models.PlayerExternalReference;
import footballmarket.models.PlayerIdentityMatcher;
import footballmarket.models.PlayerIdentityMatcher.Candidate;
import footballmarket.models.PlayerImageResolution;
import footballmarket.models.PlayerImageSyncRun;
import footballmarket.models.PlayerImageSyncRunItem;
import footballmarket.models.enums.PlayerImageResolutionStatus;
import footballmarket.models.enums.PlayerImageSkipReason;
import footballmarket.models.enums.PlayerImageSyncRunItemResult;
import footballmarket.models.enums.PlayerProvider;
import footballmarket.models.records.PlayerIdentityAliases;
import footballmarket.models.records.PlayerImageSyncSummary;
import footballmarket.repositories.PlayerExternalReferenceRepository;
import footballmarket.repositories.PlayerImageResolutionRepository;
import footballmarket.repositories.PlayerImageSyncRunItemRepository;
import footballmarket.repositories.PlayerImageSyncRunRepository;
import footballmarket.repositories.PlayerRepository;
import footballmarket.services.PlayerImageSynchronizationService;
import footballmarket.services.exceptions.PlayerImageSyncInProgressException;
import footballmarket.services.exceptions.PlayerImageSynchronizationException;
import java.time.Clock;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import org.postgresql.util.PSQLException;
import org.postgresql.util.ServerErrorMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

/** Ejecuta un solo run por instancia, sin transacciones abiertas durante red ni esperas. */
@Service
public class PlayerImageSynchronizationServiceImpl implements PlayerImageSynchronizationService {

  private static final Logger LOG =
      LoggerFactory.getLogger(PlayerImageSynchronizationServiceImpl.class);

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

  private final AtomicBoolean executing = new AtomicBoolean();
  private final Clock clock = Clock.systemUTC();

  public PlayerImageSynchronizationServiceImpl(
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

  @Override
  public PlayerImageSyncSummary synchronize(boolean force) {

    LOG.info("Solicitud de sincronización de imágenes recibida: force={}", force);

    if (!this.executing.compareAndSet(false, true)) {
      LOG.warn("Se rechazó sincronización porque ya existe otra ejecución activa");
      throw new PlayerImageSyncInProgressException();
    }

    Long runId = null;

    try {

      runId =
          this.transaction.execute(
              status ->
                  this.runs
                      .saveAndFlush(new PlayerImageSyncRun(force, this.clock.instant()))
                      .getId());

      LOG.info("Inicia sincronización manual de imágenes: run={}, force={}", runId, force);

      long cursor = 0;

      while (true) {

        long fromId = cursor;

        Long playerId =
            this.transaction.execute(
                status ->
                    this.players
                        .findFirstByIdGreaterThanOrderByIdAsc(fromId)
                        .map(Player::getId)
                        .orElse(null));

        if (playerId == null) {
          LOG.info("No quedan jugadores por evaluar: run={}", runId);
          break;
        }

        cursor = playerId;

        LOG.info("Evaluando jugador: run={}, playerId={}", runId, playerId);

        Long currentRunId = runId;

        Evaluation evaluation =
            this.transaction.execute(status -> this.evaluate(currentRunId, playerId, force));

        if (evaluation == null) {
          LOG.info(
              "Jugador no requiere procesamiento remoto: run={}, playerId={}", runId, playerId);
          continue;
        }

        LOG.info(
            "Jugador elegible para resolución: run={}, playerId={}, nombre={}, externalId={}, modo={}",
            runId,
            evaluation.playerId(),
            evaluation.name(),
            evaluation.externalId(),
            evaluation.externalId() == null ? "SEARCH" : "LOOKUP");

        this.process(currentRunId, evaluation);
      }

      Long completedId = runId;

      PlayerImageSyncSummary summary =
          this.transaction.execute(
              status -> {
                PlayerImageSyncRun run = this.runs.findById(completedId).orElseThrow();

                run.complete(this.clock.instant());

                return run.summary();
              });

      LOG.info(
          "Concluye sincronización de imágenes: run={}, estado={}, contadores={}",
          runId,
          summary.status(),
          summary.counters());

      return summary;

    } catch (RuntimeException exception) {

      LOG.error(
          "Fallo global de sincronización de imágenes: run={}, tipo={}, mensaje={}",
          runId,
          exception.getClass().getSimpleName(),
          exception.getMessage(),
          exception);

      if (runId != null) {

        Long failedId = runId;

        try {

          this.transaction.executeWithoutResult(
              status ->
                  this.runs
                      .findById(failedId)
                      .ifPresent(run -> run.fail(this.clock.instant(), "GLOBAL_FAILURE")));

        } catch (RuntimeException persistenceFailure) {

          LOG.error(
              "No se pudo cerrar el run fallido: run={}, tipo={}, mensaje={}",
              runId,
              persistenceFailure.getClass().getSimpleName(),
              persistenceFailure.getMessage(),
              persistenceFailure);
        }
      }

      throw new PlayerImageSynchronizationException();

    } finally {

      this.executing.set(false);

      LOG.info("Liberado lock de sincronización de imágenes: run={}", runId);
    }
  }

  private Evaluation evaluate(Long runId, Long playerId, boolean force) {

    Player player = this.players.findById(playerId).orElseThrow();

    LOG.info(
        "Datos jugador: id={}, nombre={}, activo={}, fechaNacimiento={}, nacionalidad={}",
        player.getId(),
        player.getName(),
        player.isActive(),
        player.getDateOfBirth(),
        player.getNationality());

    if (!player.isActive()) {

      LOG.info("Jugador inactivo, se omite: playerId={}", playerId);

      return null;
    }

    PlayerImageResolution resolution =
        this.resolutions
            .findById(playerId)
            .orElseGet(
                () -> {
                  LOG.info("No existe resolución previa; se crea una nueva: playerId={}", playerId);

                  return this.resolutions.save(new PlayerImageResolution(player));
                });

    LOG.info(
        "Resolución actual: playerId={}, estado={}, lastAttemptAt={}",
        playerId,
        resolution.getStatus(),
        resolution.getLastAttemptAt());

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

      LOG.info(
          "Jugador omitido: jugador={}, motivo={}, estado={}",
          playerId,
          reason,
          resolution.getStatus());

      return null;
    }

    Optional<PlayerExternalReference> reference =
        this.references.findByPlayerIdAndProvider(playerId, PlayerProvider.THE_SPORTS_DB);

    LOG.info(
        "Referencia TheSportsDB: playerId={}, existe={}, externalId={}",
        playerId,
        reference.isPresent(),
        reference.map(PlayerExternalReference::getExternalId).orElse(null));

    return new Evaluation(
        playerId,
        item.getId(),
        player.getName(),
        reference.map(PlayerExternalReference::getExternalId).orElse(null));
  }

  private PlayerImageSkipReason skip(PlayerImageResolution resolution, boolean force) {

    if (force) {
      LOG.debug("Force activo: no se aplican reglas de skip. estado={}", resolution.getStatus());
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

  private void process(Long runId, Evaluation evaluation) {

    LOG.info(
        "Inicio procesamiento remoto: run={}, playerId={}, nombre={}, externalId={}, modo={}",
        runId,
        evaluation.playerId(),
        evaluation.name(),
        evaluation.externalId(),
        evaluation.externalId() == null ? "SEARCH" : "LOOKUP");

    List<PlayerData> players = null;

    boolean transientFailure = false;
    boolean permanentFailure = false;

    for (int attempt = 0; attempt <= this.properties.maxRetries(); attempt++) {

      try {

        LOG.info(
            "Consulta TheSportsDB: playerId={}, intento={}/{}, modo={}",
            evaluation.playerId(),
            attempt + 1,
            this.properties.maxRetries() + 1,
            evaluation.externalId() == null ? "SEARCH" : "LOOKUP");

        players =
            evaluation.externalId() == null
                ? this.integration.search(evaluation.name())
                : this.integration.lookup(evaluation.externalId());

        LOG.info(
            "Respuesta TheSportsDB recibida: playerId={}, candidatos={}",
            evaluation.playerId(),
            players != null ? players.size() : null);

        if (players == null) {

          LOG.warn("TheSportsDB devolvió null: playerId={}", evaluation.playerId());

          permanentFailure = true;
        }

        break;

      } catch (TheSportsDbRateLimitException | TheSportsDbUnavailableException exception) {

        transientFailure = true;

        LOG.warn(
            "Error transitorio TheSportsDB: playerId={}, intento={}, tipo={}, mensaje={}",
            evaluation.playerId(),
            attempt + 1,
            exception.getClass().getSimpleName(),
            exception.getMessage());

      } catch (InvalidTheSportsDbResponseException exception) {

        permanentFailure = true;

        LOG.warn(
            "Respuesta inválida TheSportsDB: playerId={}, tipo={}, mensaje={}",
            evaluation.playerId(),
            exception.getClass().getSimpleName(),
            exception.getMessage(),
            exception);

        break;

      } finally {

        this.transaction.executeWithoutResult(
            status ->
                this.resolutions
                    .findById(evaluation.playerId())
                    .orElseThrow()
                    .markAttempt(this.clock.instant()));

        LOG.debug("Intento registrado en resolución: playerId={}", evaluation.playerId());
      }
    }

    if (players == null) {

      PlayerImageResolutionStatus result =
          permanentFailure
              ? PlayerImageResolutionStatus.FAILED
              : PlayerImageResolutionStatus.RETRYABLE_ERROR;

      LOG.warn(
          "No hay respuesta utilizable del proveedor: playerId={}, resultado={}, transientFailure={}, permanentFailure={}",
          evaluation.playerId(),
          result,
          transientFailure,
          permanentFailure);

      if (transientFailure || permanentFailure) {

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
    }

    try {

      List<PlayerData> response = players;

      this.transaction.executeWithoutResult(status -> this.apply(runId, evaluation, response));

    } catch (DataIntegrityViolationException exception) {

      LOG.warn(
          "Violación de integridad durante resolución: playerId={}, tipo={}, mensaje={}",
          evaluation.playerId(),
          exception.getClass().getSimpleName(),
          exception.getMessage());

      if (!this.isReferenceConflict(exception)) {
        throw exception;
      }

      LOG.warn("Conflicto de identidad externa detectado: playerId={}", evaluation.playerId());

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

    LOG.info(
        "Aplicando respuesta TheSportsDB: playerId={}, candidatos={}",
        evaluation.playerId(),
        response != null ? response.size() : null);

    for (PlayerData candidate : response) {

      LOG.info(
          "Candidato TSD: playerId={}, externalId={}, nombre={}, alternativo={}, equipo={}, sport={}, nacimiento={}, nacionalidad={}, cutout={}, thumbnail={}",
          evaluation.playerId(),
          candidate.externalId(),
          candidate.name(),
          candidate.alternateName(),
          candidate.team(),
          candidate.sport(),
          candidate.dateOfBirth(),
          candidate.nationality(),
          candidate.cutout(),
          candidate.thumbnail());
    }

    PlayerData match = null;

    if (evaluation.externalId() == null) {

      List<PlayerData> matches =
          response.stream()
              .filter(
                  candidate -> {
                    boolean accepted = this.matcher.matches(player, this.identity(candidate));

                    LOG.info(
                        "Resultado matching candidato: playerId={}, candidateExternalId={}, candidateName={}, accepted={}",
                        evaluation.playerId(),
                        candidate.externalId(),
                        candidate.name(),
                        accepted);

                    return accepted;
                  })
              .toList();

      LOG.info(
          "Resultado matching global: playerId={}, candidatos={}, matches={}",
          evaluation.playerId(),
          response.size(),
          matches.size());

      if (matches.size() == 1) {

        match = matches.getFirst();

        LOG.info(
            "Match único aceptado: playerId={}, externalId={}, nombre={}",
            evaluation.playerId(),
            match.externalId(),
            match.name());

      } else if (matches.isEmpty()) {

        LOG.info("No hubo candidatos válidos: playerId={}", evaluation.playerId());

      } else {

        LOG.warn(
            "Matching ambiguo: playerId={}, matches={}", evaluation.playerId(), matches.size());
      }

    } else if (response.size() == 1) {

      PlayerData candidate = response.getFirst();

      boolean sameExternalId = evaluation.externalId().equals(candidate.externalId());

      boolean identityMatches = this.matcher.matches(player, this.identity(candidate));

      LOG.info(
          "Validación lookup: playerId={}, expectedExternalId={}, returnedExternalId={}, sameExternalId={}, identityMatches={}",
          evaluation.playerId(),
          evaluation.externalId(),
          candidate.externalId(),
          sameExternalId,
          identityMatches);

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

      LOG.warn(
          "Lookup devolvió múltiples candidatos: playerId={}, cantidad={}",
          evaluation.playerId(),
          response.size());

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

      LOG.info("No se encontró identidad confiable: playerId={}", evaluation.playerId());

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
              PlayerProvider.THE_SPORTS_DB, match.externalId());

      if (owner.isPresent() && !owner.get().getPlayer().getId().equals(player.getId())) {

        LOG.warn(
            "ExternalId ya pertenece a otro jugador: playerId={}, externalId={}, ownerPlayerId={}",
            player.getId(),
            match.externalId(),
            owner.get().getPlayer().getId());

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

      LOG.info(
          "Creando referencia externa TheSportsDB: playerId={}, externalId={}",
          player.getId(),
          match.externalId());

      player.addExternalReference(PlayerProvider.THE_SPORTS_DB, match.externalId());
    }

    String previousImage = player.getImageUrl();

    String previousFallback = player.getFallbackImageUrl();

    LOG.info(
        "Imágenes antes de aplicar: playerId={}, imageUrl={}, fallbackImageUrl={}",
        player.getId(),
        previousImage,
        previousFallback);

    LOG.info(
        "Imágenes recibidas: playerId={}, cutout={}, thumbnail={}",
        player.getId(),
        match.cutout(),
        match.thumbnail());

    player.applyImages(match.cutout(), match.thumbnail());

    this.players.saveAndFlush(player);

    boolean updated =
        !java.util.Objects.equals(previousImage, player.getImageUrl())
            || !java.util.Objects.equals(previousFallback, player.getFallbackImageUrl());

    LOG.info(
        "Imágenes después de aplicar: playerId={}, imageUrl={}, fallbackImageUrl={}, updated={}",
        player.getId(),
        player.getImageUrl(),
        player.getFallbackImageUrl(),
        updated);

    PlayerImageResolutionStatus outcome =
        player.getImageUrl() != null
                && (evaluation.externalId() != null
                    || match.cutout() != null
                    || match.thumbnail() != null)
            ? PlayerImageResolutionStatus.FOUND
            : PlayerImageResolutionStatus.NOT_FOUND;

    LOG.info(
        "Resultado funcional del jugador: playerId={}, outcome={}, newIdentity={}, imageUpdated={}",
        player.getId(),
        outcome,
        newIdentity,
        updated);

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

    LOG.debug(
        "Finalizando evaluación fuera de transacción actual: run={}, playerId={}, result={}, detail={}",
        runId,
        evaluation.playerId(),
        result,
        detail);

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

    PlayerImageResolutionStatus previousStatus = resolution.getStatus();

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

    LOG.info(
        "Jugador finalizado: run={}, playerId={}, previousStatus={}, finalStatus={}, itemResult={}, identityResolved={}, imageUpdated={}, conflict={}, attempted={}, detail={}",
        runId,
        evaluation.playerId(),
        previousStatus,
        result,
        itemResult,
        identityResolved,
        imageUpdated,
        conflict,
        attempted,
        detail);

    if (conflict || result == PlayerImageResolutionStatus.FAILED) {

      LOG.warn(
          "Jugador con resolución fallida: run={}, jugador={}, resultado={}, conflicto={}, detalle={}",
          runId,
          evaluation.playerId(),
          result,
          conflict,
          detail);
    }
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
