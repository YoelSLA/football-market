package footballmarket.config;

import java.time.Clock;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Binding de estadísticas independiente del cliente de identidad; no inicializa el navegador. */
@Configuration
@EnableConfigurationProperties(PlayerStatisticsProperties.class)
public class PlayerStatisticsConfig {
  /**
   * Reloj UTC de las campañas de estadísticas.
   *
   * <p>Es sustituible en pruebas para demostrar que la fecha de una campaña queda fijada al inicio
   * aunque el recorrido cruce medianoche.
   */
  @Bean
  public Clock statisticsClock() {
    return Clock.systemUTC();
  }
}
