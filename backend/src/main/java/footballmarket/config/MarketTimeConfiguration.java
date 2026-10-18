package footballmarket.config;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Reloj UTC inyectable para decidir el vencimiento de publicaciones de forma determinista. */
@Configuration
public class MarketTimeConfiguration {
  @Bean
  public Clock marketClock() {
    return Clock.systemUTC();
  }
}
