package footballmarket.features.market.presentation.dtos.response;

import io.swagger.v3.oas.annotations.media.Schema;

/** Resumen de métricas consolidadas de las órdenes de venta del usuario. */
public record UserOrdersSummaryResponseDTO(
    @Schema(description = "Cantidad de órdenes activas.", example = "5") int activeOrders,
    @Schema(
            description = "Cantidad de tokens actualmente en venta en órdenes activas.",
            example = "32")
        int activeTokens,
    @Schema(
            description = "Valor total en centésimas de crédito de las órdenes activas.",
            example = "4850")
        long activeValue,
    @Schema(description = "Cantidad de órdenes vendidas.", example = "14") int soldOrders,
    @Schema(description = "Cantidad de tokens liquidados en órdenes vendidas.", example = "84")
        int soldTokens,
    @Schema(
            description = "Valor total en centésimas de crédito de las órdenes vendidas.",
            example = "12450")
        long soldValue,
    @Schema(description = "Cantidad de órdenes canceladas.", example = "3") int cancelledOrders,
    @Schema(description = "Cantidad de órdenes expiradas.", example = "2") int expiredOrders,
    @Schema(description = "Total de órdenes registradas por el usuario.", example = "24")
        int totalOrders) {}
