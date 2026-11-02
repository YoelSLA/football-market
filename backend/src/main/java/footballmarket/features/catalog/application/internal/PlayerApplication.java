package footballmarket.features.catalog.application.internal;

/**
 * Resultado de aplicar los jugadores de una foto.
 *
 * @param created jugadores dados de alta
 * @param updated jugadores existentes actualizados
 * @param inactive jugadores desactivados por ausencia
 * @param discarded candidatos no aplicados, por conflicto o por asociación irresoluble
 */
public record PlayerApplication(int created, int updated, int inactive, int discarded) {}
