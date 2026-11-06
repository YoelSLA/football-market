package footballmarket.features.auth.domain.records;

import footballmarket.features.auth.domain.enums.UserRole;

/**
 * Vista de dominio con la identidad y el estado actual del usuario autenticado.
 *
 * @param id identificador persistido del usuario
 * @param username nombre de usuario normalizado
 * @param email correo electrónico normalizado
 * @param role capacidad o rol en el sistema
 * @param availableCredits saldo disponible en centésimas de crédito
 */
public record CurrentUserView(
    Long id, String username, String email, UserRole role, long availableCredits) {}
