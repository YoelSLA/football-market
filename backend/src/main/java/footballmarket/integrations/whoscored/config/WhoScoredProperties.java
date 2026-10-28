package footballmarket.integrations.whoscored.config;

import footballmarket.features.identity.application.exceptions.PlayerIdentitySynchronizationException;
import java.net.URI;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuración del cliente de identidad; cada liga local se enlaza a su competición en WhoScored.
 */
@ConfigurationProperties(prefix = "whoscored")
public record WhoScoredProperties(
    String baseUrl,
    String userAgent,
    int pageSize,
    long requestIntervalMs,
    int connectTimeoutMs,
    int readTimeoutMs,
    int maxPagesPerLeague,
    int maxResponseBytes,
    List<Competition> competitions) {

  public WhoScoredProperties {
    try {
      URI uri = URI.create(baseUrl);
      if (!"https".equalsIgnoreCase(uri.getScheme())
          || uri.getHost() == null
          || uri.getUserInfo() != null
          || uri.getRawQuery() != null
          || uri.getFragment() != null
          || userAgent == null
          || userAgent.isBlank()
          || userAgent.contains("\r")
          || userAgent.contains("\n")
          || pageSize < 1
          || pageSize > 100
          || requestIntervalMs < 1
          || connectTimeoutMs < 1
          || readTimeoutMs < 1
          || maxPagesPerLeague < 1
          || maxResponseBytes < 1
          || competitions == null
          || competitions.size() != 5) {
        throw new PlayerIdentitySynchronizationException();
      }
      Set<Long> leagueIds = new HashSet<>();
      Set<Integer> tournaments = new HashSet<>();
      for (Competition competition : competitions) {
        if (competition == null
            || competition.leagueId() == null
            || competition.leagueId() < 1
            || competition.tournamentId() < 1
            || !leagueIds.add(competition.leagueId())
            || !tournaments.add(competition.tournamentId())) {
          throw new PlayerIdentitySynchronizationException();
        }
      }
      competitions = List.copyOf(competitions);
    } catch (IllegalArgumentException | NullPointerException failure) {
      throw new PlayerIdentitySynchronizationException();
    }
  }

  /** Correspondencia entre la identidad local y la competición del proveedor. */
  public record Competition(Long leagueId, int tournamentId) {}
}
