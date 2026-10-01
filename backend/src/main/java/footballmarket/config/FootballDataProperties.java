package footballmarket.config;

import footballmarket.integrations.exceptions.InvalidFootballDataConfigurationException;
import java.util.List;
import java.util.Set;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Configuración necesaria para acceder y consultar Football-Data. */
@ConfigurationProperties(prefix = "football-data")
public record FootballDataProperties(String apiKey, String baseUrl, List<String> competitions) {

  private static final Set<String> REQUIRED_COMPETITIONS = Set.of("PL", "BL1", "PD", "SA", "FL1");

  /**
   * Construye la configuración de Football-Data validando las competiciones requeridas.
   *
   * <p>La configuración debe contener exactamente las cinco ligas soportadas, sin exigir un orden
   * específico. La lista recibida se copia defensivamente para evitar modificaciones externas.
   *
   * @throws InvalidFootballDataConfigurationException si las competiciones configuradas no
   *     coinciden con las requeridas
   */
  public FootballDataProperties {
    validateCompetitions(competitions);
    competitions = List.copyOf(competitions);
  }

  /**
   * Válida que estén configuradas exactamente las competiciones soportadas por la aplicación.
   *
   * @param competitions competiciones configuradas para consultar Football-Data
   * @throws InvalidFootballDataConfigurationException si la lista es nula o las competiciones no
   *     coinciden exactamente con las requeridas
   */
  private static void validateCompetitions(List<String> competitions) {
    if (competitions == null
        || competitions.size() != REQUIRED_COMPETITIONS.size()
        || !Set.copyOf(competitions).equals(REQUIRED_COMPETITIONS)) {
      throw new InvalidFootballDataConfigurationException(
          "Debe configurar las cinco ligas PL,BL1,PD,SA,FL1, sin faltantes ni adicionales");
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
