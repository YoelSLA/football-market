package footballmarket.services.internal;

import footballmarket.models.League;
import footballmarket.models.Player;
import footballmarket.models.Team;
import footballmarket.models.enums.ExternalProvider;
import footballmarket.models.enums.ReviewCategory;
import footballmarket.models.enums.ReviewCause;
import footballmarket.models.enums.ReviewSubjectType;
import footballmarket.models.records.TeamCandidate;
import footballmarket.repositories.PlayerRepository;
import footballmarket.repositories.TeamExternalReferenceRepository;
import footballmarket.repositories.TeamRepository;
import footballmarket.services.exceptions.IncompletePhotoException;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * Resuelve los equipos de la foto y mantiene su vigencia.
 *
 * <p>Un equipo se reconoce por su referencia externa, se asocia obligatoriamente a la liga ya
 * resuelta y conserva su identificador ante un renombrado o un cambio de liga. Un equipo sin liga
 * resoluble o sin plantel determinable no se crea ni se actualiza y queda registrado; su catálogo y
 * el de sus jugadores quedan protegidos de una ausencia. Los equipos ausentes de una foto completa
 * que ninguna protección mantiene salen del catálogo sin borrado físico.
 */
@Component
public class TeamResolution {
  private final TeamRepository teams;
  private final TeamExternalReferenceRepository references;
  private final PlayerRepository players;
  private final PendingReviewRecorder reviews;

  public TeamResolution(
      TeamRepository teams,
      TeamExternalReferenceRepository references,
      PlayerRepository players,
      PendingReviewRecorder reviews) {
    this.teams = teams;
    this.references = references;
    this.players = players;
    this.reviews = reviews;
  }

  /**
   * Aplica los equipos observados y retira los ausentes que no están protegidos.
   *
   * @param observed equipos de la foto
   * @param resolvedLeagues ligas ya resueltas por identidad externa
   * @param excluded identidades externas ya descartadas por un intento anterior
   * @param excludedLeagues identidades de liga descartadas por un intento anterior
   * @param protectedPlayers jugadores que ninguna retirada puede desactivar o reasociar
   * @param attempt candidato en escritura, para acotar el replay ante una violación de unicidad
   * @return equipos resueltos por identidad externa
   * @throws IncompletePhotoException si una identidad requerida no es utilizable
   */
  public Map<String, Team> resolve(
      List<TeamCandidate> observed,
      Map<String, League> resolvedLeagues,
      Set<String> excluded,
      Set<String> excludedLeagues,
      Set<Long> protectedPlayers,
      SynchronizationAttempt attempt) {
    Map<String, Team> resolved = new LinkedHashMap<>();
    Set<Long> protectedTeams = new LinkedHashSet<>();
    for (TeamCandidate candidate : observed) {
      if (!validText(candidate.externalId())) {
        throw new IncompletePhotoException();
      }
      Team team =
          this.references
              .findByProviderAndExternalId(ExternalProvider.FOOTBALL_DATA, candidate.externalId())
              .map(reference -> reference.getTeam())
              .orElse(null);
      Long id = team == null ? null : team.getId();
      League league = resolvedLeagues.get(candidate.leagueExternalId());
      if (excluded.contains(candidate.externalId())) {
        this.protect(team, protectedTeams, protectedPlayers);
        this.conflict(candidate.externalId(), id);
        continue;
      }
      if (league == null && !excludedLeagues.contains(candidate.leagueExternalId())) {
        throw new IncompletePhotoException();
      }
      if (!validText(candidate.name()) || league == null || !candidate.rosterKnown()) {
        this.protect(team, protectedTeams, protectedPlayers);
        this.reviews.record(
            ReviewCategory.TEAM_LEAGUE_UNRESOLVED,
            ReviewCause.LEAGUE_UNRESOLVED,
            ReviewSubjectType.TEAM,
            id,
            candidate.externalId(),
            candidate.leagueExternalId() == null ? "missing" : candidate.leagueExternalId(),
            Map.of(
                "receivedLeagueExternalId",
                candidate.leagueExternalId() == null ? "" : candidate.leagueExternalId()));
        continue;
      }
      if (team == null) {
        attempt.set(ReviewSubjectType.TEAM, candidate.externalId());
        team = new Team(candidate.name(), league, true);
        team.addExternalReference(ExternalProvider.FOOTBALL_DATA, candidate.externalId());
        this.teams.saveAndFlush(team);
        attempt.clear();
      } else {
        team.update(candidate.name(), league);
        team.markCurrent();
      }
      resolved.put(candidate.externalId(), team);
      this.reviews.reconcile(ReviewSubjectType.TEAM, candidate.externalId(), team.getId());
    }
    this.retireAbsent(resolved, protectedTeams);
    return resolved;
  }

  /**
   * Un equipo cuya liga o plantel no puede determinarse conserva su estado, y con él el de sus
   * jugadores, para que una ausencia posterior no lo desactive ni lo desasocie.
   */
  private void protect(Team team, Set<Long> protectedTeams, Set<Long> protectedPlayers) {
    if (team == null) {
      return;
    }
    protectedTeams.add(team.getId());
    for (Player player : this.players.findAll()) {
      if (player.getTeam().getId().equals(team.getId())) {
        protectedPlayers.add(player.getId());
      }
    }
  }

  /** La identidad externa pertenece a otro propietario: se registra y no se reasigna. */
  private void conflict(String externalId, Long owner) {
    Map<String, Object> evidence = new LinkedHashMap<>();
    evidence.put("externalId", externalId);
    evidence.put("intendedOwner", "NEW_TEAM");
    if (owner != null) {
      evidence.put("currentOwnerId", owner);
    }
    this.reviews.record(
        ReviewCategory.EXTERNAL_IDENTITY_CONFLICT,
        ReviewCause.EXTERNAL_IDENTITY_OWNED,
        ReviewSubjectType.TEAM,
        owner,
        externalId,
        "new-team",
        evidence);
  }

  /** Solo sale del catálogo el equipo ausente que ninguna protección mantiene vigente. */
  private void retireAbsent(Map<String, Team> resolved, Set<Long> protectedTeams) {
    Set<Long> present = new HashSet<>();
    resolved.values().forEach(team -> present.add(team.getId()));
    for (Team team : this.teams.findAll()) {
      if (!present.contains(team.getId()) && !protectedTeams.contains(team.getId())) {
        team.retire();
      }
    }
    this.teams.flush();
  }

  private static boolean validText(String value) {
    return value != null && !value.isBlank() && value.length() <= 255;
  }
}
