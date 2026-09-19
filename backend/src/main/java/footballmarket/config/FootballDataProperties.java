package footballmarket.config;

import footballmarket.integrations.exceptions.InvalidFootballDataConfigurationException;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Configuración necesaria para acceder y consultar Football-Data. */
@ConfigurationProperties(prefix = "football-data")
public record FootballDataProperties(String apiKey, String baseUrl, List<String> competitions) {

  /** Valida y copia defensivamente la lista ordenada de competiciones. */
  public FootballDataProperties {
    if (competitions == null
        || competitions.isEmpty()
        || competitions.stream().anyMatch(code -> code == null || code.isBlank())) {
      throw new InvalidFootballDataConfigurationException(
          "Debe configurar al menos una competición válida");
    }
    competitions = List.copyOf(competitions);
  }

  @Override
  public String toString() {
    return "FootballDataProperties[configuración protegida]";
  }
}
