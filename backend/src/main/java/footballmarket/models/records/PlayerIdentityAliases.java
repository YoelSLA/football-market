package footballmarket.models.records;

import java.util.Map;
import java.util.Set;

/** Fuente revisable y acotada de equivalencias conservadoras para la identidad deportiva. */
public record PlayerIdentityAliases(Set<String> teamSuffixes, Map<String, String> nationalities) {
  public PlayerIdentityAliases {
    teamSuffixes = Set.copyOf(teamSuffixes);
    nationalities = Map.copyOf(nationalities);
  }

  public static PlayerIdentityAliases controlled() {
    return new PlayerIdentityAliases(
        Set.of("FC", "CF", "AC", "SC"),
        Map.of(
            "USA",
            "UNITED STATES",
            "US",
            "UNITED STATES",
            "ESPAÑA",
            "SPAIN",
            "UK",
            "UNITED KINGDOM"));
  }
}
