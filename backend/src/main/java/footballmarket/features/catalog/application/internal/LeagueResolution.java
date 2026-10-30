package footballmarket.features.catalog.application.internal;

import footballmarket.features.catalog.application.exceptions.IncompletePhotoException;
import footballmarket.features.catalog.domain.enums.ExternalProvider;
import footballmarket.features.catalog.domain.models.league.League;
import footballmarket.features.catalog.domain.records.LeagueCandidate;
import footballmarket.features.catalog.infraestructure.LeagueExternalReferenceRepository;
import footballmarket.features.catalog.infraestructure.LeagueRepository;
import footballmarket.features.identity.application.internal.PendingReviewRecorder;
import footballmarket.features.identity.domain.enums.ReviewCategory;
import footballmarket.features.identity.domain.enums.ReviewCause;
import footballmarket.features.identity.domain.enums.ReviewSubjectType;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * Resuelve las ligas de la foto por su identidad externa estable.
 *
 * <p>Una liga se reconoce por su referencia de proveedor, se crea cuando no existe y conserva su
 * identificador interno ante un renombrado. Una liga sin nombre válido no se crea ni se renombra y
 * queda registrada; una identidad externa en conflicto con otro propietario no se reasigna.
 */
@Component
public class LeagueResolution {
  private final LeagueRepository leagues;
  private final LeagueExternalReferenceRepository references;
  private final PendingReviewRecorder reviews;

  public LeagueResolution(
      LeagueRepository leagues,
      LeagueExternalReferenceRepository references,
      PendingReviewRecorder reviews) {
    this.leagues = leagues;
    this.references = references;
    this.reviews = reviews;
  }

  /**
   * Aplica las ligas observadas y devuelve las resolubles por identidad externa.
   *
   * @param observed ligas de la foto
   * @param excluded identidades externas ya descartadas por un intento anterior
   * @param attempt candidato en escritura, para acotar el replay ante una violación de unicidad
   * @return ligas resueltas por identidad externa
   * @throws IncompletePhotoException si una identidad externa requerida no es utilizable
   */
  public Map<String, League> resolve(
      List<LeagueCandidate> observed, Set<String> excluded, SynchronizationAttempt attempt) {
    Map<String, League> resolved = new LinkedHashMap<>();
    for (LeagueCandidate candidate : observed) {
      if (!validText(candidate.externalId())) {
        throw new IncompletePhotoException();
      }
      League league =
          this.references
              .findByProviderAndExternalId(ExternalProvider.FOOTBALL_DATA, candidate.externalId())
              .map(reference -> reference.getLeague())
              .orElse(null);
      Long id = league == null ? null : league.getId();
      if (excluded.contains(candidate.externalId())) {
        this.conflict(candidate.externalId(), id);
        continue;
      }
      if (!validText(candidate.name())) {
        this.reviews.record(
            ReviewCategory.INVALID_SUBJECT_DATA,
            ReviewCause.MISSING_REQUIRED_DATA,
            ReviewSubjectType.LEAGUE,
            id,
            candidate.externalId(),
            "data",
            Map.of("missing", "name"));
        continue;
      }
      if (league == null) {
        attempt.set(ReviewSubjectType.LEAGUE, candidate.externalId());
        league = new League(candidate.name());
        league.addExternalReference(ExternalProvider.FOOTBALL_DATA, candidate.externalId());
        this.leagues.saveAndFlush(league);
        attempt.clear();
      } else {
        league.rename(candidate.name());
      }
      resolved.put(candidate.externalId(), league);
      this.reviews.reconcile(ReviewSubjectType.LEAGUE, candidate.externalId(), league.getId());
    }
    return resolved;
  }

  /** La identidad externa pertenece a otro propietario: se registra y no se reasigna. */
  private void conflict(String externalId, Long owner) {
    Map<String, Object> evidence = new LinkedHashMap<>();
    evidence.put("externalId", externalId);
    evidence.put("intendedOwner", "NEW_LEAGUE");
    if (owner != null) {
      evidence.put("currentOwnerId", owner);
    }
    this.reviews.record(
        ReviewCategory.EXTERNAL_IDENTITY_CONFLICT,
        ReviewCause.EXTERNAL_IDENTITY_OWNED,
        ReviewSubjectType.LEAGUE,
        owner,
        externalId,
        "new-league",
        evidence);
  }

  private static boolean validText(String value) {
    return value != null && !value.isBlank() && value.length() <= 255;
  }
}
