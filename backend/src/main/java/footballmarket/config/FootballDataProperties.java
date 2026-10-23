package footballmarket.config;

import footballmarket.integrations.exceptions.InvalidFootballDataConfigurationException;
import java.util.List;
import java.util.Set;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Configuración necesaria para acceder y consultar Football-Data. */
@ConfigurationProperties(prefix = "football-data")
public record FootballDataProperties(String apiKey, String baseUrl, List<String> competitions) {

  /**
   * Construye la configuración de Football-Data validando las competiciones requeridas.
   *
   * <p>La lista no vacía admite competiciones adicionales sin duplicados y conserva su orden.
   *
   * @throws InvalidFootballDataConfigurationException si las competiciones son vacías, inválidas o
   *     duplicadas
   */
  public FootballDataProperties {
    validateCompetitions(competitions);
    competitions = List.copyOf(competitions);
  }

  /**
   * Valida que haya al menos una competición y que todos sus códigos sean únicos y no vacíos.
   *
   * @param competitions competiciones configuradas para consultar Football-Data
   * @throws InvalidFootballDataConfigurationException si la lista es nula, vacía o inválida
   */
  private static void validateCompetitions(List<String> competitions) {
    if (competitions == null
        || competitions.isEmpty()
        || competitions.stream()
            .anyMatch(code -> code == null || code.isBlank() || !code.equals(code.strip()))
        || Set.copyOf(competitions).size() != competitions.size()) {
      throw new InvalidFootballDataConfigurationException(
          "Debe configurar al menos una competición, con códigos no vacíos y sin duplicados");
    }
  }

  /**
   * Devuelve una representación segura de la configuración sin exponer valores sensibles.
   *
   * @return representación textual protegida de la configuración
   */
  @Override
  public String toString() {
    return "FootballDataProperties[configuración protegida]";
  }
}
