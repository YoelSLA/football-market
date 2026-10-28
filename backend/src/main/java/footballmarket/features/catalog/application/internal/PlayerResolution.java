package footballmarket.features.catalog.application.internal;

import footballmarket.features.catalog.domain.enums.ExternalProvider;
import footballmarket.features.catalog.domain.models.player.Player;
import footballmarket.features.catalog.domain.models.player.PlayerExternalReference;
import footballmarket.features.catalog.domain.models.team.Team;
import footballmarket.features.catalog.domain.records.InvalidPlayerObservation;
import footballmarket.features.catalog.domain.records.PlayerCandidate;
import footballmarket.features.catalog.infraestructure.PlayerExternalReferenceRepository;
import footballmarket.features.catalog.infraestructure.PlayerRepository;
import footballmarket.features.identity.application.internal.PendingReviewRecorder;
import footballmarket.features.identity.domain.enums.ReviewCategory;
import footballmarket.features.identity.domain.enums.ReviewCause;
import footballmarket.features.identity.domain.enums.ReviewSubjectType;
import footballmarket.features.images.domain.models.PlayerImageResolution;
import footballmarket.features.images.infraestructure.PlayerImageResolutionRepository;
import footballmarket.features.market.application.internal.PlayerTokenEmissionWriter;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * Resuelve los jugadores de la foto por su identidad externa y los persiste.
 *
 * <p>Un jugador se reconoce por su referencia de Football-Data, conserva su identificador interno y
 * se asocia obligatoriamente al equipo ya resuelto. Un jugador identificable cuyo equipo no puede
 * resolverse no se crea ni se actualiza, y el existente conserva su asociación y su actividad. Solo
 * los jugadores válidos presentes en la foto se reactivan; los ausentes se desactivan sin borrar su
 * historial. Las observaciones contradictorias del mismo propietario se clasifican en {@link
 * PlayerObservationConsolidation} antes de mutar al jugador.
 */
@Component
public class PlayerResolution {
  private final PlayerRepository players;
  private final PlayerExternalReferenceRepository references;
  private final PlayerImageResolutionRepository images;
  private final PlayerObservationConsolidation consolidation;
  private final PendingReviewRecorder reviews;
  private final PlayerTokenEmissionWriter emissions;

  public PlayerResolution(
      PlayerRepository players,
      PlayerExternalReferenceRepository references,
      PlayerImageResolutionRepository images,
      PlayerObservationConsolidation consolidation,
      PendingReviewRecorder reviews,
      PlayerTokenEmissionWriter emissions) {
    this.players = players;
    this.references = references;
    this.images = images;
    this.consolidation = consolidation;
    this.reviews = reviews;
    this.emissions = emissions;
  }

  /**
   * Aplica los jugadores observados contra los equipos ya resueltos.
   *
   * @param observed jugadores de la foto
   * @param invalid jugadores identificables con un campo obligatorio ausente
   * @param resolvedTeams equipos resueltos por identidad externa
   * @param excluded identidades externas ya descartadas por un intento anterior
   * @param protectedPlayers jugadores que ninguna ausencia puede desactivar o reasociar
   * @param attempt candidato en escritura, para acotar el replay ante una violación de unicidad
   * @return contadores de la aplicación
   */
  public PlayerApplication apply(
      List<PlayerCandidate> observed,
      List<InvalidPlayerObservation> invalid,
      Map<String, Team> resolvedTeams,
      Set<String> excluded,
      Set<Long> protectedPlayers,
      SynchronizationAttempt attempt) {
    Map<String, PlayerCandidate> candidates = new LinkedHashMap<>();
    for (PlayerCandidate candidate : observed) {
      candidates.merge(candidate.externalId(), candidate, PlayerCandidate::consolidatePresentation);
    }
    Set<String> excludedPlayers = new LinkedHashSet<>(excluded);
    Map<String, PlayerExternalReference> owners =
        this.loadOwners(candidates, excludedPlayers, protectedPlayers);
    this.recordReplayedConflicts(excluded, owners, protectedPlayers);
    this.protectInvalid(invalid, protectedPlayers);
    this.protectUnresolvedTeam(candidates, resolvedTeams, excludedPlayers, protectedPlayers);
    this.consolidation.classify(
        candidates, owners, resolvedTeams, excludedPlayers, protectedPlayers);

    int created = 0;
    int updated = 0;
    for (PlayerCandidate candidate : candidates.values()) {
      PlayerExternalReference reference = owners.get(candidate.externalId());
      if (excludedPlayers.contains(candidate.externalId())
          || reference != null && protectedPlayers.contains(reference.getPlayer().getId())) {
        excludedPlayers.add(candidate.externalId());
        continue;
      }
      attempt.set(ReviewSubjectType.PLAYER, candidate.externalId());
      if (reference == null) {
        this.create(candidate, resolvedTeams.get(candidate.teamExternalId()));
        created++;
      } else {
        this.update(
            reference.getPlayer(), candidate, resolvedTeams.get(candidate.teamExternalId()));
        updated++;
      }
      this.players.flush();
      attempt.clear();
    }
    int inactive = this.inactivateMissing(candidates.keySet(), protectedPlayers);
    return new PlayerApplication(created, updated, inactive, excludedPlayers.size());
  }

  /**
   * El alta nace con su referencia de Football-Data, su resolución de imagen pendiente y la emisión
   * inicial de tokens a nombre del superusuario.
   */
  private void create(PlayerCandidate candidate, Team team) {
    Player player = new Player(candidate.name(), team, candidate.position());
    player.addExternalReference(ExternalProvider.FOOTBALL_DATA, candidate.externalId());
    player.updateOptionalDetails(candidate.dateOfBirth(), candidate.nationality());
    this.players.save(player);
    this.images.save(new PlayerImageResolution(player));
    this.emissions.emitFor(player);
    this.reviews.reconcile(ReviewSubjectType.PLAYER, candidate.externalId(), player.getId());
  }

  /** La actualización conserva la identidad interna y reactiva al jugador presente en la foto. */
  private void update(Player player, PlayerCandidate candidate, Team team) {
    player.update(candidate.name(), team, candidate.position());
    player.updateOptionalDetails(candidate.dateOfBirth(), candidate.nationality());
    player.activate();
    this.reviews.reconcile(ReviewSubjectType.PLAYER, candidate.externalId(), player.getId());
  }

  /**
   * Localiza el propietario actual de cada identidad observada y detecta cuando la referencia ya
   * pertenece a un jugador distinto del que la foto implica.
   */
  private Map<String, PlayerExternalReference> loadOwners(
      Map<String, PlayerCandidate> candidates, Set<String> excluded, Set<Long> protectedPlayers) {
    Map<String, Long> expected = new LinkedHashMap<>();
    this.references
        .findIdentitiesByProvider(ExternalProvider.FOOTBALL_DATA)
        .forEach(identity -> expected.put(identity.getExternalId(), identity.getPlayerId()));
    Map<String, PlayerExternalReference> owners = new LinkedHashMap<>();
    for (String externalId : candidates.keySet()) {
      PlayerExternalReference reference =
          this.references
              .findByProviderAndExternalId(ExternalProvider.FOOTBALL_DATA, externalId)
              .orElse(null);
      owners.put(externalId, reference);
      Long previous = expected.get(externalId);
      Long actual = reference == null ? null : reference.getPlayer().getId();
      if (previous == null || previous.equals(actual)) {
        continue;
      }
      excluded.add(externalId);
      protectedPlayers.add(previous);
      Map<String, Object> evidence = new LinkedHashMap<>();
      evidence.put("intendedOwnerId", previous);
      if (actual != null) {
        protectedPlayers.add(actual);
        evidence.put("currentOwnerId", actual);
      }
      this.reviews.record(
          ReviewCategory.EXTERNAL_IDENTITY_CONFLICT,
          ReviewCause.EXTERNAL_IDENTITY_OWNED,
          ReviewSubjectType.PLAYER,
          previous,
          externalId,
          "owner:" + previous,
          evidence);
    }
    return owners;
  }

  /** Reexpresa como conflicto los descartes de identidad acumulados por un intento anterior. */
  private void recordReplayedConflicts(
      Set<String> excluded,
      Map<String, PlayerExternalReference> owners,
      Set<Long> protectedPlayers) {
    for (String externalId : excluded) {
      PlayerExternalReference owner = owners.get(externalId);
      Map<String, Object> evidence = new LinkedHashMap<>();
      evidence.put("externalId", externalId);
      evidence.put("intendedOwner", "NEW_PLAYER");
      if (owner != null) {
        evidence.put("currentOwnerId", owner.getPlayer().getId());
        protectedPlayers.add(owner.getPlayer().getId());
      }
      this.reviews.record(
          ReviewCategory.EXTERNAL_IDENTITY_CONFLICT,
          ReviewCause.EXTERNAL_IDENTITY_OWNED,
          ReviewSubjectType.PLAYER,
          owner == null ? null : owner.getPlayer().getId(),
          externalId,
          "new-player",
          evidence);
    }
  }

  /** Un inválido identificable no impide procesar a los demás y conserva el estado anterior. */
  private void protectInvalid(List<InvalidPlayerObservation> invalid, Set<Long> protectedPlayers) {
    for (InvalidPlayerObservation observation : invalid) {
      this.protectPlayer(
          observation.externalId(),
          observation.teamExternalId(),
          observation.teamName(),
          ReviewCause.MISSING_REQUIRED_DATA,
          protectedPlayers,
          observation.cause());
    }
  }

  /** Un jugador cuyo equipo no resolvió en esta foto no se crea ni se actualiza. */
  private void protectUnresolvedTeam(
      Map<String, PlayerCandidate> candidates,
      Map<String, Team> resolvedTeams,
      Set<String> excludedPlayers,
      Set<Long> protectedPlayers) {
    for (PlayerCandidate candidate : candidates.values()) {
      if (resolvedTeams.containsKey(candidate.teamExternalId())) {
        continue;
      }
      excludedPlayers.add(candidate.externalId());
      this.protectPlayer(
          candidate.externalId(),
          candidate.teamExternalId(),
          candidate.team(),
          ReviewCause.TEAM_UNRESOLVED,
          protectedPlayers,
          null);
    }
  }

  /**
   * Un jugador identificable sin equipo resoluble conserva su asociación y su actividad, y queda
   * protegido para que una ausencia posterior no lo desactive.
   */
  private void protectPlayer(
      String externalId,
      String teamExternalId,
      String teamName,
      ReviewCause cause,
      Set<Long> protectedPlayers,
      String diagnostic) {
    Player player =
        this.references
            .findByProviderAndExternalId(ExternalProvider.FOOTBALL_DATA, externalId)
            .map(reference -> reference.getPlayer())
            .orElse(null);
    Map<String, Object> evidence = new LinkedHashMap<>();
    evidence.put("receivedTeamExternalId", teamExternalId == null ? "" : teamExternalId);
    evidence.put("receivedTeamName", teamName == null ? "" : teamName);
    if (diagnostic != null) {
      evidence.put("diagnostic", diagnostic);
    }
    if (player != null) {
      protectedPlayers.add(player.getId());
      evidence.put("previousTeamId", player.getTeam().getId());
    }
    this.reviews.record(
        ReviewCategory.PLAYER_TEAM_UNRESOLVED,
        cause,
        ReviewSubjectType.PLAYER,
        player == null ? null : player.getId(),
        externalId,
        teamExternalId == null ? "missing" : teamExternalId,
        evidence);
  }

  /** Desactiva únicamente al jugador activo y protegido que la foto completa no confirmó. */
  private int inactivateMissing(Set<String> present, Set<Long> protectedPlayers) {
    List<PlayerExternalReference> references =
        this.references.findByProvider(ExternalProvider.FOOTBALL_DATA);
    for (PlayerExternalReference reference : references) {
      if (present.contains(reference.getExternalId())) {
        protectedPlayers.add(reference.getPlayer().getId());
      }
    }
    int inactive = 0;
    for (PlayerExternalReference reference : references) {
      Player player = reference.getPlayer();
      if (player.isActive() && !protectedPlayers.contains(player.getId())) {
        player.deactivate();
        inactive++;
      }
    }
    return inactive;
  }
}
