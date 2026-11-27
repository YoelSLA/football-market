package footballmarket.features.businessmetrics.presentation.dtos.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

/** Respuesta agregada común para las consultas de métricas de negocio. */
public record BusinessMetricsResponseDTO(
    @Schema(description = "Conteo de operaciones financieras clasificadas como compras.")
        long purchases,
    @Schema(description = "Conteo de operaciones financieras clasificadas como ventas.") long sales,
    @Schema(description = "Publicaciones que alcanzaron el estado CANCELLED.")
        long cancelledListings,
    @Schema(description = "Volumen numérico acumulado de tokens transferidos.")
        long tokensTransferred,
    @Schema(description = "Volumen de créditos negociados, expresado en centésimas de crédito.")
        long creditsTradedInCents,
    @Schema(description = "Instante de consulta en formato ISO 8601 UTC.") Instant queriedAt) {}
