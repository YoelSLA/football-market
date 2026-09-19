package footballmarket.integrations;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import footballmarket.integrations.exceptions.FootballDataRateLimitException;
import footballmarket.integrations.exceptions.FootballDataUnavailableException;
import footballmarket.models.Player;
import footballmarket.models.records.PlayerSnapshot;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Integración con la API de Football-Data.
 *
 * <p>Se encarga de obtener las competiciones, equipos y jugadores proporcionados por el servicio
 * externo. También administra los límites de peticiones de la API mediante reintentos cuando se
 * recibe una respuesta HTTP 429.
 */
@Component
public class FootballDataIntegration {

  private static final Logger LOG = LoggerFactory.getLogger(FootballDataIntegration.class);
  private static final int MAX_RETRIES = 3;
  private static final long DEFAULT_RETRY_SECONDS = 60;
  private static final long RETRY_MARGIN_SECONDS = 1;

  private final RestClient client;

  /**
   * Crea la integración utilizando el cliente HTTP configurado para Football-Data.
   *
   * @param client cliente HTTP utilizado para realizar las peticiones a Football-Data
   */
  public FootballDataIntegration(@Qualifier("footballDataRestClient") RestClient client) {
    this.client = client;
  }

  /**
   * Obtiene todos los jugadores pertenecientes a los equipos de una competición.
   *
   * <p>Los jugadores que no poseen alguno de los campos obligatorios son descartados. La operación
   * devuelve una fotografía completa de la competición o falla si no es posible obtener toda la
   * información necesaria.
   *
   * @param code código de la competición utilizado por Football-Data
   * @return fotografía con los jugadores válidos y las cantidades de jugadores obtenidos y
   *     descartados
   * @throws FootballDataUnavailableException si no es posible obtener completamente la información
   *     de la competición
   */
  public PlayerSnapshot fetchCompetition(String code) {
    try {
      Competition competition = this.read("/competitions/{code}", Competition.class, code);
      Teams teams = this.read("/competitions/{code}/teams", Teams.class, code);

      if (teams.teams() == null) {
        throw new FootballDataUnavailableException();
      }

      List<Player> players = new ArrayList<>();

      int obtained = 0;
      int discarded = 0;

      for (TeamReference reference : teams.teams()) {
        if (reference == null || reference.id() == null) {
          throw new FootballDataUnavailableException();
        }

        Team team = this.read("/teams/{id}", Team.class, reference.id());

        if (team.squad() == null) {
          throw new FootballDataUnavailableException();
        }

        for (SquadMember member : team.squad()) {
          obtained++;

          String missing = missingField(member, team.name(), competition.name());
          if (missing != null) {
            discarded++;
            LOG.warn("Jugador descartado: falta el campo obligatorio {}", missing);
            continue;
          }

          Player player =
              new Player(
                  member.id(), member.name(), team.name(), competition.name(), member.position());

          players.add(player);
        }
      }

      return new PlayerSnapshot(players, obtained, discarded);

    } catch (RestClientException | FootballDataUnavailableException ex) {
      LOG.warn("Lectura del proveedor fallida: comunicación o estructura incompleta");
      throw new FootballDataUnavailableException();
    }
  }

  /**
   * Realiza una petición GET a Football-Data y convierte la respuesta al tipo solicitado.
   *
   * <p>Cuando la API responde con HTTP 429, la petición se reintenta respetando el valor del header
   * {@code Retry-After}. Si el header no está disponible o contiene un valor inválido, se utiliza
   * un tiempo de espera predeterminado.
   *
   * @param path ruta del recurso solicitado
   * @param type tipo esperado para el cuerpo de la respuesta
   * @param parameter parámetro utilizado para completar la ruta
   * @param <T> tipo de la respuesta
   * @return cuerpo de la respuesta convertido al tipo solicitado
   * @throws FootballDataUnavailableException si la petición falla, la respuesta no contiene cuerpo
   *     o se agotan los reintentos permitidos
   */
  private <T> T read(String path, Class<T> type, Object parameter) {
    for (int attempt = 0; attempt <= MAX_RETRIES; attempt++) {
      try {
        T body =
            this.client
                .get()
                .uri(path, parameter)
                .retrieve()
                .onStatus(
                    status -> status.value() == HttpStatus.TOO_MANY_REQUESTS.value(),
                    (request, response) -> {
                      long retryAfterSeconds =
                          parseRetryAfter(response.getHeaders().getFirst("Retry-After"));

                      throw new FootballDataRateLimitException(retryAfterSeconds);
                    })
                .onStatus(
                    status -> !status.is2xxSuccessful(),
                    (request, response) -> {
                      throw new FootballDataUnavailableException();
                    })
                .body(type);

        if (body == null) {
          throw new FootballDataUnavailableException();
        }

        return body;

      } catch (FootballDataRateLimitException ex) {
        if (attempt >= MAX_RETRIES) {
          throw new FootballDataUnavailableException();
        }

        long waitSeconds = ex.retryAfterSeconds() + RETRY_MARGIN_SECONDS;

        waitBeforeRetry(waitSeconds);
      }
    }

    throw new FootballDataUnavailableException();
  }

  /**
   * Obtiene la cantidad de segundos indicada por el header {@code Retry-After}.
   *
   * <p>Si el header no existe o su contenido no puede convertirse a un número, se utiliza el tiempo
   * de espera predeterminado.
   *
   * @param retryAfter valor recibido en el header {@code Retry-After}
   * @return cantidad de segundos que deben esperarse antes de realizar otro intento
   */
  private static long parseRetryAfter(String retryAfter) {
    if (retryAfter == null || retryAfter.isBlank()) {
      return DEFAULT_RETRY_SECONDS;
    }

    try {
      long seconds = Long.parseLong(retryAfter);
      return seconds >= 0 ? seconds : DEFAULT_RETRY_SECONDS;
    } catch (NumberFormatException ex) {
      return DEFAULT_RETRY_SECONDS;
    }
  }

  /**
   * Suspende temporalmente el hilo actual antes de reintentar una petición.
   *
   * <p>Si el hilo es interrumpido durante la espera, se restaura su estado de interrupción y se
   * cancela la operación.
   *
   * @param seconds cantidad de segundos que debe esperar el hilo
   * @throws FootballDataUnavailableException si el hilo es interrumpido durante la espera
   */
  private static void waitBeforeRetry(long seconds) {
    try {
      Thread.sleep(Duration.ofSeconds(seconds));
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
      throw new FootballDataUnavailableException();
    }
  }

  /**
   * Determina si falta alguno de los campos obligatorios necesarios para crear un jugador.
   *
   * @param member jugador recibido desde Football-Data
   * @param team nombre del equipo
   * @param league nombre de la competición
   * @return nombre del campo faltante o {@code null} si todos los campos requeridos están presentes
   */
  private static String missingField(SquadMember member, String team, String league) {
    if (member == null || member.id() == null) {
      return "id";
    }

    if (blank(member.name())) {
      return "name";
    }

    if (blank(team)) {
      return "team";
    }

    if (blank(league)) {
      return "league";
    }

    if (blank(member.position())) {
      return "position";
    }

    return null;
  }

  /**
   * Comprueba si una cadena es nula, está vacía o contiene únicamente espacios.
   *
   * @param value cadena a comprobar
   * @return {@code true} si la cadena no contiene un valor válido
   */
  private static boolean blank(String value) {
    return value == null || value.isBlank();
  }

  /** Representa la información necesaria de una competición recibida desde Football-Data. */
  @JsonIgnoreProperties(ignoreUnknown = true)
  private record Competition(String name) {}

  /** Representa la colección de equipos pertenecientes a una competición. */
  @JsonIgnoreProperties(ignoreUnknown = true)
  private record Teams(List<TeamReference> teams) {}

  /** Representa la referencia a un equipo devuelta en el listado de una competición. */
  @JsonIgnoreProperties(ignoreUnknown = true)
  private record TeamReference(Long id) {}

  /** Representa la información necesaria de un equipo y su plantel. */
  @JsonIgnoreProperties(ignoreUnknown = true)
  private record Team(String name, List<SquadMember> squad) {}

  /** Representa la información necesaria de un jugador perteneciente a un plantel. */
  @JsonIgnoreProperties(ignoreUnknown = true)
  private record SquadMember(Long id, String name, String position) {}
}
