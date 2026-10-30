package footballmarket.features.catalog.application.internal;

import footballmarket.features.catalog.domain.enums.ExternalProvider;
import footballmarket.features.catalog.domain.models.player.Player;
import footballmarket.features.catalog.domain.models.player.PlayerExternalReference;
import footballmarket.features.catalog.domain.models.player.PlayerPresentation;
import footballmarket.features.catalog.domain.models.team.Team;
import footballmarket.features.catalog.domain.models.team.TeamNameNormalizer;
import footballmarket.features.catalog.domain.records.PlayerCandidate;
import footballmarket.features.identity.application.internal.PendingReviewRecorder;
import footballmarket.features.identity.domain.enums.ReviewCategory;
import footballmarket.features.identity.domain.enums.ReviewCause;
import footballmarket.features.identity.domain.enums.ReviewSubjectType;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * Clasifica las observaciones de un mismo propietario antes de mutar su jugador.
 *
 * <p>Un jugador puede recibir varias referencias del mismo proveedor y, por tanto, varias
 * observaciones en la misma foto. Si el equipo, el nombre o la posición se contradicen, el jugador
 * se protege entero y sus observaciones se registran como un único caso: ninguna se elige. Si las
 * observaciones son equivalentes, se conserva la presentación ya persistida y los opcionales
 * incompatibles no invalidan al jugador.
 */
@Component
public class PlayerObservationConsolidation {
  private final PendingReviewRecorder reviews;

  public PlayerObservationConsolidation(PendingReviewRecorder reviews) {
    this.reviews = reviews;
  }

  /**
   * Reescribe los candidatos coherentes de cada propietario y marca los contradictivos.
   *
   * @param candidates candidatos de la foto, indexados por identidad externa
   * @param owners propietario actual de cada identidad observada
   * @param resolvedTeams equipos resueltos por identidad externa
   * @param excluded identidades que este intento no procesa
   * @param protectedPlayers jugadores que ninguna ausencia ni reasociación puede tocar
   */
  public void classify(
      Map<String, PlayerCandidate> candidates,
      Map<String, PlayerExternalReference> owners,
      Map<String, Team> resolvedTeams,
      Set<String> excluded,
      Set<Long> protectedPlayers) {
    for (List<PlayerCandidate> observations :
        this.group(candidates, owners, resolvedTeams, excluded)) {
      Player player = owners.get(observations.getFirst().externalId()).getPlayer();
      long teamCount = this.distinctTeams(observations, resolvedTeams);
      if (teamCount > 1
          || this.distinctNormalized(observations, PlayerCandidate::name) > 1
          || this.distinctNormalized(observations, PlayerCandidate::position) > 1) {
        this.protect(
            player, observations, resolvedTeams, excluded, protectedPlayers, teamCount > 1);
        continue;
      }
      this.converge(candidates, player, observations);
    }
  }

  /** Agrupa por propietario únicamente las observaciones que este intento puede procesar. */
  private List<List<PlayerCandidate>> group(
      Map<String, PlayerCandidate> candidates,
      Map<String, PlayerExternalReference> owners,
      Map<String, Team> resolvedTeams,
      Set<String> excluded) {
    Map<Long, List<PlayerCandidate>> groups = new LinkedHashMap<>();
    for (PlayerCandidate candidate : candidates.values()) {
      PlayerExternalReference owner = owners.get(candidate.externalId());
      if (owner != null
          && !excluded.contains(candidate.externalId())
          && resolvedTeams.containsKey(candidate.teamExternalId())) {
        groups
            .computeIfAbsent(owner.getPlayer().getId(), ignored -> new ArrayList<>())
            .add(candidate);
      }
    }
    return new ArrayList<>(groups.values());
  }

  /** Dos observaciones discrepan cuando su equipo resuelto es un equipo interno distinto. */
  private long distinctTeams(List<PlayerCandidate> observations, Map<String, Team> resolvedTeams) {
    return observations.stream()
        .map(candidate -> resolvedTeams.get(candidate.teamExternalId()).getId())
        .distinct()
        .count();
  }

  /** La comparación de texto es estricta por normalización, no por orden ni por formato. */
  private long distinctNormalized(
      List<PlayerCandidate> observations,
      java.util.function.Function<PlayerCandidate, String> attribute) {
    return observations.stream()
        .map(attribute)
        .map(TeamNameNormalizer::normalize)
        .distinct()
        .count();
  }

  /** El sujeto contradictorio se conserva íntegro y sus observaciones se registran una sola vez. */
  private void protect(
      Player player,
      List<PlayerCandidate> observations,
      Map<String, Team> resolvedTeams,
      Set<String> excluded,
      Set<Long> protectedPlayers,
      boolean conflictingTeams) {
    protectedPlayers.add(player.getId());
    observations.forEach(candidate -> excluded.add(candidate.externalId()));
    Map<String, Object> evidence = new LinkedHashMap<>();
    evidence.put("previousActive", player.isActive());
    evidence.put("previousTeamId", player.getTeam().getId());
    evidence.put(
        "observations",
        observations.stream()
            .sorted(Comparator.comparing(PlayerCandidate::externalId))
            .map(candidate -> this.observation(candidate, resolvedTeams))
            .toList());
    // Conservar la clave previa del problema; cambiar el atributo contradictorio no duplica el
    // caso.
    this.reviews.observe(
        "INVALID_SUBJECT_DATA:PLAYER:" + player.getId() + ":CONFLICTING_PLAYER_TEAMS",
        ReviewCategory.INVALID_SUBJECT_DATA,
        conflictingTeams
            ? ReviewCause.CONFLICTING_PLAYER_TEAMS
            : ReviewCause.CONFLICTING_PLAYER_STATE,
        ReviewSubjectType.PLAYER,
        player.getId(),
        null,
        evidence);
  }

  private Map<String, Object> observation(
      PlayerCandidate candidate, Map<String, Team> resolvedTeams) {
    return Map.of(
        "provider",
        ExternalProvider.FOOTBALL_DATA.name(),
        "externalId",
        candidate.externalId(),
        "name",
        candidate.name(),
        "position",
        candidate.position(),
        "teamExternalId",
        candidate.teamExternalId(),
        "teamId",
        resolvedTeams.get(candidate.teamExternalId()).getId());
  }

  /** Unifica observaciones equivalentes conservando la presentación ya persistida. */
  private void converge(
      Map<String, PlayerCandidate> candidates, Player player, List<PlayerCandidate> observations) {
    String name =
        PlayerPresentation.equivalent(player.getName(), observations.getFirst().name())
            ? player.getName()
            : PlayerPresentation.canonical(
                observations.stream().map(PlayerCandidate::name).toList());
    String position =
        PlayerPresentation.equivalent(player.getPosition(), observations.getFirst().position())
            ? player.getPosition()
            : PlayerPresentation.canonical(
                observations.stream().map(PlayerCandidate::position).toList());
    List<LocalDate> dates = distinct(observations, PlayerCandidate::dateOfBirth);
    List<String> nationalities = distinct(observations, PlayerCandidate::nationality);
    if (dates.size() > 1) {
      this.optionalConflict(player, ReviewCause.CONFLICTING_DATE_OF_BIRTH, "dateOfBirth", dates);
    }
    if (nationalities.size() > 1) {
      this.optionalConflict(
          player, ReviewCause.CONFLICTING_NATIONALITY, "nationality", nationalities);
    }
    for (PlayerCandidate candidate : observations) {
      candidates.put(
          candidate.externalId(),
          this.converged(candidate, name, position, dates, nationalities, player));
    }
  }

  private PlayerCandidate converged(
      PlayerCandidate candidate,
      String name,
      String position,
      List<LocalDate> dates,
      List<String> nationalities,
      Player player) {
    return new PlayerCandidate(
        candidate.externalId(),
        name,
        candidate.team(),
        candidate.league(),
        position,
        dates.size() == 1 ? dates.getFirst() : player.getDateOfBirth(),
        nationalities.size() == 1 ? nationalities.getFirst() : player.getNationality(),
        candidate.teamExternalId());
  }

  /** Los opcionales incompatibles no invalidan al jugador ni borran el valor persistido. */
  private void optionalConflict(
      Player player, ReviewCause cause, String attribute, List<?> values) {
    Map<String, Object> evidence = new LinkedHashMap<>();
    evidence.put("attribute", attribute);
    evidence.put("receivedValues", values);
    if (player.getDateOfBirth() != null || player.getNationality() != null) {
      evidence.put(
          "previousValues",
          Map.of(
              "dateOfBirth",
              player.getDateOfBirth() == null ? "" : player.getDateOfBirth().toString(),
              "nationality",
              player.getNationality() == null ? "" : player.getNationality()));
    }
    this.reviews.record(
        ReviewCategory.PLAYER_OPTIONAL_CONFLICT,
        cause,
        ReviewSubjectType.PLAYER,
        player.getId(),
        null,
        attribute,
        evidence);
  }

  private <T> List<T> distinct(
      List<PlayerCandidate> observations,
      java.util.function.Function<PlayerCandidate, T> attribute) {
    return observations.stream().map(attribute).filter(Objects::nonNull).distinct().toList();
  }
}
