package footballmarket.support;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.Map;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.json.JsonMapper;

/** Proveedor HTTP de testing: serializa exclusivamente contratos externos, nunca DTO propios. */
public final class IdentityControlledProvider implements AutoCloseable {
  private final HttpServer server;
  private final JsonMapper mapper = JsonMapper.builder().build();
  private volatile boolean historicalScenario;
  private final java.util.concurrent.atomic.AtomicInteger historicalRequests =
      new java.util.concurrent.atomic.AtomicInteger();
  private static final List<String> CODES = List.of("PL", "BL1", "PD", "SA", "FL1");

  public IdentityControlledProvider() throws IOException {
    server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    server.createContext(
        "/",
        exchange -> {
          String path = exchange.getRequestURI().getPath();
          Object response;
          if (path.startsWith("/competitions/")) {
            String code = path.split("/")[2];
            int index = CODES.indexOf(code) + 1;
            response =
                path.endsWith("/teams")
                    ? Map.of("count", 1, "teams", List.of(Map.of("id", 200 + index)))
                    : Map.of("id", 100 + index, "name", "League " + index);
          } else if (path.startsWith("/teams/")) {
            int index = Integer.parseInt(path.substring("/teams/".length())) - 200;
            response =
                Map.of(
                    "name",
                    "Team " + index,
                    "squad",
                    List.of(
                        Map.of(
                            "id", 300 + index, "name", "Player " + index, "position", "Forward")));
          } else if (path.contains("getplayerstatistics")) {
            Map<String, String> query =
                java.util.Arrays.stream(exchange.getRequestURI().getQuery().split("&"))
                    .map(part -> part.split("=", 2))
                    .collect(
                        java.util.stream.Collectors.toMap(
                            parts -> parts[0], parts -> parts.length == 2 ? parts[1] : ""));
            int tournament = Integer.parseInt(query.get("tournamentOptions"));
            int index = List.of(2, 3, 4, 5, 22).indexOf(tournament) + 1;
            boolean historical = "false".equals(query.get("isCurrent"));
            if (historical) {
              historicalRequests.incrementAndGet();
            }
            boolean empty = historicalScenario && !historical && index == 1;
            response =
                Map.of(
                    "playerTableStats",
                    empty
                        ? List.of()
                        : List.of(
                            Map.of(
                                "playerId",
                                1000 + index,
                                "name",
                                "Player " + index,
                                "teamName",
                                "Team " + index,
                                "tournamentId",
                                tournament)),
                    "paging",
                    Map.of(
                        "currentPage",
                        1,
                        "totalPages",
                        1,
                        "resultsPerPage",
                        3,
                        "totalResults",
                        empty ? 0 : 1,
                        "firstRecordIndex",
                        empty ? 0 : 1,
                        "lastRecordIndex",
                        empty ? 0 : 1));
          } else {
            response = Map.of("teams", List.of());
          }
          byte[] bytes = mapper.writeValueAsBytes(response);
          exchange.getResponseHeaders().set("Content-Type", "application/json");
          exchange.sendResponseHeaders(200, bytes.length);
          exchange.getResponseBody().write(bytes);
          exchange.close();
        });
    server.start();
  }

  public RestClient client() {
    return RestClient.builder()
        .baseUrl("http://127.0.0.1:" + server.getAddress().getPort())
        .defaultHeader("User-Agent", "identity-e2e-test")
        .build();
  }

  public void historicalScenario(boolean enabled) {
    historicalScenario = enabled;
    historicalRequests.set(0);
  }

  public int historicalRequests() {
    return historicalRequests.get();
  }

  @Override
  public void close() {
    server.stop(0);
  }
}
