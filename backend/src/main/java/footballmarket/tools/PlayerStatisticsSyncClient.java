package footballmarket.tools;

import footballmarket.controllers.dtos.requests.PlayerStatisticsSyncRequestDTO;
import footballmarket.controllers.dtos.responses.ErrorResponseDTO;
import footballmarket.controllers.dtos.responses.PlayerStatisticsCoverageResponseDTO;
import footballmarket.controllers.dtos.responses.PlayerStatisticsSyncResponseDTO;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import org.springframework.web.client.RestClient;

/**
 * Cliente operativo del desarrollador para scraping por lotes o campaña.
 *
 * <p>No arranca Spring, no consulta el proveedor ni la persistencia y no admite canal: solo invoca
 * {@code /sync/scraping} y {@code /coverage} con DTO y cliente HTTP reales. El JWT se toma del
 * entorno y nunca de argumentos ni de la URL.
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
      PlayerStatisticsCoverageResponseDTO initial = this.coverage(client, extractionDate);
      System.out.printf(
          "Cobertura inicial SCRAPING %s: pendientes %d, completos %d%n",
          extractionDate, initial.pendingPlayers(), initial.completedPlayers());
      if (initial.coverageComplete()) {
        System.out.println("Campaña completa: no hay lotes pendientes");
        return 0;
      }
      if (options.mode() == Mode.BATCH) {
        int progressed =
            this.batch(client, extractionDate, options.maxPlayers(), initial.pendingPlayers());
        boolean complete = this.coverage(client, extractionDate).coverageComplete();
        return progressed == 0 && !complete ? 1 : progressed;
      }
      return this.campaign(client, extractionDate, options.maxPlayers(), initial.pendingPlayers());
    } catch (org.springframework.web.client.RestClientResponseException failure) {
      System.err.println(
          "Fallo HTTP " + failure.getStatusCode().value() + ": " + this.describe(failure));
      return 1;
    } catch (RuntimeException failure) {
      System.err.println("Fallo de transporte: " + failure.getClass().getSimpleName());
      return 1;
    }
  }

  /** Ejecuta un lote y devuelve cero solo si avanzó; nunca reintenta peticiones inciertas. */
  private int batch(
      RestClient client, LocalDate extractionDate, Integer maxPlayers, long pendingBefore) {
    PlayerStatisticsSyncResponseDTO summary = this.scrape(client, extractionDate, maxPlayers);
    System.out.printf(
        "Lote %d: estado %s, seleccionados %d, completados %d, fallidos %d, pendientes %d%n",
        summary.runId(),
        summary.status(),
        summary.players().selected(),
        summary.players().completed(),
        summary.players().failed(),
        summary.players().pending());
    if (summary.status().equals("FAILED") || summary.status().equals("INTERRUPTED")) {
      System.err.println("Campaña incompleta: el lote terminó en " + summary.status());
      return 1;
    }
    if (summary.coverageComplete()) {
      return 0;
    }
    if (summary.players().pending() >= pendingBefore) {
      System.err.println("Campaña incompleta: el lote no redujo los pendientes");
      return 1;
    }
    return 0;
  }

  private int campaign(
      RestClient client, LocalDate extractionDate, Integer maxPlayers, long pendingBefore) {
    long previous = pendingBefore;
    while (true) {
      PlayerStatisticsCoverageResponseDTO current = this.coverage(client, extractionDate);
      if (current.coverageComplete()) {
        System.out.println("Campaña completa");
        return 0;
      }
      if (this.batch(client, extractionDate, maxPlayers, current.pendingPlayers()) != 0) {
        return 1;
      }
      long remaining = this.coverage(client, extractionDate).pendingPlayers();
      if (remaining >= previous) {
        System.err.println("Campaña incompleta: no hubo progreso en los pendientes");
        return 1;
      }
      previous = remaining;
    }
  }

  private PlayerStatisticsSyncResponseDTO scrape(
      RestClient client, LocalDate extractionDate, Integer maxPlayers) {
    return client
        .post()
        .uri("/api/players/statistics/whoscored/sync/scraping")
        .body(new PlayerStatisticsSyncRequestDTO(extractionDate, maxPlayers))
        .retrieve()
        .body(PlayerStatisticsSyncResponseDTO.class);
  }

  private PlayerStatisticsCoverageResponseDTO coverage(
      RestClient client, LocalDate extractionDate) {
    return client
        .get()
        .uri(
            uriBuilder ->
                uriBuilder
                    .path("/api/players/statistics/whoscored/coverage")
                    .queryParam("source", "SCRAPING")
                    .queryParam("extractionDate", extractionDate)
                    .build())
        .retrieve()
        .body(PlayerStatisticsCoverageResponseDTO.class);
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

  /** Modos operativos del cliente; no existe modo JSON. */
  enum Mode {
    BATCH,
    CAMPAIGN
  }

  /** Opciones no sensibles admitidas por el cliente. */
  record Options(Mode mode, LocalDate extractionDate, Integer maxPlayers, String baseUrl) {
    static Options parse(String[] args) {
      Mode mode = null;
      LocalDate extractionDate = null;
      Integer maxPlayers = null;
      String baseUrl = null;
      List<String> allowed = List.of("--mode", "--extraction-date", "--max-players", "--base-url");
      for (int index = 0; index < args.length; index += 2) {
        if (index + 1 >= args.length || !allowed.contains(args[index])) {
          throw new IllegalArgumentException("Opción no reconocida");
        }
        String value = args[index + 1];
        switch (args[index]) {
          case "--mode" -> {
            if ("campaign".equalsIgnoreCase(value)) {
              mode = Mode.CAMPAIGN;
            } else if ("batch".equalsIgnoreCase(value)) {
              mode = Mode.BATCH;
            } else {
              throw new IllegalArgumentException("El modo solo admite batch o campaign");
            }
          }
          case "--extraction-date" -> extractionDate = LocalDate.parse(value);
          case "--max-players" -> {
            int parsed = Integer.parseInt(value);
            if (parsed < 1) {
              throw new IllegalArgumentException("El máximo debe ser un entero positivo");
            }
            maxPlayers = parsed;
          }
          default -> baseUrl = value;
        }
      }
      if (mode == null) {
        throw new IllegalArgumentException("Falta --mode=batch|campaign");
      }
      if (baseUrl == null || baseUrl.isBlank()) {
        throw new IllegalArgumentException("Falta --base-url");
      }
      return new Options(mode, extractionDate, maxPlayers, baseUrl);
    }
  }

  /** Ayuda del cliente, sin ejemplos con secretos. */
  static final class Usage {
    static final String TEXT =
        "Uso: --mode=batch|campaign [--extraction-date=YYYY-MM-DD] [--max-players=N]"
            + " --base-url=http://localhost:8080";

    private Usage() {}
  }
}
