package footballmarket.models.records;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Entrada de un ranking de jugadores por score.
 *
 * @param position posición consecutiva desde uno
 * @param playerId identificador interno del jugador
 * @param score resultado final persistido de la estrategia
 * @param valuationDate fecha de la valuación utilizada
 */
public record ScoreRankingEntry(
    Integer position, Long playerId, BigDecimal score, LocalDate valuationDate) {}
