package footballmarket.features.portfolio.presentation.dtos.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * Posición del portfolio de un jugador.
 *
 * <p>Los importes son enteros en centésimas de crédito. Los campos dependientes de la cotización
 * son nulos cuando no hay cotización vigente, y el precio medio de adquisición es nulo en una
 * posición cerrada, porque no hay cantidad abierta que lo defina.
 */
public record PortfolioPositionResponseDTO(
    @Schema(
            description = "Identificador interno del jugador.",
            example = "41",
            requiredMode = Schema.RequiredMode.REQUIRED)
        Long playerId,
    @Schema(
            description = "Nombre del jugador; nulo si ya no es recuperable del catálogo.",
            example = "Joel Robles",
            nullable = true,
            requiredMode = Schema.RequiredMode.REQUIRED)
        String playerName,
    @Schema(
            description =
                "Nombre del equipo del jugador; nulo si ya no es recuperable del catálogo.",
            example = "Real Betis Balompié",
            nullable = true,
            requiredMode = Schema.RequiredMode.REQUIRED)
        String teamName,
    @Schema(
            description =
                "Posición deportiva del jugador; nula si ya no es recuperable del catálogo.",
            example = "Goalkeeper",
            nullable = true,
            requiredMode = Schema.RequiredMode.REQUIRED)
        String position,
    @Schema(
            description = "Imagen persistida del jugador; nula si no existe.",
            example = "https://example.org/player/41.png",
            nullable = true,
            requiredMode = Schema.RequiredMode.REQUIRED)
        String imageUrl,
    @Schema(
            description = "Estado activo del jugador en el catálogo.",
            example = "true",
            requiredMode = Schema.RequiredMode.REQUIRED)
        boolean active,
    @Schema(
            description = "Estado de la posición: OPEN o CLOSED.",
            example = "OPEN",
            requiredMode = Schema.RequiredMode.REQUIRED)
        String status,
    @Schema(
            description = "Estado de valoración: VALUED, UNVALUED o NOT_APPLICABLE.",
            example = "VALUED",
            requiredMode = Schema.RequiredMode.REQUIRED)
        String valuationStatus,
    @Schema(
            description = "Tokens poseídos del jugador.",
            example = "60",
            requiredMode = Schema.RequiredMode.REQUIRED)
        int quantity,
    @Schema(
            description = "Tokens reservados por publicaciones activas y vigentes.",
            example = "20",
            requiredMode = Schema.RequiredMode.REQUIRED)
        int reservedQuantity,
    @Schema(
            description = "Tokens disponibles: poseídos menos reservados.",
            example = "40",
            requiredMode = Schema.RequiredMode.REQUIRED)
        int availableQuantity,
    @Schema(
            description = "Costo de adquisición de la posición abierta, en centésimas de crédito.",
            example = "6000",
            requiredMode = Schema.RequiredMode.REQUIRED)
        long acquisitionCost,
    @Schema(
            description =
                "Precio medio de adquisición, en centésimas de crédito; nulo en una posición cerrada.",
            example = "100",
            nullable = true,
            requiredMode = Schema.RequiredMode.REQUIRED)
        Long averageUnitPrice,
    @Schema(
            description =
                "Cotización vigente por token, en centésimas de crédito; nula sin cotización o en una posición cerrada.",
            example = "125",
            nullable = true,
            requiredMode = Schema.RequiredMode.REQUIRED)
        Long currentUnitPrice,
    @Schema(
            description = "Instante de cálculo de la cotización vigente.",
            example = "2026-10-20T14:00:00Z",
            nullable = true,
            requiredMode = Schema.RequiredMode.REQUIRED)
        OffsetDateTime priceUpdatedAt,
    @Schema(
            description =
                "Valor actual de la posición, en centésimas de crédito; nulo sin cotización o en una posición cerrada.",
            example = "7500",
            nullable = true,
            requiredMode = Schema.RequiredMode.REQUIRED)
        Long currentValue,
    @Schema(
            description = "Resultado realizado acumulado del jugador, en centésimas de crédito.",
            example = "0",
            requiredMode = Schema.RequiredMode.REQUIRED)
        long realizedProfitLoss,
    @Schema(
            description =
                "Resultado no realizado de la posición, en centésimas de crédito; nulo sin cotización o en una posición cerrada.",
            example = "1500",
            nullable = true,
            requiredMode = Schema.RequiredMode.REQUIRED)
        Long unrealizedProfitLoss,
    @Schema(
            description =
                "Resultado total de la posición, en centésimas de crédito; nulo sin cotización; en una posición cerrada, el realizado acumulado.",
            example = "1500",
            nullable = true,
            requiredMode = Schema.RequiredMode.REQUIRED)
        Long totalProfitLoss,
    @Schema(
            description =
                "Rendimiento no realizado respecto del costo de adquisición, en puntos porcentuales con dos decimales; nulo sin cotización o con costo de adquisición cero.",
            example = "25.00",
            nullable = true,
            requiredMode = Schema.RequiredMode.REQUIRED)
        BigDecimal unrealizedProfitLossPercentage,
    @Schema(
            description =
                "Peso de la posición sobre el valor total del portfolio, en puntos porcentuales con dos decimales; nulo si el valor total no está disponible o es cero.",
            example = "55.56",
            nullable = true,
            requiredMode = Schema.RequiredMode.REQUIRED)
        BigDecimal portfolioWeightPercentage,
    @Schema(
            description = "Inicio del ciclo de tenencia vigente.",
            example = "2026-10-18T10:12:00Z",
            requiredMode = Schema.RequiredMode.REQUIRED)
        OffsetDateTime openedAt,
    @Schema(
            description =
                "Venta que llevó la tenencia a cero; nula mientras la posición esté abierta.",
            example = "2026-10-19T09:00:00Z",
            nullable = true,
            requiredMode = Schema.RequiredMode.REQUIRED)
        OffsetDateTime closedAt,
    @Schema(
            description =
                "Instante de la última operación confirmada del usuario sobre el jugador; nula si no tiene ninguna.",
            example = "2026-10-18T10:12:00Z",
            nullable = true,
            requiredMode = Schema.RequiredMode.REQUIRED)
        OffsetDateTime lastTransactionAt,
    @Schema(
            description =
                "Operaciones confirmadas en las que el usuario es comprador sobre este jugador.",
            example = "2",
            requiredMode = Schema.RequiredMode.REQUIRED)
        long totalBuyTransactions,
    @Schema(
            description =
                "Operaciones confirmadas en las que el usuario es vendedor sobre este jugador.",
            example = "0",
            requiredMode = Schema.RequiredMode.REQUIRED)
        long totalSellTransactions) {}
