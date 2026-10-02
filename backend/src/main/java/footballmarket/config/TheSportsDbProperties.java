package footballmarket.config;

import footballmarket.integrations.exceptions.InvalidTheSportsDbConfigurationException;
import java.net.URI;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Parámetros validados de cuota, reintentos, retención y acceso a TheSportsDB. */
@ConfigurationProperties(prefix = "the-sports-db")
public record TheSportsDbProperties(
    String apiKey,
    String baseUrl,
    long requestIntervalMs,
    int maxRetries,
    int imageRetryDays,
    Integer auditRetentionDays) {
  public TheSportsDbProperties {
    try {
      URI uri = URI.create(baseUrl);
      if (apiKey == null
          || !apiKey.matches("[A-Za-z0-9_-]+")
          || !"https".equalsIgnoreCase(uri.getScheme())
          || uri.getHost() == null
          || uri.getUserInfo() != null
          || uri.getFragment() != null
          || requestIntervalMs < 2500
          || maxRetries < 0
          || imageRetryDays < 1
          || (auditRetentionDays != null && auditRetentionDays < 1)) {
        throw new InvalidTheSportsDbConfigurationException();
      }
    } catch (IllegalArgumentException | NullPointerException exception) {
      throw new InvalidTheSportsDbConfigurationException();
    }
  }

  /** No expone credenciales en registros de configuración o fallos de startup. */
  @Override
  public String toString() {
    return "TheSportsDbProperties[configuración protegida]";
  }
}
