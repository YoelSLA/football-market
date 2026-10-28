package footballmarket.features.quote.presentation.dtos.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Cotización persistida de un jugador con sus datos de auditoría.
 *
 * @param effectiveDate fecha de vigencia de la cotización
 * @param price precio vigente en centésimas de crédito
 * @param previousPrice precio anterior; nulo solo en la fila inicial
 * @param multiplier multiplicador aplicado, a seis decimales
 * @param score score que sustenta el precio; nulo en las filas inicial y arrastrada
 * @param strategy estrategia que produjo el precio; nula en la fila inicial
 * @param strategyVersion versión de la estrategia; nula en la fila inicial
 * @param sourceValuationDate fecha de la valuación de origen; nula sin valuación
 * @param origin origen de la fila: {@code INITIAL}, {@code RECOMPUTED} o {@code CARRIED_FORWARD}
 * @param calculatedAt instante de cálculo de la cotización, no el de la lectura
 */
@Schema(
    name = "QuoteResponse",
    description = "Cotización persistida de un jugador con sus datos de auditoría")
public record QuoteResponseDTO(
    @Schema(description = "Fecha de vigencia de la cotización", example = "2026-10-20")
        LocalDate effectiveDate,
    @Schema(description = "Precio vigente en centésimas de crédito", example = "105") long price,
    @Schema(
            description = "Precio anterior en centésimas; nulo solo en la fila inicial",
            example = "100",
            nullable = true)
        Long previousPrice,
    @Schema(description = "Multiplicador aplicado, a seis decimales", example = "1.050000")
        BigDecimal multiplier,
    @Schema(
            description = "Score que sustenta el precio; nulo en las filas inicial y arrastrada",
            example = "0.6",
            nullable = true)
        BigDecimal score,
    @Schema(
            description = "Estrategia que produjo el precio; nula en la fila inicial",
            example = "POSITIONAL_PERFORMANCE",
            nullable = true)
        String strategy,
    @Schema(
            description = "Versión de la estrategia; nula en la fila inicial",
            example = "v1",
            nullable = true)
        String strategyVersion,
    @Schema(
            description = "Fecha de la valuación de origen; nula sin valuación",
            example = "2026-10-20",
            nullable = true)
        LocalDate sourceValuationDate,
    @Schema(
            description = "Origen de la fila",
            example = "RECOMPUTED",
            allowableValues = {"INITIAL", "RECOMPUTED", "CARRIED_FORWARD"})
        String origin,
    @Schema(
            description = "Instante de cálculo de la cotización, no el de la lectura",
            example = "2026-10-20T18:00:00Z")
        Instant calculatedAt) {}
