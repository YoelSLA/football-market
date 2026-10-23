package footballmarket.integrations;

import footballmarket.integrations.exceptions.WhoScoredStatisticsException;
import footballmarket.integrations.records.WhoScoredStructuredDataBlock;
import footballmarket.integrations.records.WhoScoredStructuredDataBlock.Entry;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** Lee únicamente el array JSON tournaments, sin ejecutar JavaScript ni usar punto flotante. */
@Component
public class WhoScoredStructuredDataParser {
  private static final org.slf4j.Logger LOG =
      org.slf4j.LoggerFactory.getLogger(WhoScoredStructuredDataParser.class);
  private static final Pattern START = Pattern.compile("\\btournaments\\s*:\\s*(\\[)");
  private final JsonMapper mapper =
      JsonMapper.builder()
          .enable(tools.jackson.databind.DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS)
          .disable(tools.jackson.databind.cfg.JsonNodeFeature.STRIP_TRAILING_BIGDECIMAL_ZEROES)
          .build();

  public WhoScoredStructuredDataBlock parse(String html) {
    if (html == null) {
      return new WhoScoredStructuredDataBlock(List.of());
    }
    Matcher start = START.matcher(html);
    if (!start.find()) {
      return new WhoScoredStructuredDataBlock(List.of());
    }
    int begin = start.start(1);
    int end = findArrayEnd(html, begin);
    if (end < 0) {
      return new WhoScoredStructuredDataBlock(List.of());
    }
    try {
      JsonNode rows = this.mapper.readTree(html.substring(begin, end + 1));
      List<Entry> entries = new ArrayList<>();
      for (JsonNode row : rows) {
        entries.add(
            new Entry(
                text(row, "TournamentId"),
                text(row, "SeasonId"),
                text(row, "TeamId"),
                decimal(row, "Rating"),
                decimal(row, "ShotsOnTarget"),
                decimal(row, "SecondYellow"),
                decimal(row, "GameStarted"),
                decimal(row, "SubOn")));
      }
      return new WhoScoredStructuredDataBlock(entries);
    } catch (tools.jackson.core.JacksonException failure) {
      return new WhoScoredStructuredDataBlock(List.of());
    }
  }

  private static int findArrayEnd(String html, int begin) {
    int depth = 0;
    boolean quoted = false;
    boolean escaped = false;
    for (int i = begin; i < html.length(); i++) {
      char c = html.charAt(i);
      if (quoted) {
        if (escaped) {
          escaped = false;
        } else if (c == '\\') {
          escaped = true;
        } else if (c == '"') {
          quoted = false;
        }
      } else if (c == '"') {
        quoted = true;
      } else if (c == '[') {
        depth++;
      } else if (c == ']' && --depth == 0) {
        return i;
      }
    }
    return -1;
  }

  private static String text(JsonNode row, String field) {
    JsonNode value = row.get(field);
    return value == null || value.isNull() ? null : value.asText();
  }

  private static BigDecimal decimal(JsonNode row, String field) {
    JsonNode value = row.get(field);
    if (value == null || value.isNull()) {
      return null;
    }
    try {
      return value.isNumber()
          ? value.decimalValue()
          : new BigDecimal(value.asText().replace(',', '.'));
    } catch (NumberFormatException failure) {
      LOG.warn("Valor no numérico en el bloque estructurado del campo {}", field);
      throw new WhoScoredStatisticsException("PROVIDER_DATA_INCONSISTENT", null);
    }
  }
}
