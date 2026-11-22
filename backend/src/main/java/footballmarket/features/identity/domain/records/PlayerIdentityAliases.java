package footballmarket.features.identity.domain.records;

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
        Map.ofEntries(
            Map.entry("USA", "UNITED STATES"),
            Map.entry("US", "UNITED STATES"),
            Map.entry("ESPAÑA", "SPAIN"),
            Map.entry("UK", "UNITED KINGDOM"),
            Map.entry("THE NETHERLANDS", "NETHERLANDS"),
            Map.entry("DR CONGO", "CONGO DR"),
            Map.entry("DEMOCRATIC REPUBLIC OF THE CONGO", "CONGO DR"),
            Map.entry("CONGO, DEMOCRATIC REPUBLIC OF THE", "CONGO DR"),
            Map.entry("CÔTE D'IVOIRE", "IVORY COAST"),
            Map.entry("COTE D'IVOIRE", "IVORY COAST"),
            Map.entry("KOREA, SOUTH", "SOUTH KOREA")));
  }
}
