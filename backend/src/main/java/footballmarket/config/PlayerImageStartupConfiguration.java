package footballmarket.config;

import footballmarket.services.PlayerImageRunRecoveryService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Conecta la recuperación persistente con el ciclo de vida del contexto. */
@Configuration(proxyBeanMethods = false)
public class PlayerImageStartupConfiguration {
  private final PlayerImageRunRecoveryService recovery;

  public PlayerImageStartupConfiguration(PlayerImageRunRecoveryService recovery) {
    this.recovery = recovery;
  }

  /** Impide servir solicitudes si no se pudieron cerrar los runs de un arranque anterior. */
  @Bean
  public org.springframework.beans.factory.SmartInitializingSingleton recoverPlayerImageRuns() {
    return this.recovery::recover;
  }
}
