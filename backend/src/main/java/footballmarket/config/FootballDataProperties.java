package footballmarket.config;

import footballmarket.integrations.exceptions.InvalidFootballDataConfigurationException;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Configuración necesaria para acceder y consultar Football-Data. */
@ConfigurationProperties(prefix = "football-data")
public record FootballDataProperties(String apiKey, String baseUrl, List<String> competitions) {

  /** Valida las cinco ligas obligatorias sin exigir orden y copia la lista defensivamente. */
  public FootballDataProperties {
    if (competitions == null
        || competitions.size() != 5
        || !competitions.containsAll(List.of("PL", "BL1", "PD", "SA", "FL1"))) {
      throw new InvalidFootballDataConfigurationException(
          "Debe configurar las cinco ligas PL,BL1,PD,SA,FL1, sin faltantes ni adicionales");
    }
    competitions = List.copyOf(competitions);
  }

  @Override
  public String toString() {
    return "FootballDataProperties[configuración protegida]";
  }
}
