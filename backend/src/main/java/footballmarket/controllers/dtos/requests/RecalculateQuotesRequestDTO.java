package footballmarket.controllers.dtos.requests;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;

/** Parámetros ajustables de una ejecución manual de cotizaciones. */
@Schema(
    name = "RecalculateQuotesRequest",
    description = "Parámetros de una ejecución manual de cotizaciones")
public record RecalculateQuotesRequestDTO(
    @Schema(
            description =
                "Estrategia del catálogo de estrategias: POSITIONAL_PERFORMANCE o GENERAL_ATTRIBUTES",
            example = "POSITIONAL_PERFORMANCE",
            requiredMode = Schema.RequiredMode.REQUIRED)
        String strategy,
    @Schema(
            description = "Factor de ajuste k, en el rango cerrado [0,10; 1,00]; por defecto 0,10",
            example = "0.10",
            minimum = "0.10",
            maximum = "1.00")
        BigDecimal adjustmentFactor,
    @Schema(
            description =
                "Tope de variación del período, como fracción del precio vigente, en el rango "
                    + "cerrado (0; 1]; por defecto 0,10",
            example = "0.10",
            minimum = "0",
            maximum = "1")
        BigDecimal variationCap,
    @Schema(
            description =
                "Piso mínimo de precio en centésimas, siempre mayor que cero; por defecto 1",
            example = "1",
            minimum = "1")
        Long minPriceFloor) {}
