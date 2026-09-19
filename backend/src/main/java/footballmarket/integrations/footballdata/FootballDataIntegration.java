package footballmarket.integrations.footballdata;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import footballmarket.models.Player;
import footballmarket.models.PlayerSnapshot;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class FootballDataIntegration {
  private static final Logger log = LoggerFactory.getLogger(FootballDataIntegration.class);
  private final RestClient client;

  public FootballDataIntegration(@Qualifier("footballDataRestClient") RestClient client) {
    this.client = client;
  }

  /** Obtiene todos los planteles de una competición o falla sin entregar una foto parcial. */
  public PlayerSnapshot fetchCompetition(String code) {
    try {
      Competition competition = read("/competitions/{code}", Competition.class, code);
      Teams teams = read("/competitions/{code}/teams", Teams.class, code);
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
        Team team = read("/teams/{id}", Team.class, reference.id());
        if (team.squad() == null) {
          throw new FootballDataUnavailableException();
        }
        for (SquadMember member : team.squad()) {
          obtained++;
          String missing = missingField(member, team.name(), competition.name());
          if (missing != null) {
            discarded++;
            log.warn("Jugador descartado: falta el campo obligatorio {}", missing);
          } else {
            players.add(
                new Player(
                    member.id(),
                    member.name(),
                    team.name(),
                    competition.name(),
                    member.position()));
          }
        }
      }
      return new PlayerSnapshot(players, obtained, discarded);
    } catch (RestClientException | FootballDataUnavailableException ex) {
      log.warn("Lectura del proveedor fallida: comunicación o estructura incompleta");
      throw new FootballDataUnavailableException();
    }
  }

  private <T> T read(String path, Class<T> type, Object parameter) {
    T body =
        client
            .get()
            .uri(path, parameter)
            .retrieve()
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
  }

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

  private static boolean blank(String value) {
    return value == null || value.isBlank();
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  private record Competition(String name) {}

  @JsonIgnoreProperties(ignoreUnknown = true)
  private record Teams(List<TeamReference> teams) {}

  @JsonIgnoreProperties(ignoreUnknown = true)
  private record TeamReference(Long id) {}

  @JsonIgnoreProperties(ignoreUnknown = true)
  private record Team(String name, List<SquadMember> squad) {}

  @JsonIgnoreProperties(ignoreUnknown = true)
  private record SquadMember(Long id, String name, String position) {}
}
