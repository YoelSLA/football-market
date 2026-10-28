package footballmarket.services.internal;

import footballmarket.integrations.records.*;
import footballmarket.models.*;
import footballmarket.models.enums.*;
import footballmarket.models.exceptions.InvalidPlayerException;
import footballmarket.services.internal.PlayerMetricsContext.MetricValue;
import java.math.BigDecimal;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

/**
 * Traduce las filas externas y aplica exclusivamente las reglas numéricas de Model.
 *
 * <p>El resumen inicial aporta las nueve métricas de contexto y de totales de temporada, y cada
 * subcategoría de detalle aporta las suyas por división de apariciones. Las siete métricas que solo
 * publicaba la pestaña defensiva se obtienen por la interfaz JSON y no forman parte del recorrido.
 */
@Component
public class ScrapingMetricAssembler {
  private static final org.slf4j.Logger LOG =
      org.slf4j.LoggerFactory.getLogger(ScrapingMetricAssembler.class);

  /** Contador de apariciones, con el suplente opcional entre paréntesis. */
  private static final Pattern APPEARANCES =
      Pattern.compile("^([0-9]+)\\s*(?:\\(\\s*[0-9]*\\s*\\))?$");

  /**
   * Columnas que el proveedor publica en las tablas y que no pertenecen al catálogo cerrado.
   *
   * <p>Se diagnostican sin crear métricas: son acumulados, identificadores o indicadores de otro
   * ámbito que el recorrido no necesita.
   */
  private static final Set<String> NOT_CATALOGED =
      Set.of(
          "app",
          "rating",
          "manofthematch",
          "duelaerialwon",
          "tackletotalwon",
          "tackletotalsuccess",
          "challengelost",
          "tacklewontotal",
          "shotstotal",
          "goaltotal",
          "passtotal",
          "passlongballaccurate",
          "dribblewon",
          "keypassestotal",
          "assist",
          "foulgiven",
          "interceptionall",
          "offsidegiven",
          "clearancetotal",
          "turnover",
          "dispossessed",
          "wasfouled",
          "outfielderblock");

  /** Detecta columnas ajenas sin conservar texto externo en logs ni crear definiciones. */
  public boolean hasUnknownColumns(WhoScoredScrapedPlayer player) {
    for (WhoScoredScrapedContextRow row : player.rows()) {
      for (WhoScoredScrapedMetricCell cell : row.cells()) {
        if (!NOT_CATALOGED.contains(cell.providerField().toLowerCase(Locale.ROOT))) {
          try {
            MetricCatalog.canonicalKey(cell.providerField());
          } catch (InvalidPlayerException unknown) {
            return true;
          }
        }
      }
    }
    return false;
  }

  private static final Set<String> INITIAL =
      Set.of(
          "minsplayed",
          "apps",
          "goal",
          "assisttotal",
          "shotspergame",
          "passsuccess",
          "aerialwonpergame",
          "yellowcard",
          "redcard");

  public List<PlayerMetricsContext> assemble(WhoScoredScrapedPlayer scraped) {
    return this.assemble(scraped, null, null);
  }

  /** Correlaciona el rechazo de una celda con el intento local sin exponer identidad externa. */
  public List<PlayerMetricsContext> assemble(
      WhoScoredScrapedPlayer scraped, Long runId, Long playerId) {
    try {
      return this.assembleValues(scraped, "run=" + runId + " player=" + playerId);
    } catch (InvalidPlayerException invalid) {
      LOG.warn("Ensamblado inconsistente: {}", invalid.getMessage());
      throw new footballmarket.integrations.exceptions.WhoScoredStatisticsException(
          "PROVIDER_DATA_INCONSISTENT", null);
    }
  }

  private List<PlayerMetricsContext> assembleValues(
      WhoScoredScrapedPlayer scraped, String diagnostic) {
    Map<String, List<WhoScoredScrapedContextRow>> groups = new LinkedHashMap<>();
    for (WhoScoredScrapedContextRow row : scraped.rows()) {
      groups
          .computeIfAbsent(
              row.competitionId() + ":" + row.seasonKey(), ignored -> new ArrayList<>())
          .add(row);
    }
    List<PlayerMetricsContext> result = new ArrayList<>();
    for (List<WhoScoredScrapedContextRow> rows : groups.values()) {
      WhoScoredScrapedContextRow context = rows.getFirst();
      Map<String, MetricValue> metrics = new LinkedHashMap<>();
      for (MetricDefinition definition : MetricCatalog.definitions()) {
        if (definition.sources().contains(MetricSource.SCRAPING)) {
          metrics.put(
              definition.key(),
              new MetricValue(
                  definition.key(),
                  null,
                  MetricAvailability.ABSENT,
                  definition.derivation(),
                  null,
                  context.observedAt()));
        }
      }
      for (WhoScoredScrapedContextRow row : rows) {
        Map<String, String> fields = new HashMap<>();
        row.cells()
            .forEach(
                cell -> fields.put(cell.providerField().toLowerCase(Locale.ROOT), cell.text()));
        BigDecimal apps = appearances(fields.get("apps"));
        for (WhoScoredScrapedMetricCell cell : row.cells()) {
          String field = cell.providerField().toLowerCase(Locale.ROOT);
          if (row.section().equals("initial") && !INITIAL.contains(field)) {
            continue;
          }
          String key;
          try {
            key = MetricCatalog.canonicalKey(field);
          } catch (InvalidPlayerException unknown) {
            continue;
          }
          MetricDefinition definition = MetricCatalog.require(key);
          boolean derived = definition.derivation() == DerivationRule.DIVIDE_BY_APPEARANCES;
          if (!row.section().equals("initial") && !derived) {
            continue;
          }
          String published = field.equals("apps") ? appearancesText(cell.text()) : cell.text();
          MetricValueNormalizer.NormalizedMetricValue normalized =
              diagnosticValue(published, row, field, diagnostic);
          MetricValue value;
          if (derived) {
            MetricDerivationApplier.DerivedMetricValue quotient =
                MetricDerivationApplier.perAppearance(normalized.value(), apps);
            value =
                new MetricValue(
                    key,
                    quotient.value(),
                    quotient.availability(),
                    DerivationRule.DIVIDE_BY_APPEARANCES,
                    quotient.observedPrecision(),
                    row.observedAt());
          } else {
            value =
                new MetricValue(
                    key,
                    normalized.value(),
                    normalized.availability(),
                    DerivationRule.NONE,
                    normalized.observedPrecision(),
                    row.observedAt());
          }
          MetricValue previous = metrics.get(key);
          if (previous != null
              && (value.availability() == MetricAvailability.PRESENT
                  || previous.availability() != MetricAvailability.PRESENT)) {
            metrics.put(key, value);
          }
        }
        if (row.section().equals("initial")) {
          this.deriveSummary(metrics, fields, apps, row, "goal", "goals_per_match");
          this.deriveSummary(metrics, fields, apps, row, "assisttotal", "assists_per_match");
        }
      }
      WhoScoredStructuredDataBlock.Entry block = structuredEntry(scraped, context);
      if (block != null) {
        this.structured(metrics, "whoscored_rating_average", block.rating(), context);
        this.structured(metrics, "shots_on_target_total", block.shotsOnTarget(), context);
        this.structured(metrics, "second_yellow_total", block.secondYellow(), context);
        this.structured(metrics, "starts_total", block.gameStarted(), context);
        this.structured(metrics, "substitute_appearances_total", block.subOn(), context);
      }
      Map<String, PlayerMetricsContext.MetricResult> published = new LinkedHashMap<>();
      metrics.forEach(
          (key, metric) -> {
            if (metric.availability() == MetricAvailability.PRESENT && metric.value() != null) {
              published.put(
                  key,
                  new PlayerMetricsContext.MetricResult(
                      metric.value(), metric.observedPrecision()));
            }
          });
      result.add(
          new PlayerMetricsContext(
              context.competitionId(),
              context.teamId(),
              context.seasonKey(),
              context.seasonLabel(),
              published));
    }
    return List.copyOf(result);
  }

  /**
   * Bloque estructurado del contexto, o nulo cuando el proveedor no lo publica.
   *
   * <p>Un jugador que cambia de equipo dentro de la misma temporada aparece con una entrada por
   * equipo. Cuando la fila declara su equipo se toma esa entrada; si no lo declara y hay varias, se
   * toma la primera y la ambigüedad queda registrada.
   */
  private static WhoScoredStructuredDataBlock.Entry structuredEntry(
      WhoScoredScrapedPlayer scraped, WhoScoredScrapedContextRow context) {
    List<WhoScoredStructuredDataBlock.Entry> matches =
        scraped.structured().entries().stream()
            .filter(
                entry ->
                    context.competitionId().equals(entry.competitionId())
                        && context.seasonKey().equals(entry.seasonId()))
            .toList();
    if (matches.isEmpty()) {
      return null;
    }
    WhoScoredStructuredDataBlock.Entry selected = matches.getFirst();
    if (context.teamId() != null) {
      for (WhoScoredStructuredDataBlock.Entry entry : matches) {
        if (context.teamId().equals(entry.teamId())) {
          return entry;
        }
      }
    }
    if (matches.size() > 1) {
      LOG.warn(
          "Varios bloques estructurados para el mismo contexto: competition={} season={} entries={}",
          context.competitionId(),
          context.seasonKey(),
          matches.size());
    }
    return selected;
  }

  /**
   * Valor publicado en una tabla del proveedor.
   *
   * <p>El guion y el vacío son ausencias, nunca ceros inferidos (FR-022/FR-031).
   */
  private static MetricValueNormalizer.NormalizedMetricValue publishedValue(String raw) {
    return MetricValueNormalizer.normalize(raw);
  }

  /** Solo permite tokens cortos de aspecto numérico; el resto se informa sin texto externo. */
  private static MetricValueNormalizer.NormalizedMetricValue diagnosticValue(
      String raw, WhoScoredScrapedContextRow row, String field, String diagnostic) {
    try {
      return publishedValue(raw);
    } catch (InvalidPlayerException invalid) {
      String token =
          raw != null
                  && raw.length() <= 48
                  && (raw.matches("[0-9\\s\\p{Z}.,%()+/\\-−–—]*")
                      || Set.of("N/A", "n/a", "NaN", "null").contains(raw))
              ? raw.codePoints()
                  .mapToObj(code -> String.format("U+%04X", code))
                  .collect(java.util.stream.Collectors.joining(" "))
              : "[redacted]";
      LOG.warn(
          "{} worker={} evento=invalid-decimal section={} field={} competition={} season={} token={} length={}",
          diagnostic,
          Thread.currentThread().getName(),
          row.section(),
          field,
          row.competitionId(),
          row.seasonKey(),
          token,
          raw == null ? 0 : raw.length());
      throw invalid;
    }
  }

  /**
   * Apariciones publicadas en la celda de apariciones.
   *
   * <p>El proveedor puede publicarla como {@code 6(1)}, donde el paréntesis cuenta las apariciones
   * como suplente. Solo la parte anterior al paréntesis son apariciones, y es también el
   * denominador de las métricas derivadas; el suplente no se persiste desde aquí porque la métrica
   * de suplentes se obtiene del bloque estructurado. Un valor que no sea un contador sigue siendo
   * un dato incoherente.
   */
  private static String appearancesText(String published) {
    String value = published == null ? null : published.strip();
    if (value == null || value.isEmpty() || value.equals("-")) {
      return value;
    }
    Matcher counter = APPEARANCES.matcher(value);
    if (!counter.matches()) {
      throw new InvalidPlayerException("El valor de apariciones no es un contador válido");
    }
    return counter.group(1);
  }

  private static BigDecimal appearances(String published) {
    return publishedValue(appearancesText(published)).value();
  }

  private void deriveSummary(
      Map<String, MetricValue> metrics,
      Map<String, String> fields,
      BigDecimal apps,
      WhoScoredScrapedContextRow row,
      String field,
      String key) {
    MetricDerivationApplier.DerivedMetricValue value =
        MetricDerivationApplier.perAppearance(publishedValue(fields.get(field)).value(), apps);
    metrics.put(
        key,
        new MetricValue(
            key,
            value.value(),
            value.availability(),
            DerivationRule.DIVIDE_BY_APPEARANCES,
            value.observedPrecision(),
            row.observedAt()));
  }

  private void structured(
      Map<String, MetricValue> metrics,
      String key,
      BigDecimal value,
      WhoScoredScrapedContextRow row) {
    metrics.put(
        key,
        new MetricValue(
            key,
            value,
            value == null ? MetricAvailability.ABSENT : MetricAvailability.PRESENT,
            DerivationRule.NONE,
            value == null ? null : Math.max(0, value.scale()),
            row.observedAt()));
  }
}
