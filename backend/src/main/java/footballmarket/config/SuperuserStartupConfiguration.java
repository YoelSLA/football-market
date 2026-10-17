package footballmarket.config;

import footballmarket.services.SuperuserProvisioningService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Conecta el aprovisionamiento del SUPERUSER con el ciclo de vida del contexto.
 *
 * <p>El aprovisionamiento solo se ejecuta cuando la propiedad {@code
 * footballmarket.superuser.enabled} está habilitada. La inicialización es idempotente, por lo que
 * reiniciar la aplicación no produce usuarios adicionales ni modifica el SUPERUSER ya existente.
 */
@Configuration(proxyBeanMethods = false)
public class SuperuserStartupConfiguration {

  private final SuperuserProvisioningService provisioningService;
  private final SuperuserProperties properties;

  public SuperuserStartupConfiguration(
      SuperuserProvisioningService provisioningService, SuperuserProperties properties) {
    this.provisioningService = provisioningService;
    this.properties = properties;
  }

  /**
   * Aprovisiona el SUPERUSER antes de atender solicitudes cuando la configuración lo habilita.
   *
   * <p>Con el aprovisionamiento deshabilitado no se crea ninguna cuenta por el arranque.
   *
   * @return operación de arranque que ejecuta la provisión
   */
  @Bean
  public org.springframework.beans.factory.SmartInitializingSingleton provisionSuperuser() {
    return () -> {
      if (this.properties.enabled()) {
        this.provisioningService.provisionSuperuser();
      }
    };
  }
}
