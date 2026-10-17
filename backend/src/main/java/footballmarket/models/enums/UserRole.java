package footballmarket.models.enums;

/** Rol que el usuario ostenta dentro del sistema. */
public enum UserRole {

  /** Usuario registrado públicamente. Es el rol que recibe toda alta del registro público. */
  USER,

  /**
   * Dueño inicial de la totalidad de los tokens de los jugadores.
   *
   * <p>Es una capacidad singular del dominio de mercado y no equivale a una administración general
   * del sistema.
   */
  SUPERUSER
}
