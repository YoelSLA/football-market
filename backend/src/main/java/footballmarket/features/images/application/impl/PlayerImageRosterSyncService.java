package footballmarket.features.images.application.impl;

import footballmarket.features.catalog.domain.enums.ExternalProvider;
import footballmarket.features.catalog.domain.models.player.Player;
import footballmarket.features.catalog.domain.models.player.PlayerExternalReference;
import footballmarket.features.catalog.domain.models.team.Team;
import footballmarket.features.catalog.domain.models.team.TeamExternalReference;
import footballmarket.features.catalog.infraestructure.PlayerExternalReferenceRepository;
import footballmarket.features.catalog.infraestructure.PlayerRepository;
import footballmarket.features.catalog.infraestructure.TeamRepository;
import footballmarket.features.identity.domain.models.PlayerIdentityMatcher;
import footballmarket.features.identity.domain.models.PlayerIdentityMatcher.Candidate;
import footballmarket.features.identity.domain.records.PlayerIdentityAliases;
import footballmarket.features.images.domain.enums.PlayerImageResolutionStatus;
import footballmarket.features.images.domain.enums.PlayerImageSyncRunItemResult;
import footballmarket.features.images.domain.models.PlayerImageResolution;
import footballmarket.features.images.domain.models.PlayerImageSyncRun;
import footballmarket.features.images.domain.models.PlayerImageSyncRunItem;
import footballmarket.features.images.infraestructure.PlayerImageResolutionRepository;
import footballmarket.features.images.infraestructure.PlayerImageSyncRunItemRepository;
import footballmarket.features.images.infraestructure.PlayerImageSyncRunRepository;
import footballmarket.integrations.thesportsdb.TheSportsDbIntegration;
import footballmarket.integrations.thesportsdb.TheSportsDbIntegration.PlayerData;
import footballmarket.integrations.thesportsdb.TheSportsDbIntegration.TeamData;
import footballmarket.integrations.thesportsdb.TheSportsDbIntegration.TeamSearch;
import footballmarket.integrations.thesportsdb.exceptions.TheSportsDbRateLimitException;
import footballmarket.integrations.thesportsdb.exceptions.TheSportsDbUnavailableException;
import java.time.Clock;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

/** Sincroniza imágenes de jugadores a partir del plantel completo del equipo en TheSportsDB. */
@Service
public class PlayerImageRosterSyncService {

  private static final Logger LOG = LoggerFactory.getLogger(PlayerImageRosterSyncService.class);

  private final TeamRepository teams;
  private final PlayerRepository players;
  private final PlayerExternalReferenceRepository references;
  private final PlayerImageResolutionRepository resolutions;
  private final PlayerImageSyncRunRepository runs;
  private final PlayerImageSyncRunItemRepository items;
  private final TheSportsDbIntegration integration;
  private final TransactionTemplate transaction;
  private final PlayerIdentityMatcher matcher =
      new PlayerIdentityMatcher(PlayerIdentityAliases.controlled());
  private final Clock clock = Clock.systemUTC();

  public PlayerImageRosterSyncService(
      TeamRepository teams,
      PlayerRepository players,
      PlayerExternalReferenceRepository references,
      PlayerImageResolutionRepository resolutions,
      PlayerImageSyncRunRepository runs,
      PlayerImageSyncRunItemRepository items,
      TheSportsDbIntegration integration,
      PlatformTransactionManager manager) {
    this.teams = teams;
    this.players = players;
    this.references = references;
    this.resolutions = resolutions;
    this.runs = runs;
    this.items = items;
    this.integration = integration;
    this.transaction = new TransactionTemplate(manager);
    this.transaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
  }

  /**
   * Sincroniza los jugadores de un equipo usando su plantel completo en TheSportsDB.
   *
   * @param runId identificador del run activo
   * @param teamId identificador del equipo
   * @param force si true, sobreescribe fotos existentes
   * @return cantidad de jugadores enriquecidos exitosamente
   */
  public int syncTeamRoster(Long runId, Long teamId, boolean force) {
    Team team =
        this.transaction.execute(
            status ->
                this.teams.findWithReferencesById(teamId).filter(Team::isCurrent).orElse(null));

    if (team == null) {
      LOG.info("Equipo {} no encontrado o no vigente, se omite", teamId);
      return 0;
    }

    String teamExternalId = this.resolveTeamExternalId(team);
    if (teamExternalId == null) {
      LOG.warn("No se pudo resolver la identidad de TheSportsDB para el equipo {}", team.getName());
      return 0;
    }

    List<PlayerData> roster;
    try {
      LOG.info(
          "Consultando plantel en TheSportsDB para equipo {} (id={})",
          team.getName(),
          teamExternalId);
      roster = this.integration.lookupPlayersByTeam(teamExternalId);
    } catch (TheSportsDbRateLimitException | TheSportsDbUnavailableException exception) {
      LOG.warn(
          "Fallo temporal al consultar plantel de {}: {}", team.getName(), exception.getMessage());
      return 0;
    } catch (RuntimeException exception) {
      LOG.warn("Error al consultar plantel de {}: {}", team.getName(), exception.getMessage());
      return 0;
    }

    if (roster == null || roster.isEmpty()) {
      LOG.info("El plantel de {} en TheSportsDB está vacío", team.getName());
      return 0;
    }

    return this.transaction.execute(
        status -> this.applyRosterToPlayers(runId, teamId, roster, force));
  }

  private String resolveTeamExternalId(Team team) {
    Optional<String> existingId =
        this.transaction.execute(
            status -> {
              Team currentTeam = this.teams.findWithReferencesById(team.getId()).orElse(null);
              if (currentTeam == null) {
                return Optional.empty();
              }
              return currentTeam.getExternalReferences().stream()
                  .filter(r -> r.getProvider() == ExternalProvider.THE_SPORTS_DB)
                  .map(TeamExternalReference::getExternalId)
                  .findFirst();
            });

    if (existingId != null && existingId.isPresent()) {
      return existingId.get();
    }

    String cleanName = this.cleanTeamName(team.getName());
    try {
      TeamSearch search = this.integration.searchTeams(cleanName);
      if (search == null || search.candidates().isEmpty()) {
        search = this.integration.searchTeams(team.getName());
      }
      if (search != null && !search.candidates().isEmpty()) {
        List<TeamData> soccerTeams =
            search.candidates().stream().filter(c -> "Soccer".equalsIgnoreCase(c.sport())).toList();

        TeamData match =
            !soccerTeams.isEmpty() ? soccerTeams.getFirst() : search.candidates().getFirst();
        String externalId = match.externalId();
        this.transaction.executeWithoutResult(
            status -> {
              Team managed = this.teams.findWithReferencesById(team.getId()).orElseThrow();
              managed.addExternalReference(ExternalProvider.THE_SPORTS_DB, externalId);
              this.teams.saveAndFlush(managed);
            });
        LOG.info("Equipo {} vinculado con TheSportsDB id={}", team.getName(), externalId);
        return externalId;
      }
    } catch (RuntimeException exception) {
      LOG.warn(
          "Error resolviendo identidad externa del equipo {}: {}",
          team.getName(),
          exception.getMessage());
    }
    return null;
  }

  private int applyRosterToPlayers(
      Long runId, Long teamId, List<PlayerData> roster, boolean force) {
    List<Player> teamPlayers = this.players.findByTeamIdAndActiveTrueOrderByIdAsc(teamId);
    PlayerImageSyncRun run = this.runs.findById(runId).orElseThrow();
    int updatedCount = 0;

    for (Player player : teamPlayers) {
      if (!force && player.getImageUrl() != null) {
        continue;
      }

      PlayerData match = null;
      List<PlayerData> matches =
          roster.stream()
              .filter(candidate -> this.matcher.matchesRoster(player, this.identity(candidate)))
              .toList();

      if (matches.size() == 1) {
        match = matches.getFirst();
      }

      if (match == null) {
        continue;
      }

      if (match.cutout() == null && match.thumbnail() == null && player.getImageUrl() == null) {
        continue;
      }

      Optional<PlayerExternalReference> owner =
          this.references.findByProviderAndExternalId(
              ExternalProvider.THE_SPORTS_DB, match.externalId());

      if (owner.isPresent() && !owner.get().getPlayer().getId().equals(player.getId())) {
        LOG.warn(
            "Conflicto: jugador {} ya tiene externalId {}",
            owner.get().getPlayer().getId(),
            match.externalId());
        continue;
      }

      if (player.getExternalReferences().stream()
          .noneMatch(r -> r.getProvider() == ExternalProvider.THE_SPORTS_DB)) {
        player.addExternalReference(ExternalProvider.THE_SPORTS_DB, match.externalId());
      }

      String prevImage = player.getImageUrl();
      String prevFallback = player.getFallbackImageUrl();
      player.applyImages(match.cutout(), match.thumbnail());
      this.players.saveAndFlush(player);

      boolean updated =
          !Objects.equals(prevImage, player.getImageUrl())
              || !Objects.equals(prevFallback, player.getFallbackImageUrl());

      PlayerImageResolution resolution =
          this.resolutions
              .findById(player.getId())
              .orElseGet(() -> this.resolutions.save(new PlayerImageResolution(player)));

      PlayerImageResolutionStatus outcome =
          player.getImageUrl() != null
              ? PlayerImageResolutionStatus.FOUND
              : PlayerImageResolutionStatus.NOT_FOUND;

      resolution.recordAttempt(outcome, this.clock.instant());

      PlayerImageSyncRunItem item =
          this.items
              .findByRunIdAndPlayerId(runId, player.getId())
              .orElseGet(
                  () -> {
                    run.evaluate();
                    return this.items.save(
                        new PlayerImageSyncRunItem(run, player, resolution.getStatus()));
                  });

      item.finish(
          outcome,
          PlayerImageSyncRunItemResult.valueOf(outcome.name()),
          null,
          true,
          updated,
          false,
          false,
          updated ? "Imagen asociada desde plantel" : "Sin sustitución de imagen",
          this.clock.instant());

      run.result(PlayerImageSyncRunItemResult.valueOf(outcome.name()), null, true, false);
      updatedCount++;
    }

    this.runs.saveAndFlush(run);
    LOG.info("Plantel procesado para equipo {}: {} jugadores actualizados", teamId, updatedCount);
    return updatedCount;
  }

  private String cleanTeamName(String name) {
    if (name == null) {
      return "";
    }
    String cleaned = name;
    boolean changed = true;
    while (changed) {
      changed = false;
      for (String suffix :
          Set.of(
              "FC",
              "CF",
              "AC",
              "SC",
              "CD",
              "UD",
              "AFC",
              "AS",
              "RC",
              "RCD",
              "ACF",
              "SV",
              "SCO",
              "OLYMPIQUE DE",
              "OLYMPIQUE",
              "STADE",
              "1901",
              "CA",
              "BALOMPIE",
              "1909")) {
        if (cleaned.endsWith(" " + suffix)) {
          cleaned = cleaned.substring(0, cleaned.length() - suffix.length() - 1).strip();
          changed = true;
          break;
        } else if (cleaned.startsWith(suffix + " ")) {
          cleaned = cleaned.substring(suffix.length() + 1).strip();
          changed = true;
          break;
        }
      }
    }
    return cleaned.strip();
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
}
