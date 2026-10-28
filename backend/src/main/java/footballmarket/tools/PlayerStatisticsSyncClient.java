package footballmarket.tools;

import footballmarket.controllers.dtos.requests.PlayerStatisticsSyncRequestDTO;
import footballmarket.controllers.dtos.responses.ErrorResponseDTO;
import footballmarket.controllers.dtos.responses.PlayerStatisticsSyncResponseDTO;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import org.springframework.web.client.RestClient;

/**
 * Cliente operativo del desarrollador para un único POST síncrono de scraping.
 *
 * <p>No arranca Spring, no consulta el proveedor ni la persistencia y no admite canal: solo invoca
 * {@code /sync/scraping} con DTO y cliente HTTP reales. El JWT se toma del entorno y nunca de
 * argumentos ni de la URL.
 */
public final class PlayerStatisticsSyncClient {
  private static final String TOKEN_VARIABLE = "FOOTBALL_MARKET_TOKEN";

  /** El cliente es invocable; no es un bean de la aplicación ni requiere construcción especial. */
  public PlayerStatisticsSyncClient() {}

  public static void main(String[] args) {
    System.exit(new PlayerStatisticsSyncClient().run(args));
  }

  /** Ejecuta el cliente con el entorno del proceso y devuelve el código de salida. */
  public int run(String[] args) {
    return this.run(args, System.getenv());
  }

  /**
   * Ejecuta el cliente con un entorno explícito.
   *
   * <p>Solo la entrada de producción usa {@link System#getenv()}; esta sobrecarga permite invocar
   * el mismo cliente con un entorno controlado sin alterar su comportamiento.
   */
  public int run(String[] args, java.util.Map<String, String> environment) {
    Options options;
    try {
      options = Options.parse(args);
    } catch (IllegalArgumentException invalid) {
      System.err.println(invalid.getMessage());
      System.err.println(Usage.TEXT);
      return 2;
    }
    String token = environment.get(TOKEN_VARIABLE);
    if (token == null || token.isBlank()) {
      System.err.println("Falta la variable de entorno " + TOKEN_VARIABLE);
      return 2;
    }
    LocalDate extractionDate =
        options.extractionDate() == null ? LocalDate.now(ZoneOffset.UTC) : options.extractionDate();
    RestClient client =
        RestClient.builder()
            .baseUrl(options.baseUrl())
            .defaultHeader("Authorization", "Bearer " + token)
            .build();
    try {
      return this.scrape(client, extractionDate);
    } catch (org.springframework.web.client.RestClientResponseException failure) {
      System.err.println(
          "Fallo HTTP " + failure.getStatusCode().value() + ": " + this.describe(failure));
      return 1;
    } catch (RuntimeException failure) {
      System.err.println("Fallo de transporte: " + failure.getClass().getSimpleName());
      return 1;
    }
  }

  /** Un POST sin timeout total propio; salida cero solo con cobertura completa, nunca retry. */
  private int scrape(RestClient client, LocalDate extractionDate) {
    PlayerStatisticsSyncResponseDTO summary =
        client
            .post()
            .uri("/api/players/statistics/whoscored/sync/scraping")
            .body(new PlayerStatisticsSyncRequestDTO(extractionDate))
            .retrieve()
            .body(PlayerStatisticsSyncResponseDTO.class);
    if (summary == null) {
      return 1;
    }
    System.out.printf(
        "Ejecución %d: estado %s, seleccionados %d, completados %d, fallidos %d, pendientes %d%n",
        summary.runId(),
        summary.status(),
        summary.players().selected(),
        summary.players().completed(),
        summary.players().failed(),
        summary.players().pending());
    return summary.coverageComplete() ? 0 : 1;
  }

  /** Solo informa el código y el mensaje del error; nunca incluye credenciales ni cabeceras. */
  private String describe(org.springframework.web.client.RestClientResponseException failure) {
    ErrorResponseDTO body = null;
    try {
      body = failure.getResponseBodyAs(ErrorResponseDTO.class);
    } catch (RuntimeException unreadable) {
      body = null;
    }
    return body == null || body.message() == null ? "sin cuerpo" : body.message();
  }

  /** Opciones no sensibles admitidas por el cliente. */
  record Options(LocalDate extractionDate, String baseUrl) {
    static Options parse(String[] args) {
      LocalDate extractionDate = null;
      String baseUrl = null;
      List<String> allowed = List.of("--extraction-date", "--base-url");
      for (int index = 0; index < args.length; index += 2) {
        if (index + 1 >= args.length || !allowed.contains(args[index])) {
          throw new IllegalArgumentException("Opción no reconocida");
        }
        String value = args[index + 1];
        switch (args[index]) {
          case "--extraction-date" -> extractionDate = LocalDate.parse(value);
          default -> baseUrl = value;
        }
      }
      if (baseUrl == null || baseUrl.isBlank()) {
        throw new IllegalArgumentException("Falta --base-url");
      }
      return new Options(extractionDate, baseUrl);
    }
  }

  /** Ayuda del cliente, sin ejemplos con secretos. */
  static final class Usage {
    static final String TEXT =
        "Uso: [--extraction-date YYYY-MM-DD] --base-url http://localhost:8080";

    private Usage() {}
  }
}
