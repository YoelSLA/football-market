package footballmarket.models;

import footballmarket.models.records.PlayerIdentityAliases;
import java.text.Normalizer;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

/** Comparación conservadora de identidades sin puntuaciones ni acceso al proveedor. */
public final class PlayerIdentityMatcher {
  private static final Pattern MARKS = Pattern.compile("\\p{M}+");
  private static final Pattern SEPARATORS = Pattern.compile("[\\s-]+");
  private final PlayerIdentityAliases aliases;

  public PlayerIdentityMatcher(PlayerIdentityAliases aliases) {
    this.aliases = aliases;
  }

  /** Identidad del candidato sin contrato externo ni datos de infraestructura. */
  public record Candidate(
      String name,
      String alternateName,
      String team,
      String sport,
      LocalDate dateOfBirth,
      String nationality) {}

  /** Acepta un único candidato válido, nunca el primero de varios válidos. */
  public Optional<Candidate> uniqueMatch(Player player, List<Candidate> candidates) {
    List<Candidate> matches =
        candidates.stream().filter(candidate -> this.matches(player, candidate)).toList();
    return matches.size() == 1 ? Optional.of(matches.getFirst()) : Optional.empty();
  }

  /** Reutiliza las reglas fuertes para validar la identidad consultada por un ID ya conocido. */
  public boolean matches(Player player, Candidate candidate) {
    if (player.getTeam() == null
        || candidate == null
        || !"Soccer".equals(candidate.sport())
        || (!normalize(player.getName()).equals(normalize(candidate.name()))
            && !normalize(player.getName()).equals(normalize(candidate.alternateName())))) {
      return false;
    }
    boolean sameTeam =
        this.normalizeTeam(player.getTeam().getName()).equals(this.normalizeTeam(candidate.team()));
    boolean sameDate =
        player.getDateOfBirth() != null
            && candidate.dateOfBirth() != null
            && player.getDateOfBirth().equals(candidate.dateOfBirth());
    boolean sameCountry =
        player.getNationality() != null
            && candidate.nationality() != null
            && this.normalizeNationality(player.getNationality())
                .equals(this.normalizeNationality(candidate.nationality()));
    if (!sameTeam) {
      return sameDate && sameCountry;
    }
    return (player.getDateOfBirth() == null || candidate.dateOfBirth() == null || sameDate)
        && (player.getNationality() == null || candidate.nationality() == null || sameCountry);
  }

  private String normalizeNationality(String nationality) {
    String normalized = normalize(nationality);
    for (var entry : this.aliases.nationalities().entrySet()) {
      if (normalized.equals(normalize(entry.getKey()))) {
        return normalize(entry.getValue());
      }
    }
    return normalized;
  }

  private String normalizeTeam(String team) {
    String normalized = normalize(team);
    for (String suffix : this.aliases.teamSuffixes()) {
      String normalizedSuffix = normalize(suffix);
      if (normalized.endsWith(" " + normalizedSuffix)) {
        return normalized.substring(0, normalized.length() - normalizedSuffix.length() - 1);
      }
      if (normalized.startsWith(normalizedSuffix + " ")) {
        return normalized.substring(normalizedSuffix.length() + 1);
      }
    }
    return normalized;
  }

  private static String normalize(String text) {
    if (text == null) {
      return "";
    }
    String withoutMarks =
        MARKS.matcher(Normalizer.normalize(text, Normalizer.Form.NFD)).replaceAll("");
    return SEPARATORS
        .matcher(withoutMarks.strip().toUpperCase(java.util.Locale.ROOT))
        .replaceAll(" ");
  }
}
