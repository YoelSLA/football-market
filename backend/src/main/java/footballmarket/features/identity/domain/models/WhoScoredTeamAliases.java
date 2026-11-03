package footballmarket.features.identity.domain.models;

import footballmarket.features.catalog.domain.exceptions.InvalidPlayerException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** SPEC-008 — Catálogo normativo de alias de equipos; fijo durante todo el TP. */
public final class WhoScoredTeamAliases {
  private final Map<String, String> localByExternal;

  /** Par revisado explícitamente, orientado de nombre externo a local. */
  public record Alias(String local, String external) {}

  public WhoScoredTeamAliases(List<Alias> aliases) {
    Map<String, String> normalized = new HashMap<>();
    for (Alias alias : aliases) {
      String local = WhoScoredIdentityNormalizer.normalize(alias.local());
      String external = WhoScoredIdentityNormalizer.normalize(alias.external());
      if (!WhoScoredIdentityNormalizer.validName(local)
          || !WhoScoredIdentityNormalizer.validName(external)) {
        throw new InvalidPlayerException("El catálogo de alias contiene nombres inválidos");
      }
      String previous = normalized.putIfAbsent(external, local);
      if (previous != null && !previous.equals(local)) {
        throw new InvalidPlayerException("El catálogo de alias contiene una colisión inversa");
      }
    }
    this.localByExternal = Map.copyOf(normalized);
  }

  public boolean matches(String localName, String externalName) {
    String local = WhoScoredIdentityNormalizer.normalize(localName);
    String external = WhoScoredIdentityNormalizer.normalize(externalName);
    return !local.isEmpty()
        && (local.equals(external) || local.equals(this.localByExternal.get(external)));
  }

  public static WhoScoredTeamAliases normative() {
    return new WhoScoredTeamAliases(
        List.of(
            new Alias("Arsenal FC", "Arsenal"), new Alias("Aston Villa FC", "Aston Villa"),
            new Alias("Chelsea FC", "Chelsea"), new Alias("Everton FC", "Everton"),
            new Alias("Fulham FC", "Fulham"), new Alias("Liverpool FC", "Liverpool"),
            new Alias("Manchester City FC", "Man City"),
                new Alias("Manchester United FC", "Man Utd"),
            new Alias("Newcastle United FC", "Newcastle"),
                new Alias("Sunderland AFC", "Sunderland"),
            new Alias("Tottenham Hotspur FC", "Tottenham"), new Alias("Hull City AFC", "Hull"),
            new Alias("Leeds United FC", "Leeds"), new Alias("Ipswich Town FC", "Ipswich"),
            new Alias("Nottingham Forest FC", "Nottingham Forest"),
                new Alias("Crystal Palace FC", "Crystal Palace"),
            new Alias("Brighton & Hove Albion FC", "Brighton"),
                new Alias("Brentford FC", "Brentford"),
            new Alias("AFC Bournemouth", "Bournemouth"), new Alias("Coventry City FC", "Coventry"),
            new Alias("1. FC Köln", "FC Koln"), new Alias("TSG 1899 Hoffenheim", "Hoffenheim"),
            new Alias("Bayer 04 Leverkusen", "Leverkusen"),
                new Alias("Borussia Dortmund", "Borussia Dortmund"),
            new Alias("FC Bayern München", "Bayern"), new Alias("FC Schalke 04", "Schalke"),
            new Alias("Hamburger SV", "Hamburg"), new Alias("VfB Stuttgart", "Stuttgart"),
            new Alias("SV Werder Bremen", "Werder Bremen"), new Alias("1. FSV Mainz 05", "Mainz"),
            new Alias("FC Augsburg", "Augsburg"), new Alias("SC Freiburg", "Freiburg"),
            new Alias("Borussia Mönchengladbach", "Borussia M.Gladbach"),
                new Alias("Eintracht Frankfurt", "Eintracht Frankfurt"),
            new Alias("1. FC Union Berlin", "Union Berlin"),
                new Alias("SC Paderborn 07", "Paderborn"),
            new Alias("SV 07 Elversberg", "Elversberg"), new Alias("RB Leipzig", "RBL"),
            new Alias("Athletic Club", "Athletic Club"),
                new Alias("Club Atlético de Madrid", "Atletico"),
            new Alias("CA Osasuna", "Osasuna"), new Alias("RCD Espanyol de Barcelona", "Espanyol"),
            new Alias("FC Barcelona", "Barcelona"), new Alias("Getafe CF", "Getafe"),
            new Alias("Málaga CF", "Malaga"), new Alias("Real Madrid CF", "Real Madrid"),
            new Alias("Rayo Vallecano de Madrid", "Rayo Vallecano"),
                new Alias("Levante UD", "Levante"),
            new Alias("Real Betis Balompié", "Real Betis"),
                new Alias("Real Sociedad de Fútbol", "Real Sociedad"),
            new Alias("Villarreal CF", "Villarreal"), new Alias("Valencia CF", "Valencia"),
            new Alias("Deportivo Alavés", "Deportivo Alaves"), new Alias("Elche CF", "Elche"),
            new Alias("RC Celta de Vigo", "Celta Vigo"), new Alias("Sevilla FC", "Sevilla"),
            new Alias("RC Deportivo La Coruña", "Deportivo"),
                new Alias("Real Racing Club de Santander", "Racing Santander"),
            new Alias("AC Milan", "AC Milan"), new Alias("ACF Fiorentina", "Fiorentina"),
            new Alias("AS Roma", "Roma"), new Alias("Atalanta BC", "Atalanta"),
            new Alias("Bologna FC 1909", "Bologna"), new Alias("Cagliari Calcio", "Cagliari"),
            new Alias("Genoa CFC", "Genoa"), new Alias("FC Internazionale Milano", "Inter"),
            new Alias("Juventus FC", "Juventus"), new Alias("SS Lazio", "Lazio"),
            new Alias("Parma Calcio 1913", "Parma Calcio 1913"), new Alias("SSC Napoli", "Napoli"),
            new Alias("Udinese Calcio", "Udinese"), new Alias("Venezia FC", "Venezia"),
            new Alias("Frosinone Calcio", "Frosinone"), new Alias("US Sassuolo Calcio", "Sassuolo"),
            new Alias("Torino FC", "Torino"), new Alias("US Lecce", "Lecce"),
            new Alias("AC Monza", "Monza"), new Alias("Como 1907", "Como"),
            new Alias("Toulouse FC", "Toulouse"), new Alias("Stade Brestois 29", "Brest"),
            new Alias("Olympique de Marseille", "Marseille"), new Alias("AJ Auxerre", "Auxerre"),
            new Alias("Lille OSC", "Lille"), new Alias("OGC Nice", "Nice"),
            new Alias("Olympique Lyonnais", "Lyon"), new Alias("Paris Saint-Germain FC", "PSG"),
            new Alias("FC Lorient", "Lorient"), new Alias("Stade Rennais FC 1901", "Rennes"),
            new Alias("ES Troyes AC", "Troyes"), new Alias("Angers SCO", "Angers"),
            new Alias("Le Havre AC", "Le Havre"), new Alias("Le Mans FC", "Le Mans"),
            new Alias("Racing Club de Lens", "Lens"), new Alias("AS Monaco FC", "Monaco"),
            new Alias("RC Strasbourg Alsace", "Strasbourg"), new Alias("Paris FC", "Paris FC")));
  }
}
