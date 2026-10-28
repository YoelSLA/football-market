package footballmarket.features.valuation.infraestructure.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/** Registra la configuración validada del cálculo de valoraciones. */
@Configuration
@EnableConfigurationProperties(ValuationProperties.class)
public class ValuationConfig {}
