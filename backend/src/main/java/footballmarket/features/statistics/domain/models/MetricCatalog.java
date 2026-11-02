package footballmarket.features.statistics.domain.models;

import footballmarket.features.catalog.domain.exceptions.InvalidPlayerException;
import footballmarket.features.statistics.domain.enums.DerivationRule;
import footballmarket.features.statistics.domain.enums.MetricSource;
import footballmarket.features.statistics.domain.enums.MetricUnit;
import footballmarket.features.statistics.domain.records.MetricDefinition;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Catálogo cerrado de las 60 métricas de SPEC-012. */
public final class MetricCatalog {
  private static final Set<String> SCRAPING_ONLY =
      Set.of("shots_on_target_total", "second_yellow_total", "starts_total");

  /**
   * Métricas que solo publica la interfaz JSON.
   *
   * <p>Las ocho primeras salen de los resúmenes ofensivo y de distribución, que el scraping no
   * recorre. Las siete siguientes solo las publica el resumen defensivo del proveedor, que quedó
   * fuera del recorrido de scraping y se obtiene por la interfaz pública.
   */
  private static final Set<String> JSON_ONLY =
      Set.of(
          "key_passes_per_match",
          "successful_dribbles_per_match",
          "offsides_per_match",
          "dispossessed_per_match",
          "bad_controls_per_match",
          "passes_per_match",
          "successful_long_balls_per_match",
          "successful_crosses_per_match",
          "tackles_per_match",
          "interceptions_per_match",
          "clearances_per_match",
          "blocked_shots_per_match",
          "dribbled_past_per_match",
          "offsides_won_per_match",
          "own_goals_total");

  private static final Map<String, MetricDefinition> DEFINITIONS = createDefinitions();
  private static final Map<String, String> ALIASES =
      Map.of(
          "foulspergame", "fouls_committed_per_match",
          "foulcommitted", "fouls_committed_per_match");

  private MetricCatalog() {}

  public static List<MetricDefinition> definitions() {
    return List.copyOf(DEFINITIONS.values());
  }

  public static MetricDefinition require(String key) {
    MetricDefinition definition = DEFINITIONS.get(key);
    if (definition == null) {
      throw new InvalidPlayerException("La métrica no pertenece al catálogo cerrado");
    }
    return definition;
  }

  public static String canonicalKey(String providerField) {
    if (providerField == null || providerField.isBlank()) {
      throw new InvalidPlayerException("La columna externa es obligatoria");
    }
    String normalized = providerField.strip().toLowerCase(java.util.Locale.ROOT);
    String alias = ALIASES.get(normalized);
    if (alias != null) {
      return alias;
    }
    return DEFINITIONS.values().stream()
        .filter(
            definition ->
                definition.providerFields().values().stream()
                    .flatMap(Set::stream)
                    .anyMatch(field -> field.equalsIgnoreCase(providerField.strip())))
        .sorted(
            java.util.Comparator.comparing(
                definition -> definition.derivation() != DerivationRule.NONE))
        .map(MetricDefinition::key)
        .findFirst()
        .orElseThrow(
            () -> new InvalidPlayerException("La columna externa no pertenece al catálogo"));
  }

  private static Map<String, MetricDefinition> createDefinitions() {
    List<Seed> seeds = new ArrayList<>();
    seeds.add(seed("minutes_played_total", "minsPlayed", MetricUnit.SEASON_TOTAL));
    seeds.add(seed("appearances_total", "apps", MetricUnit.SEASON_TOTAL));
    seeds.add(seed("goals_total", "goal", MetricUnit.SEASON_TOTAL));
    seeds.add(seed("assists_total", "assistTotal", MetricUnit.SEASON_TOTAL));
    seeds.add(seed("shots_per_match", "shotsPerGame", MetricUnit.PER_MATCH));
    seeds.add(seed("pass_completion_percentage", "passSuccess", MetricUnit.PERCENTAGE));
    seeds.add(seed("aerial_duels_won_per_match", "aerialWonPerGame", MetricUnit.PER_MATCH));
    seeds.add(seed("yellow_cards_total", "yellowCard", MetricUnit.SEASON_TOTAL));
    seeds.add(seed("red_cards_total", "redCard", MetricUnit.SEASON_TOTAL));
    seeds.add(seed("tackles_per_match", "tacklePerGame", MetricUnit.PER_MATCH));
    seeds.add(seed("interceptions_per_match", "interceptionPerGame", MetricUnit.PER_MATCH));
    seeds.add(seed("clearances_per_match", "clearancePerGame", MetricUnit.PER_MATCH));
    seeds.add(seed("blocked_shots_per_match", "outfielderBlockPerGame", MetricUnit.PER_MATCH));
    seeds.add(seed("dribbled_past_per_match", "wasDribbledPerGame", MetricUnit.PER_MATCH));
    seeds.add(seed("offsides_won_per_match", "offsideWonPerGame", MetricUnit.PER_MATCH));
    seeds.add(seed("own_goals_total", "goalOwn", MetricUnit.SEASON_TOTAL));
    seeds.add(derived("saves_per_match", "saveTotal"));
    seeds.add(derived("six_yard_box_saves_per_match", "saveSixYardBox"));
    seeds.add(derived("penalty_area_saves_per_match", "savePenaltyArea"));
    seeds.add(derived("outside_box_saves_per_match", "saveObox"));
    seeds.add(derived("attempted_tackles_per_match", "tackleTotalAttempted"));
    seeds.add(derived("blocked_crosses_per_match", "passCrossBlockedDefensive"));
    seeds.add(derived("blocked_passes_per_match", "outfielderBlockedPass"));
    seeds.add(derived("failed_dribbles_per_match", "dribbleLost"));
    seeds.add(derived("total_dribbles_per_match", "dribbleTotal"));
    seeds.add(derived("aerial_duels_lost_per_match", "duelAerialLost"));
    seeds.add(derived("total_aerial_duels_per_match", "duelAerialTotal"));
    seeds.add(derived("inaccurate_long_balls_per_match", "passLongBallInaccurate"));
    seeds.add(derived("accurate_short_passes_per_match", "shortPassAccurate"));
    seeds.add(derived("inaccurate_short_passes_per_match", "shortPassInaccurate"));
    seeds.add(derived("long_key_passes_per_match", "keyPassLong"));
    seeds.add(derived("short_key_passes_per_match", "keyPassShort"));
    seeds.add(derived("six_yard_box_shots_per_match", "shotSixYardBox"));
    seeds.add(derived("penalty_area_shots_per_match", "shotPenaltyArea"));
    seeds.add(derived("outside_box_shots_per_match", "shotOboxTotal"));
    seeds.add(derived("six_yard_box_goals_per_match", "goalSixYardBox"));
    seeds.add(derived("penalty_area_goals_per_match", "goalPenaltyArea"));
    seeds.add(derived("outside_box_goals_per_match", "goalObox"));
    seeds.add(derived("cross_assists_per_match", "assistCross"));
    seeds.add(derived("corner_assists_per_match", "assistCorner"));
    seeds.add(derived("through_ball_assists_per_match", "assistThroughball"));
    seeds.add(derived("free_kick_assists_per_match", "assistFreekick"));
    seeds.add(derived("throw_in_assists_per_match", "assistThrowin"));
    seeds.add(derived("other_assists_per_match", "assistOther"));
    seeds.add(derived("fouls_committed_per_match", "foulCommitted"));
    seeds.add(derived("goals_per_match", "goal"));
    seeds.add(derived("assists_per_match", "assistTotal"));
    seeds.add(optional("whoscored_rating_average", "rating", MetricUnit.AVERAGE_RATING));
    seeds.add(optional("shots_on_target_total", "ShotsOnTarget", MetricUnit.SEASON_TOTAL));
    seeds.add(optional("second_yellow_total", "SecondYellow", MetricUnit.SEASON_TOTAL));
    seeds.add(optional("starts_total", "GameStarted", MetricUnit.SEASON_TOTAL));
    seeds.add(optional("substitute_appearances_total", "SubOn", MetricUnit.SEASON_TOTAL));
    seeds.add(json("key_passes_per_match", "keyPassPerGame"));
    seeds.add(json("successful_dribbles_per_match", "dribbleWonPerGame"));
    seeds.add(json("offsides_per_match", "offsideGivenPerGame"));
    seeds.add(json("dispossessed_per_match", "dispossessedPerGame"));
    seeds.add(json("bad_controls_per_match", "turnoverPerGame"));
    seeds.add(json("passes_per_match", "totalPassesPerGame"));
    seeds.add(json("successful_long_balls_per_match", "accurateLongPassPerGame"));
    seeds.add(json("successful_crosses_per_match", "accurateCrossesPerGame"));

    Map<String, MetricDefinition> definitions = new LinkedHashMap<>();
    for (Seed seed : seeds) {
      Set<MetricSource> sources =
          JSON_ONLY.contains(seed.key())
              ? Set.of(MetricSource.JSON)
              : SCRAPING_ONLY.contains(seed.key())
                  ? Set.of(MetricSource.SCRAPING)
                  : Set.of(MetricSource.SCRAPING, MetricSource.JSON);
      Map<MetricSource, Set<String>> fields = new LinkedHashMap<>();
      for (MetricSource source : sources) {
        fields.put(source, Set.of(seed.field()));
      }
      MetricDefinition definition =
          new MetricDefinition(
              seed.key(), seed.unit(), sources, fields, seed.optional(), seed.derivation());
      if (definitions.put(seed.key(), definition) != null) {
        throw new InvalidPlayerException("El catálogo contiene claves duplicadas");
      }
    }
    long scraping = count(definitions, MetricSource.SCRAPING);
    long json = count(definitions, MetricSource.JSON);
    long shared =
        definitions.values().stream()
            .filter(definition -> definition.sources().size() == 2)
            .count();
    if (definitions.size() != 60 || scraping != 45 || json != 57 || shared != 42) {
      throw new InvalidPlayerException("La cobertura del catálogo es inconsistente");
    }
    return Map.copyOf(definitions);
  }

  private static long count(Map<String, MetricDefinition> definitions, MetricSource source) {
    return definitions.values().stream()
        .filter(definition -> definition.sources().contains(source))
        .count();
  }

  private static Seed seed(String key, String field, MetricUnit unit) {
    return new Seed(key, field, unit, false, DerivationRule.NONE);
  }

  private static Seed optional(String key, String field, MetricUnit unit) {
    return new Seed(key, field, unit, true, DerivationRule.NONE);
  }

  private static Seed derived(String key, String field) {
    return new Seed(key, field, MetricUnit.PER_MATCH, false, DerivationRule.DIVIDE_BY_APPEARANCES);
  }

  private static Seed json(String key, String field) {
    return new Seed(key, field, MetricUnit.PER_MATCH, false, DerivationRule.NONE);
  }

  private record Seed(
      String key, String field, MetricUnit unit, boolean optional, DerivationRule derivation) {}
}
