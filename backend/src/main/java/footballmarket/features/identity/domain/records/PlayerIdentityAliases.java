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
        Set.of(
            "FC",
            "CF",
            "AC",
            "SC",
            "AFC",
            "AS",
            "RC",
            "RCD",
            "ACF",
            "RACING CLUB DE",
            "ALSACE",
            "SV",
            "SCO",
            "OLYMPIQUE DE",
            "OLYMPIQUE",
            "STADE",
            "1901",
            "CA",
            "BALOMPIE",
            "1909"),
        Map.ofEntries(
            Map.entry("USA", "UNITED STATES"),
            Map.entry("US", "UNITED STATES"),
            Map.entry("ESPAÑA", "SPAIN"),
            Map.entry("UK", "UNITED KINGDOM"),
            Map.entry("ENGLAND", "UNITED KINGDOM"),
            Map.entry("SCOTLAND", "UNITED KINGDOM"),
            Map.entry("WALES", "UNITED KINGDOM"),
            Map.entry("NORTHERN IRELAND", "UNITED KINGDOM"),
            Map.entry("GREAT BRITAIN", "UNITED KINGDOM"),
            Map.entry("THE NETHERLANDS", "NETHERLANDS"),
            Map.entry("DR CONGO", "CONGO DR"),
            Map.entry("DEMOCRATIC REPUBLIC OF THE CONGO", "CONGO DR"),
            Map.entry("CONGO, DEMOCRATIC REPUBLIC OF THE", "CONGO DR"),
            Map.entry("CÔTE D'IVOIRE", "IVORY COAST"),
            Map.entry("COTE D'IVOIRE", "IVORY COAST"),
            Map.entry("KOREA, SOUTH", "SOUTH KOREA"),
            Map.entry("BOSNIA-HERZEGOVINA", "BOSNIA AND HERZEGOVINA"),
            Map.entry("BOSNIA & HERZEGOVINA", "BOSNIA AND HERZEGOVINA"),
            Map.entry("CAPE VERDE ISLANDS", "CAPE VERDE"),
            Map.entry("CHINA PR", "CHINA"),
            Map.entry("CZECH REPUBLIC", "CZECHIA"),
            Map.entry("REPUBLIC OF IRELAND", "IRELAND"),
            Map.entry("NORTH MACEDONIA", "MACEDONIA"),
            Map.entry("TÜRKIYE", "TURKEY"),
            Map.entry("TURKIYE", "TURKEY")));
  }
}
