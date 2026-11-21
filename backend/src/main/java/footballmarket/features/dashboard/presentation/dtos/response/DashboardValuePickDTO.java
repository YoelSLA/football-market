package footballmarket.features.dashboard.presentation.dtos.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;

public record DashboardValuePickDTO(
    @Schema(
            description = "Identificador del jugador.",
            example = "305",
            requiredMode = Schema.RequiredMode.REQUIRED)
        Long playerId,
    @Schema(
            description = "Nombre del jugador.",
            example = "Florian",
            requiredMode = Schema.RequiredMode.REQUIRED)
        String firstName,
    @Schema(
            description = "Apellido del jugador.",
            example = "Wirtz",
            requiredMode = Schema.RequiredMode.REQUIRED)
        String lastName,
    @Schema(
            description = "Nombre del equipo.",
            example = "Bayer Leverkusen",
            requiredMode = Schema.RequiredMode.REQUIRED)
        String teamName,
    @Schema(
            description = "Nombre de la liga.",
            example = "Bundesliga",
            requiredMode = Schema.RequiredMode.REQUIRED)
        String leagueName,
    @Schema(
            description = "Posición deportiva normalizada.",
            example = "MEDIOCAMPISTA",
            requiredMode = Schema.RequiredMode.REQUIRED)
        String position,
    @Schema(
            description = "URL de la imagen del jugador.",
            example = "https://media.footballmarket.com/players/wirtz.png",
            nullable = true)
        String imageUrl,
    @Schema(
            description = "Score WhoScored de rendimiento.",
            example = "8.25",
            requiredMode = Schema.RequiredMode.REQUIRED)
        BigDecimal whoScoredScore,
    @Schema(
            description = "Cotización oficial actual en centésimas.",
            example = "12000",
            requiredMode = Schema.RequiredMode.REQUIRED)
        long currentPrice,
    @Schema(
            description = "Ratio de valor deportivo sobre precio.",
            example = "68.75",
            requiredMode = Schema.RequiredMode.REQUIRED)
        BigDecimal valueRatio) {}
