package footballmarket.models.records;

import footballmarket.models.enums.QuoteOrigin;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Entrada del ranking de jugadores por cotización vigente.
 *
 * <p>El precio se expresa en centésimas de crédito y el instante de cálculo es el de la cotización
 * consumida, nunca el de la lectura.
 *
 * @param playerId jugador al que corresponde la cotización vigente
 * @param price precio vigente del jugador en centésimas de crédito
 * @param effectiveDate fecha de vigencia de la cotización vigente
 * @param origin origen de la fila vigente: {@code INITIAL}, {@code RECOMPUTED} o {@code
 *     CARRIED_FORWARD}
 * @param calculatedAt instante de cálculo de la cotización vigente, en UTC
 */
public record PlayerRankingEntry(
    Long playerId, long price, LocalDate effectiveDate, QuoteOrigin origin, Instant calculatedAt) {}
