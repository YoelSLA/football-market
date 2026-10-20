package footballmarket.controllers.dtos.responses;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.util.Map;

/** Cobertura agregada de una evaluación estadística poblacional. */
public record ValuationEvaluationResponseDTO(
    @Schema(description = "Fecha UTC fijada al iniciar la evaluación") LocalDate valuationDate,
    @Schema(description = "Jugadores con ambas estrategias publicadas") long evaluatedPlayers,
    @Schema(description = "Jugadores con fallos inesperados") long failedPlayers,
    @Schema(description = "Conteos por motivo funcional de exclusión")
        Map<String, Long> excludedPlayers,
    @Schema(description = "Conteos de métricas derivadas imputadas")
        Map<String, Long> imputedMetricCounts) {}
