package footballmarket.services.internal;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Métricas de un jugador en un contexto competitivo, listas para una fila ancha.
 *
 * @param competitionId competencia equivalente en el proveedor
 * @param teamId equipo en el proveedor, o null si no se publicó
 * @param seasonKey clave de temporada del proveedor
 * @param seasonLabel etiqueta legible de la temporada
 * @param metrics métricas efectivamente publicadas, indexadas por clave canónica del catálogo
 */
public record PlayerMetricsContext(
    String competitionId,
    String teamId,
    String seasonKey,
    String seasonLabel,
    Map<String, MetricResult> metrics) {

  public PlayerMetricsContext {
    metrics = Map.copyOf(metrics);
  }

  /** Valor publicado y precisión decimal observada; la ausencia no se representa aquí. */
  public record MetricResult(BigDecimal value, Integer precision) {
    public MetricResult {
      if (value == null) {
        throw new IllegalArgumentException("Una métrica publicada requiere valor");
      }
    }
  }

  /** Valor ensamblado con su regla aplicada, antes de decidir si se persiste. */
  public record MetricValue(
      String key,
      BigDecimal value,
      footballmarket.models.enums.MetricAvailability availability,
      footballmarket.models.enums.DerivationRule derivation,
      Integer observedPrecision,
      java.time.Instant observedAt) {}
}
