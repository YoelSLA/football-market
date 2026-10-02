package footballmarket.services.internal;

/**
 * Resultado parcial de la sincronización de candidatos.
 *
 * @param created cantidad de jugadores creados
 * @param updated cantidad de jugadores actualizados
 */
public record SynchronizationCounters(int created, int updated) {}
