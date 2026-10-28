package footballmarket.features.quote.presentation.dtos.response;

import footballmarket.features.valuation.domain.enums.ValuationStrategy;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Resumen trazable de una corrida de cotizaciones.
 *
 * @param quoteDate fecha de cotización de la corrida: la fecha UTC del día de la ejecución
 * @param strategy estrategia aplicada en la corrida
 * @param strategyVersion versión de la estrategia aplicada
 * @param adjustmentFactor factor de ajuste aplicado
 * @param variationCap tope de variación aplicado
 * @param minPriceFloor piso mínimo de precio aplicado, en centésimas
 * @param pricedPlayers jugadores cotizados en la corrida, incluidos los que no se repreciaron
 * @param recomputedRows filas {@code RECOMPUTED} escritas
 * @param carriedForwardRows filas {@code CARRIED_FORWARD} escritas
 * @param notRepricedPlayers jugadores sin valuación posterior a su fecha de vigencia de referencia
 * @param failedPlayers jugadores con fallo individual; el detalle interno no se expone
 * @param startedAt instante de inicio de la corrida
 * @param finishedAt instante de finalización de la corrida
 */
@Schema(
    name = "QuoteExecutionResponse",
    description = "Resumen persistido de una corrida de cotizaciones")
public record QuoteExecutionResponseDTO(
    @Schema(description = "Fecha de cotización de la corrida", example = "2026-10-20")
        LocalDate quoteDate,
    @Schema(description = "Estrategia aplicada en la corrida", example = "POSITIONAL_PERFORMANCE")
        ValuationStrategy strategy,
    @Schema(description = "Versión de la estrategia aplicada", example = "v1")
        String strategyVersion,
    @Schema(description = "Factor de ajuste aplicado", example = "0.10")
        BigDecimal adjustmentFactor,
    @Schema(description = "Tope de variación aplicado", example = "0.10") BigDecimal variationCap,
    @Schema(description = "Piso mínimo de precio aplicado, en centésimas", example = "1")
        long minPriceFloor,
    @Schema(description = "Jugadores cotizados en la corrida", example = "42") int pricedPlayers,
    @Schema(description = "Filas RECOMPUTED escritas", example = "30") int recomputedRows,
    @Schema(description = "Filas CARRIED_FORWARD escritas", example = "12") int carriedForwardRows,
    @Schema(
            description =
                "Jugadores sin valuación posterior a su fecha de vigencia de referencia, que no "
                    + "se repreciaron",
            example = "3")
        int notRepricedPlayers,
    @Schema(description = "Jugadores con fallo individual", example = "0") int failedPlayers,
    @Schema(description = "Instante de inicio de la corrida", example = "2026-10-20T18:00:00Z")
        Instant startedAt,
    @Schema(
            description = "Instante de finalización de la corrida",
            example = "2026-10-20T18:00:12Z")
        Instant finishedAt) {}
