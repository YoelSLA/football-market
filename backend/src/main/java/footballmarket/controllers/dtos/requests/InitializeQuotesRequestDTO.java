package footballmarket.controllers.dtos.requests;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;

/**
 * Solicitud de inicialización del mercado.
 *
 * @param marketDate fecha de vigencia de las filas iniciales; si se omite se usa la fecha UTC del
 *     día en que corre la operación
 */
@Schema(
    name = "InitializeQuotesRequest",
    description = "Inicialización del mercado de cotizaciones")
public record InitializeQuotesRequestDTO(
    @Schema(
            description =
                "Fecha de mercado. Si se omite se usa la fecha UTC del día en que corre la operación",
            example = "2026-10-20",
            nullable = true)
        LocalDate marketDate) {}
