package footballmarket.integrations;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import footballmarket.integrations.exceptions.WhoScoredStatisticsException;
import footballmarket.integrations.exceptions.WhoScoredStatisticsStructureChangedException;
import footballmarket.integrations.records.*;
import java.time.Instant;
import java.util.*;
import java.util.regex.*;
import org.springframework.stereotype.Component;

/**
 * Interpreta columnas técnicas y enlaces del DOM; nunca resuelve identidades por nombres.
 *
 * <p>La fila declara su competición y su equipo en los enlaces de la propia fila, pero no declara
 * su temporada: no existe ningún enlace a la temporada dentro de la fila. La temporada es la
 * identidad real del contexto y solo consta en el bloque de datos estructurado del HTML original,
 * indexado por competición y equipo. Los enlaces {@code /tournaments/{id}/seasons/{seasonId}} que
 * hay en la página pertenecen a otros widgets del documento —partidos recientes y desplegables con
 * más de doscientas competiciones— y no describen la fila, por lo que no se usan para deducir la
 * temporada.
 *
 * <p>La etiqueta de temporada y los años de participación quedan ausentes porque el proveedor no
 * los publica junto al resto de datos del contexto: su ausencia es un dato ausente y no invalida la
 * fila.
 */
@Component
public class WhoScoredScrapingTableParser {
  private static final Pattern COMPETITION = Pattern.compile("/tournaments/([0-9]+)(?:/|$)");
  private static final Pattern TEAM = Pattern.compile("/teams/([0-9]+)(?:/|$)");
  private static final org.slf4j.Logger LOG =
      org.slf4j.LoggerFactory.getLogger(WhoScoredScrapingTableParser.class);

  public List<WhoScoredScrapedContextRow> parse(
      Page page,
      String container,
      String section,
      WhoScoredStructuredDataBlock structured,
      Instant observedAt) {
    Locator table = page.locator(container + " table");
    if (table.count() != 1) {
      throw new WhoScoredStatisticsStructureChangedException();
    }
    Locator headers = table.locator("thead th");
    if (headers.count() < 2) {
      throw new WhoScoredStatisticsStructureChangedException();
    }
    List<WhoScoredScrapedContextRow> result = new ArrayList<>();
    for (Locator row : table.locator("tbody tr").all()) {
      Locator cells = row.locator(":scope > td");
      if (cells.count() == 1 && cells.first().getAttribute("colspan") != null) {
        continue;
      }
      if (cells.count() != headers.count()) {
        throw new WhoScoredStatisticsStructureChangedException();
      }
      String competition = optionalId(row, "competition");
      if (competition == null) {
        // La última fila de cada tabla es la de totales y promedios: no declara competición ni
        // temporada y no es un contexto del jugador. No tener enlace no invalida el resto de la
        // tabla; si la fila perdiera su contexto, el jugador se confirmaría sin contexto resuelto.
        LOG.info(
            "Fila sin contexto omitida en {}: {}",
            section,
            summary(row).orElse("sin texto legible"));
        continue;
      }
      if (!currentSeason(structured, competition)) {
        // El resumen publica también temporadas y Coppas anteriores. El bloque estructurado solo
        // declara la temporada vigente, así que una fila sin entrada no es un fallo del recorrido:
        // no aporta datos de la campaña en curso y la selección de liga descarta igualmente el
        // resto de competiciones.
        LOG.info(
            "Fila fuera de la temporada vigente omitida en {}: competition={}",
            section,
            competition);
        continue;
      }
      String team = optionalId(row, "team");
      String season = seasonOf(structured, section, competition, team);
      List<WhoScoredScrapedMetricCell> metrics = new ArrayList<>();
      for (int index = 1; index < headers.count(); index++) {
        String field = headers.nth(index).getAttribute("data-stat-name");
        if (field == null || field.isBlank()) {
          throw new WhoScoredStatisticsStructureChangedException();
        }
        metrics.add(new WhoScoredScrapedMetricCell(field, cells.nth(index).innerText()));
      }
      result.add(
          new WhoScoredScrapedContextRow(
              section, competition, team, season, null, null, metrics, observedAt));
    }
    return List.copyOf(result);
  }

  /** El bloque estructurado declara la temporada vigente de cada competición con actividad. */
  private static boolean currentSeason(
      WhoScoredStructuredDataBlock structured, String competition) {
    for (WhoScoredStructuredDataBlock.Entry entry : structured.entries()) {
      if (competition.equals(entry.competitionId()) && entry.seasonId() != null) {
        return true;
      }
    }
    return false;
  }

  /**
   * Temporada real del contexto, leída del bloque estructurado por competición y equipo.
   *
   * <p>Cuando la fila omite el enlace de equipo y la competición acumula más de una temporada, se
   * conserva la de clave mayor, que es la vigente. La ambigüedad queda registrada porque es una
   * decisión del recorrido y no un dato publicado por la fila.
   */
  private static String seasonOf(
      WhoScoredStructuredDataBlock structured, String section, String competition, String team) {
    String candidate = null;
    int matches = 0;
    for (WhoScoredStructuredDataBlock.Entry entry : structured.entries()) {
      if (!competition.equals(entry.competitionId())
          || entry.seasonId() == null
          || (team != null && !team.equals(entry.teamId()))) {
        continue;
      }
      matches++;
      if (candidate == null || seasonRank(entry.seasonId()) > seasonRank(candidate)) {
        candidate = entry.seasonId();
      }
    }
    if (candidate == null) {
      LOG.warn(
          "El bloque estructurado no declara temporada en {}: competition={} team={}",
          section,
          competition,
          team);
      throw new WhoScoredStatisticsException("PROVIDER_DATA_INCONSISTENT", null);
    }
    if (matches > 1) {
      LOG.warn(
          "Varias temporadas candidatas en {}: competition={} team={} candidatas={} vigente={}",
          section,
          competition,
          team,
          matches,
          candidate);
    }
    return candidate;
  }

  private static long seasonRank(String seasonKey) {
    try {
      return Long.parseLong(seasonKey.strip());
    } catch (NumberFormatException notNumeric) {
      return Long.MIN_VALUE;
    }
  }

  /**
   * Identificador de la fila, o nulo cuando la celda no declara ninguno.
   *
   * <p>La fila de totales no declara ninguno y se descarta. Varios identificadores distintos en la
   * misma fila sí son un dato incoherente, porque entonces no se sabe de qué contexto habla.
   */
  private static String optionalId(Locator row, String kind) {
    Pattern pattern = kind.equals("competition") ? COMPETITION : TEAM;
    Set<String> ids = new LinkedHashSet<>();
    for (Locator link : row.locator("a").all()) {
      Matcher matcher = pattern.matcher(String.valueOf(link.getAttribute("href")));
      if (matcher.find()) {
        ids.add(matcher.group(1));
      }
    }
    if (ids.size() > 1) {
      LOG.warn("Identificadores contradictorios en la fila ({}): {}", kind, ids);
      throw new WhoScoredStatisticsException("PROVIDER_DATA_INCONSISTENT", null);
    }
    return ids.isEmpty() ? null : ids.iterator().next();
  }

  /** Texto de la celda que describe la fila, para diagnosticar una fila sin contexto. */
  private static java.util.Optional<String> summary(Locator row) {
    Locator cells = row.locator(":scope > td");
    if (cells.count() == 0) {
      return java.util.Optional.empty();
    }
    String text = cells.first().innerText().strip().replaceAll("\\s+", " ");
    return text.isEmpty() ? java.util.Optional.empty() : java.util.Optional.of(text);
  }
}
