package footballmarket.features.quote.infraestructure.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/** Registra la configuración validada del control de variabilidad de las cotizaciones. */
@Configuration
@EnableConfigurationProperties(QuoteProperties.class)
public class QuoteConfig {}
