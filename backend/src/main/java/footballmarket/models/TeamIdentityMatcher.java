package footballmarket.models;

import footballmarket.models.enums.TeamResolutionResult;
import footballmarket.models.exceptions.InvalidCatalogEntityException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Resolución estricta por identidad distinta, nunca por número de filas ni relevancia. */
public final class TeamIdentityMatcher {
  private TeamIdentityMatcher() {}

  /** Datos de dominio desacoplados del contrato HTTP externo. */
  public record Candidate(String externalId, String name, String sport) {}

  /** Resultado puro; externalId solo está presente para una coincidencia confiable. */
  public record Evaluation(TeamResolutionResult result, String externalId) {}

  public static Evaluation evaluate(String name, List<Candidate> candidates, boolean complete) {
    Map<String, Candidate> distinct = new LinkedHashMap<>();
    for (Candidate candidate : candidates) {
      if (candidate == null
          || candidate.externalId() == null
          || candidate.externalId().isBlank()
          || candidate.externalId().length() > 255) {
        throw new InvalidCatalogEntityException(
            "La evaluación externa contiene una identidad inválida");
      }
      Candidate previous = distinct.putIfAbsent(candidate.externalId(), candidate);
      if (previous != null
          && (!TeamNameNormalizer.normalize(previous.name())
                  .equals(TeamNameNormalizer.normalize(candidate.name()))
              || !java.util.Objects.equals(previous.sport(), candidate.sport()))) {
        throw new InvalidCatalogEntityException(
            "La evaluación contiene identidades contradictorias");
      }
    }
    if (!complete) {
      return new Evaluation(TeamResolutionResult.COMPLETENESS_UNPROVEN, null);
    }
    List<Candidate> matches =
        distinct.values().stream()
            .filter(
                candidate ->
                    "Soccer".equals(candidate.sport())
                        && candidate.name() != null
                        && !candidate.name().isBlank()
                        && TeamNameNormalizer.normalize(name)
                            .equals(TeamNameNormalizer.normalize(candidate.name())))
            .toList();
    if (matches.size() == 1) {
      return new Evaluation(TeamResolutionResult.MATCH, matches.getFirst().externalId());
    }
    return new Evaluation(
        matches.isEmpty() ? TeamResolutionResult.NO_MATCH : TeamResolutionResult.AMBIGUOUS, null);
  }
}
