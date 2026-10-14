package footballmarket.config;

import footballmarket.services.exceptions.InvalidSuperuserConfigurationException;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuración del aprovisionamiento interno del SUPERUSER.
 *
 * <p>El SUPERUSER es la cuenta singular del dominio de mercado que concentra inicialmente la
 * totalidad de los tokens de los jugadores. No equivale a una administración general del sistema y
 * no se crea mediante el registro público.
 *
 * <p>Su aprovisionamiento está deshabilitado por defecto y solo se ejecuta cuando {@code enabled}
 * vale {@code true}. Identidad y contraseña provienen del entorno de ejecución: el repositorio no
 * declara credenciales ni valores por defecto de identidad o contraseña.
 *
 * @param enabled indica si el aprovisionamiento debe ejecutarse al arrancar
 * @param username nombre de usuario público del SUPERUSER
 * @param email correo electrónico del SUPERUSER, que es su credencial de acceso
 * @param password contraseña en texto plano del SUPERUSER, provista por el entorno y almacenada
 *     únicamente como hash BCrypt
 */
@ConfigurationProperties(prefix = "footballmarket.superuser")
public record SuperuserProperties(boolean enabled, String username, String email, String password) {

  /**
   * Valida que la configuración esté completa cuando el aprovisionamiento está habilitado.
   *
   * @throws InvalidSuperuserConfigurationException si el aprovisionamiento está habilitado y falta
   *     alguna de las propiedades de identidad o contraseña
   */
  public SuperuserProperties {
    if (enabled) {
      requireConfigured(username, "username");
      requireConfigured(email, "email");
      requireConfigured(password, "password");
    }
  }

  /**
   * Rechaza una propiedad requerida ausente o vacía.
   *
   * @param value valor configurado
   * @param property nombre de la propiedad requerida dentro del prefijo de configuración
   * @throws InvalidSuperuserConfigurationException si el valor no fue proporcionado
   */
  private static void requireConfigured(String value, String property) {
    if (value == null || value.isBlank()) {
      throw new InvalidSuperuserConfigurationException(
          "La propiedad footballmarket.superuser.%s es obligatoria cuando el aprovisionamiento del"
              + " SUPERUSER está habilitado.".formatted(property));
    }
  }

  /**
   * Devuelve una representación segura de la configuración sin exponer la contraseña.
   *
   * @return representación textual protegida de la configuración
   */
  @Override
  public String toString() {
    return "SuperuserProperties[enabled=%s, username=%s, email=%s, password=%s]"
        .formatted(
            this.enabled, describe(this.username), describe(this.email), describe(this.password));
  }

  /**
   * Describe un valor configurado sin revelar su contenido.
   *
   * @param value valor configurado
   * @return indicación de si el valor fue proporcionado
   */
  private static String describe(String value) {
    return value == null || value.isBlank() ? "sin configurar" : "configurado";
  }
}
