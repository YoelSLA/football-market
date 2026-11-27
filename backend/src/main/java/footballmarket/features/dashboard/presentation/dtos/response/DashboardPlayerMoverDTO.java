package footballmarket.features.dashboard.presentation.dtos.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;

public record DashboardPlayerMoverDTO(
    @Schema(
            description = "Identificador del jugador.",
            example = "101",
            requiredMode = Schema.RequiredMode.REQUIRED)
        Long playerId,
    @Schema(
            description = "Nombre del jugador.",
            example = "Kylian",
            requiredMode = Schema.RequiredMode.REQUIRED)
        String firstName,
    @Schema(
            description = "Apellido del jugador.",
            example = "Mbappé",
            requiredMode = Schema.RequiredMode.REQUIRED)
        String lastName,
    @Schema(
            description = "Nombre del equipo.",
            example = "Real Madrid",
            requiredMode = Schema.RequiredMode.REQUIRED)
        String teamName,
    @Schema(
            description = "Nombre de la liga.",
            example = "La Liga",
            requiredMode = Schema.RequiredMode.REQUIRED)
        String leagueName,
    @Schema(
            description = "Posición deportiva normalizada.",
            example = "DELANTERO",
            requiredMode = Schema.RequiredMode.REQUIRED)
        String position,
    @Schema(
            description = "URL de la imagen del jugador.",
            example = "https://media.footballmarket.com/players/mbappe.png",
            nullable = true)
        String imageUrl,
    @Schema(
            description = "Cotización oficial actual en centésimas.",
            example = "24500",
            requiredMode = Schema.RequiredMode.REQUIRED)
        long currentPrice,
    @Schema(
            description = "Cotización anterior inmediata en centésimas.",
            example = "21000",
            nullable = true)
        Long previousPrice,
    @Schema(description = "Variación nominal en centésimas.", example = "3500", nullable = true)
        Long deltaCredits,
    @Schema(
            description = "Variación porcentual respecto a la cotización anterior.",
            example = "16.67",
            nullable = true)
        BigDecimal deltaPercentage,
    @Schema(
            description = "Score WhoScored de la última evaluación.",
            example = "8.75",
            nullable = true)
        BigDecimal score) {}
