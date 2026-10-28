package footballmarket.support;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import org.springframework.web.client.RestClient;

/** Proveedor JSON de loopback con respuestas y errores explícitos, sin Internet. */
public final class WhoScoredStatisticsProviderStub implements AutoCloseable {
  private final HttpServer server;
  private final ConcurrentLinkedQueue<Reply> replies = new ConcurrentLinkedQueue<>();
  private final List<URI> requests = new CopyOnWriteArrayList<>();

  public WhoScoredStatisticsProviderStub() throws IOException {
    server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    server.createContext(
        "/",
        exchange -> {
          requests.add(exchange.getRequestURI());
          Reply reply = replies.poll();
          if (reply == null) {
            reply = new Reply(500, "application/json", "{}", null);
          }
          byte[] bytes = reply.body().getBytes(StandardCharsets.UTF_8);
          exchange.getResponseHeaders().set("Content-Type", reply.contentType());
          if (reply.retryAfter() != null) {
            exchange.getResponseHeaders().set("Retry-After", reply.retryAfter());
          }
          exchange.sendResponseHeaders(reply.status(), bytes.length);
          exchange.getResponseBody().write(bytes);
          exchange.close();
        });
    server.start();
  }

  public void enqueue(int status, String contentType, String body, String retryAfter) {
    replies.add(new Reply(status, contentType, body, retryAfter));
  }

  public RestClient client() {
    return RestClient.builder().baseUrl(baseUrl()).build();
  }

  public String baseUrl() {
    return "http://127.0.0.1:" + server.getAddress().getPort();
  }

  public List<URI> requests() {
    return List.copyOf(requests);
  }

  @Override
  public void close() {
    server.stop(0);
  }

  private record Reply(int status, String contentType, String body, String retryAfter) {}
}
