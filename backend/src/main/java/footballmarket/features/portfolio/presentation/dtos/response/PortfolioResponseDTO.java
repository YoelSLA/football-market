package footballmarket.features.portfolio.presentation.dtos.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * Portfolio personal del usuario autenticado.
 *
 * <p>El resumen representa el portfolio completo; las posiciones corresponden a la página
 * solicitada y la paginación describe esa página.
 */
public record PortfolioResponseDTO(
    @Schema(
            description =
                "Indicadores globales del portfolio, independientes del filtro y de la página.",
            requiredMode = Schema.RequiredMode.REQUIRED)
        PortfolioSummaryResponseDTO summary,
    @Schema(
            description = "Posiciones del portfolio en la página solicitada.",
            requiredMode = Schema.RequiredMode.REQUIRED)
        List<PortfolioPositionResponseDTO> positions,
    @Schema(
            description = "Metadatos de la página solicitada.",
            requiredMode = Schema.RequiredMode.REQUIRED)
        PortfolioPaginationResponseDTO pagination) {}
