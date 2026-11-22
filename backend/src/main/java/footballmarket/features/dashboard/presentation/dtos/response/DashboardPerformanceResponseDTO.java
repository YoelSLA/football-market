package footballmarket.features.dashboard.presentation.dtos.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record DashboardPerformanceResponseDTO(
    @Schema(
            description = "Fechas efectivas de recálculo ordenadas cronológicamente.",
            requiredMode = Schema.RequiredMode.REQUIRED)
        List<LocalDate> dates,
    @Schema(
            description =
                "Serie de valoración del portfolio del usuario en centésimas en cada fecha.",
            requiredMode = Schema.RequiredMode.REQUIRED)
        List<Long> userPortfolioSeries,
    @Schema(
            description =
                "Serie normalizada del índice Football Market Index (FMI base 100) en cada fecha.",
            requiredMode = Schema.RequiredMode.REQUIRED)
        List<BigDecimal> marketBenchmarkSeries) {}
