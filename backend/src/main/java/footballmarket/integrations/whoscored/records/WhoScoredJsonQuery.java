package footballmarket.integrations.whoscored.records;

import footballmarket.integrations.whoscored.exceptions.WhoScoredStatisticsStructureChangedException;
import java.util.List;
import java.util.Objects;

/**
 * Consulta JSON de estadísticas de un jugador en una combinación conocida.
 *
 * <p>Las quince combinaciones observadas son la única fuente de verdad del recorrido: ninguna
 * combinación especulativa se admite para intentar obtener métricas no publicadas en JSON.
 *
 * <p>Cinco combinaciones observadas quedaron fuera del recorrido: {@code cards/type}, {@code
 * clearances/success}, {@code interception/success}, {@code offsides/type} y {@code
 * possession-loss/type}. Se comprobó que todas declaran solo columnas que {@code summary/all} ya
 * publica, más columnas ajenas al catálogo cerrado ({@code clearanceTotal}, {@code
 * interceptionAll}, {@code offsideGiven}, {@code dispossessed}, {@code turnover}). Quitarlas no
 * reduce la unión de métricas obtenible y evita una cuarta parte de las peticiones por jugador.
 */
public record WhoScoredJsonQuery(String playerId, String category, String subcategory) {

  /** Única lista de combinaciones conocidas del canal JSON. */
  public static final List<Combination> COMBINATIONS =
      List.of(
          new Combination("summary", "all"),
          new Combination("summary", "defensive"),
          new Combination("summary", "offensive"),
          new Combination("summary", "passing"),
          new Combination("aerial", "success"),
          new Combination("assists", "type"),
          new Combination("blocks", "type"),
          new Combination("dribbles", "success"),
          new Combination("fouls", "type"),
          new Combination("goals", "zones"),
          new Combination("key-passes", "length"),
          new Combination("passes", "length"),
          new Combination("saves", "shotzone"),
          new Combination("shots", "zones"),
          new Combination("tackles", "success"));

  public WhoScoredJsonQuery {
    if (playerId == null || !playerId.matches("[1-9][0-9]*")) {
      throw new WhoScoredStatisticsStructureChangedException();
    }
    if (!isKnown(category, subcategory)) {
      throw new WhoScoredStatisticsStructureChangedException();
    }
  }

  /** Constructor de la combinación observada para un jugador confirmado. */
  public static WhoScoredJsonQuery of(String playerId, Combination combination) {
    return new WhoScoredJsonQuery(playerId, combination.category(), combination.subcategory());
  }

  /** Etiqueta de sección admitida por el registro de incidencias. */
  public String section() {
    return this.category + "/" + this.subcategory;
  }

  private static boolean isKnown(String category, String subcategory) {
    if (category == null || subcategory == null) {
      return false;
    }
    for (Combination combination : COMBINATIONS) {
      if (combination.category().equals(category)
          && combination.subcategory().equals(subcategory)) {
        return true;
      }
    }
    return false;
  }

  /** Combinación conocida de categoría y subcategoría. */
  public record Combination(String category, String subcategory) {
    public Combination {
      Objects.requireNonNull(category);
      Objects.requireNonNull(subcategory);
    }

    /** Etiqueta de sección admitida por el registro de incidencias. */
    public String section() {
      return this.category + "/" + this.subcategory;
    }
  }
}
