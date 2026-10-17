package footballmarket.services;

import footballmarket.models.User;
import java.util.Optional;

/**
 * Contrato del aprovisionamiento interno de la cuenta singular SUPERUSER del dominio de mercado.
 *
 * <p>El SUPERUSER es el dueño inicial de la totalidad de los tokens de los jugadores. No se crea
 * mediante el registro público ni desde una interfaz HTTP.
 */
public interface SuperuserProvisioningService {

  /**
   * Asegura que exista exactamente un usuario con rol SUPERUSER y lo devuelve.
   *
   * <p>La operación es idempotente: si el SUPERUSER ya existe y corresponde a la configuración, no
   * crea usuarios adicionales ni modifica la cuenta existente, y devuelve esa misma cuenta. Si el
   * estado persistido es incompatible con la configuración, falla de forma explícita en lugar de
   * crear una segunda cuenta o alterar la existente.
   *
   * @return usuario con rol SUPERUSER
   * @throws footballmarket.services.exceptions.SuperuserProvisioningException si el estado
   *     persistido es incompatible con la configuración de arranque
   */
  User provisionSuperuser();

  /**
   * Recupera el usuario con rol SUPERUSER para las operaciones posteriores del mercado.
   *
   * @return usuario con rol SUPERUSER, o vacío si todavía no fue aprovisionado
   */
  Optional<User> findSuperuser();
}
