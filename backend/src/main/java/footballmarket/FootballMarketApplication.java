package footballmarket;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/** Punto de entrada de la aplicación Football Market. */
@SpringBootApplication
@ConfigurationPropertiesScan
public class FootballMarketApplication {

  /**
   * Inicia la aplicación Spring Boot.
   *
   * @param args argumentos de línea de comandos
   */
  public static void main(String[] args) {
    SpringApplication.run(FootballMarketApplication.class, args);
  }
}
