package footballmarket.models.records;

import java.time.LocalDate;

/**
 * Resultado observable de la inicialización del mercado.
 *
 * @param marketDate fecha de mercado vigente
 * @param initializedPlayers jugadores cotizables con fila inicial vigente
 * @param alreadyInitialized {@code true} cuando el mercado ya estaba inicializado en esa fecha y la
 *     operación no escribió filas
 */
public record MarketInitializationResult(
    LocalDate marketDate, int initializedPlayers, boolean alreadyInitialized) {}
