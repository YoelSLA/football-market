package footballmarket.security;

/**
 * Contrato para verificar la existencia de una identidad persistida correspondiente al sujeto de
 * una autenticación JWT.
 */
@FunctionalInterface
public interface PersistedIdentityValidator {

  /**
   * Comprueba si el sujeto corresponde a una identidad persistida válida.
   *
   * @param subject sujeto del token JWT (correo electrónico)
   * @return {@code true} si la identidad existe; {@code false} si es nula, vacía o no existe
   */
  boolean exists(String subject);
}
