package footballmarket.features.market.presentation.dtos.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Datos de la cotización vigente consolidada en el catálogo del Mercado Oficial.
 *
 * @param price precio unitario vigente en centésimas de crédito
 * @param previousPrice precio unitario de la cotización anterior en centésimas
 * @param variationPercentage variación porcentual redondeada a 2 decimales
 * @param performanceScore score técnico de rendimiento
 * @param multiplier multiplicador de valuación aplicado
 * @param calculationOrigin origen o disparador del cálculo
 * @param effectiveDate fecha representativa de vigencia (YYYY-MM-DD)
 * @param calculatedAt instante exacto UTC en que se persistió el cálculo
 */
@Schema(
    name = "OfficialMarketQuoteResponse",
    description = "Cotización vigente consolidada del jugador en el Mercado Oficial")
public record OfficialMarketQuoteResponseDTO(
    @Schema(description = "Precio unitario vigente en centésimas de crédito", example = "25000")
        long price,
    @Schema(
            description = "Precio unitario anterior en centésimas de crédito",
            example = "24000",
            nullable = true)
        Long previousPrice,
    @Schema(
            description =
                "Variación porcentual calculada contra el precio anterior. Nulo si no existe precio previo válido",
            example = "4.17",
            nullable = true)
        Double variationPercentage,
    @Schema(description = "Score técnico de rendimiento que originó la valuación", example = "8.92")
        double performanceScore,
    @Schema(description = "Multiplicador de valuación aplicado", example = "1.25")
        double multiplier,
    @Schema(
            description = "Origen o disparador del cálculo",
            example = "MANUAL_RECALCULATION",
            allowableValues = {
              "INITIAL",
              "RECOMPUTED",
              "CARRIED_FORWARD",
              "MANUAL_RECALCULATION",
              "SCHEDULED_TASK",
              "INITIAL_PROVISIONING"
            })
        String calculationOrigin,
    @Schema(description = "Fecha de vigencia de la cotización", example = "2026-11-12")
        LocalDate effectiveDate,
    @Schema(
            description = "Instante exacto UTC en que se persistió el cálculo",
            example = "2026-11-12T15:30:00Z")
        Instant calculatedAt) {}
