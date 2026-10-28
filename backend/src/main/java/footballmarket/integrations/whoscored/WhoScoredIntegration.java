package footballmarket.integrations.whoscored;

import footballmarket.integrations.whoscored.config.WhoScoredProperties;
import footballmarket.integrations.whoscored.exceptions.WhoScoredIntegrationException;
import footballmarket.integrations.whoscored.records.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/** Adaptador del catálogo paginado; acepta JSON válido independientemente del MIME. */
@Component
public class WhoScoredIntegration {
  private final RestClient client;
  private final WhoScoredRequestPacer pacer;
  private final WhoScoredProperties properties;
  private final ObjectMapper mapper;

  public WhoScoredIntegration(
      @Qualifier("whoScoredRestClient") RestClient client,
      WhoScoredRequestPacer pacer,
      WhoScoredProperties properties,
      ObjectMapper mapper) {
    this.client = client;
    this.pacer = pacer;
    this.properties = properties;
    this.mapper = mapper;
  }

  public WhoScoredCatalogPage currentPage(int tournamentId, int page) {
    return this.catalogPage(tournamentId, page, true);
  }

  /**
   * Misma validación y ritmo del actual, conservando contextos históricos y temporadas opcionales.
   */
  public WhoScoredCatalogPage historicalPage(int tournamentId, int page) {
    return this.catalogPage(tournamentId, page, false);
  }

  private WhoScoredCatalogPage catalogPage(int tournamentId, int page, boolean current) {
    if (page < 1 || page > this.properties.maxPagesPerLeague()) {
      throw failure("PROVIDER_LIMIT_EXCEEDED", null).withContext(tournamentId, page);
    }
    try {
      return this.pacer.execute(() -> this.request(tournamentId, page, current));
    } catch (WhoScoredIntegrationException failure) {
      throw failure.withContext(tournamentId, page);
    }
  }

  private WhoScoredCatalogPage request(int tournamentId, int page, boolean current) {
    try {
      return this.client
          .get()
          .uri(
              builder ->
                  builder
                      .path("/statisticsfeed/1/getplayerstatistics")
                      .queryParam("category", "summary")
                      .queryParam("subcategory", "all")
                      .queryParam("statsAccumulationType", 0)
                      .queryParam("isCurrent", current)
                      .queryParam("playerId", "")
                      .queryParam("includeZeroValues", true)
                      .queryParam("field", "Overall")
                      .queryParam("sortBy", "Rating")
                      .queryParam("tournamentOptions", tournamentId)
                      .queryParam("numberOfPlayersToPick", this.properties.pageSize())
                      .queryParam("page", page)
                      .build())
          .exchange(
              (request, response) -> {
                int status = response.getStatusCode().value();
                if (status == 429) {
                  this.pacer.defer(response.getHeaders().getFirst("Retry-After"));
                }
                if (status < 200 || status >= 300) {
                  throw failure(
                      status == 403
                          ? "PROVIDER_BLOCKED"
                          : status == 429 ? "PROVIDER_RATE_LIMITED" : "PROVIDER_HTTP_ERROR",
                      status);
                }
                byte[] bytes = response.getBody().readNBytes(this.properties.maxResponseBytes());
                if (response.getBody().read() != -1) {
                  throw failure("PROVIDER_LIMIT_EXCEEDED", status);
                }
                return this.parse(bytes, tournamentId, page);
              });
    } catch (WhoScoredIntegrationException known) {
      throw known;
    } catch (RuntimeException failure) {
      if (Thread.currentThread().isInterrupted()) {
        throw new WhoScoredIntegrationException("EXECUTION_INTERRUPTED", null, true);
      }
      throw failure("PROVIDER_UNAVAILABLE", null);
    }
  }

  private WhoScoredCatalogPage parse(byte[] bytes, int tournamentId, int page) {
    try {
      JsonNode root =
          this.mapper
              .reader()
              .with(tools.jackson.databind.DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
              .readTree(bytes);
      if (root == null
          || !root.isObject()
          || !root.path("playerTableStats").isArray()
          || !root.path("paging").isObject()) {
        throw failure("PROVIDER_INVALID_RESPONSE", 200);
      }
      JsonNode rows = root.path("playerTableStats");
      JsonNode paging = root.path("paging");
      long current = integer(paging, "currentPage");
      long totalPages = integer(paging, "totalPages");
      long size = integer(paging, "resultsPerPage");
      long total = integer(paging, "totalResults");
      long first = integer(paging, "firstRecordIndex");
      long last = integer(paging, "lastRecordIndex");
      if (current != page
          || size != this.properties.pageSize()
          || total < 0
          || totalPages < 0
          || totalPages > Integer.MAX_VALUE) {
        throw failure("PROVIDER_INVALID_RESPONSE", 200);
      }
      if (total == 0) {
        if (page != 1 || totalPages > 1 || !rows.isEmpty() || first < 0 || first > 1 || last != 0) {
          throw failure("PROVIDER_INVALID_RESPONSE", 200);
        }
      } else if (totalPages != (total - 1) / size + 1
          || current > totalPages
          || first != (current - 1) * size + 1
          || last != Math.min(current * size, total)
          || rows.size() != last - first + 1) {
        throw failure("PROVIDER_INVALID_RESPONSE", 200);
      }
      if (totalPages > this.properties.maxPagesPerLeague()) {
        throw failure("PROVIDER_LIMIT_EXCEEDED", 200);
      }
      List<WhoScoredPlayerIdentity> identities = new ArrayList<>();
      List<WhoScoredDiscardedIdentityRow> discarded = new ArrayList<>();
      List<String> fingerprints = new ArrayList<>();
      int position = 0;
      for (JsonNode row : rows) {
        position++;
        fingerprints.add(identityFingerprint(row));
        if (row.hasNonNull("tournamentId")
            && (!row.path("tournamentId").isIntegralNumber()
                || !row.path("tournamentId").canConvertToInt()
                || row.path("tournamentId").intValue() != tournamentId)) {
          throw failure("PROVIDER_INVALID_RESPONSE", 200);
        }
        String id = id(row.path("playerId"));
        String teamId = id(row.path("teamId"));
        String name = text(row, "name");
        String teamName = text(row, "teamName");
        String reason =
            id == null
                ? "INVALID_EXTERNAL_PLAYER_ID"
                : !validName(name)
                    ? "INVALID_EXTERNAL_PLAYER_NAME"
                    : !validName(teamName)
                        ? "INVALID_EXTERNAL_TEAM_NAME"
                        : row.hasNonNull("teamId") && teamId == null
                            ? "INVALID_EXTERNAL_TEAM_ID"
                            : null;
        if (reason != null) {
          discarded.add(new WhoScoredDiscardedIdentityRow(position, reason));
        } else {
          identities.add(
              new WhoScoredPlayerIdentity(
                  id,
                  name,
                  teamId,
                  teamName,
                  tournamentId,
                  text(row, "tournamentName"),
                  id(row.path("seasonId")),
                  text(row, "seasonName")));
        }
      }
      fingerprints.sort(String::compareTo);
      return new WhoScoredCatalogPage(
          identities,
          discarded,
          new WhoScoredPaging(page, (int) totalPages, (int) size, total, first, last),
          fingerprints.stream()
              .map(WhoScoredIntegration::digest)
              .distinct()
              .sorted()
              .collect(Collectors.joining(",")));
    } catch (WhoScoredIntegrationException known) {
      throw known;
    } catch (RuntimeException failure) {
      throw failure("PROVIDER_INVALID_RESPONSE", 200);
    }
  }

  private static long integer(JsonNode object, String name) {
    JsonNode value = object.path(name);
    if (!value.isIntegralNumber() || !value.canConvertToLong()) {
      throw failure("PROVIDER_INVALID_RESPONSE", 200);
    }
    return value.longValue();
  }

  private static String id(JsonNode value) {
    String raw =
        value.isString()
            ? value.stringValue()
            : value.isIntegralNumber() && value.canConvertToLong()
                ? Long.toString(value.longValue())
                : null;
    try {
      if (raw != null && raw.matches("[0-9]+")) {
        long parsed = Long.parseLong(raw);
        return parsed > 0 ? Long.toString(parsed) : null;
      }
    } catch (NumberFormatException invalid) {
      return null;
    }
    return null;
  }

  private static String text(JsonNode row, String name) {
    JsonNode value = row.path(name);
    return value.isString() ? value.stringValue() : null;
  }

  private static boolean validName(String name) {
    return footballmarket.features.identity.domain.models.WhoScoredIdentityNormalizer.validName(
        name);
  }

  private static String identityFingerprint(JsonNode row) {
    return row.path("playerId")
        + "|"
        + row.path("name")
        + "|"
        + row.path("teamId")
        + "|"
        + row.path("teamName")
        + "|"
        + row.path("tournamentId")
        + "|"
        + row.path("seasonId")
        + "|"
        + row.path("seasonName");
  }

  private static String digest(String value) {
    try {
      return HexFormat.of()
          .formatHex(
              MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException impossible) {
      throw failure("PROVIDER_INVALID_RESPONSE", 200);
    }
  }

  private static WhoScoredIntegrationException failure(String code, Integer status) {
    return new WhoScoredIntegrationException(code, status, false);
  }
}
