package footballmarket.config;

import footballmarket.integrations.QuoteSource;
import footballmarket.integrations.SimulatedQuoteSource;
import footballmarket.integrations.exceptions.MissingQuoteSourceException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

/**
 * Auto-configuración del simulador de cotizaciones, posterior a las configuraciones de usuario.
 *
 * <p>El simulador es un componente estrictamente temporal de desarrollo y pruebas, desactivado por
 * defecto. La fuente activa se resuelve una sola vez, en el arranque: cuando existe una fuente real
 * de REQ-S2-03 el simulador no se registra, aunque su propiedad esté habilitada, porque la
 * selección se resuelve por presencia de la fuente real y no por propiedad.
 */
@AutoConfiguration
public class QuoteSimulationConfiguration {

  /** Habilita el simulador solo donde se autorice. */
  public static final String ENABLED_PROPERTY = "market.quote.simulator.enabled";

  /** Precio por token simulado en centésimas de crédito. */
  public static final String PRICE_PROPERTY = "market.quote.simulator.price";

  /**
   * Registra el simulador únicamente cuando no existe otra fuente de cotización y la propiedad de
   * habilitación está activa.
   *
   * @param price precio por token en centésimas leído de {@value #PRICE_PROPERTY}
   * @return simulador registrado como fuente de cotización
   */
  @Bean
  @ConditionalOnMissingBean(QuoteSource.class)
  @ConditionalOnProperty(name = ENABLED_PROPERTY, havingValue = "true")
  public QuoteSource simulatedQuoteSource(@Value("${" + PRICE_PROPERTY + ":100}") long price) {
    return new SimulatedQuoteSource(price);
  }

  /**
   * Verifica al arrancar que exista una fuente de cotización activa.
   *
   * <p>Sin fuente real y sin simulador no existe precio válido, de modo que la compra no podría
   * confirmarse. Se prefiere un fallo de configuración explícito en el arranque antes que un precio
   * inventado o una degradación silenciosa.
   *
   * @param quotes fuentes de cotización presentes en el contexto
   * @return operación de arranque que valida la disponibilidad de una fuente
   */
  @Bean
  public SmartInitializingSingleton quoteSourceAvailabilityGuard(
      ObjectProvider<QuoteSource> quotes) {
    return () -> {
      if (quotes.stream().findAny().isEmpty()) {
        throw new MissingQuoteSourceException();
      }
    };
  }
}
