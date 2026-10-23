package footballmarket.controllers.dtos.responses;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/** Cobertura consultable de una campaña sin acceso al proveedor. */
public record PlayerStatisticsCoverageResponseDTO(
    @Schema(description = "Canal consultado") String source,
    @Schema(description = "Fecha UTC de campaña") LocalDate extractionDate,
    @Schema(description = "Jugadores del catálogo vigente") long catalogPlayers,
    @Schema(description = "Jugadores con referencia confirmada") long eligiblePlayers,
    @Schema(description = "Excluidos por falta de referencia") long excludedWithoutReference,
    @Schema(description = "Jugadores confirmados") long completedPlayers,
    @Schema(description = "Jugadores pendientes, incluidos fallidos") long pendingPlayers,
    @Schema(description = "Jugadores fallidos pendientes") long failedPlayers,
    @Schema(description = "Cobertura completa para fecha y canal") boolean coverageComplete,
    @Schema(description = "Claves sin ninguna observación presente en la población elegible")
        List<String> systemicallyAbsentMetrics,
    @Schema(description = "Última ejecución, o null si no existe") Long lastRunId,
    @Schema(description = "Fin UTC de la última ejecución") Instant lastRunFinishedAt) {
  public PlayerStatisticsCoverageResponseDTO {
    systemicallyAbsentMetrics = List.copyOf(systemicallyAbsentMetrics);
  }
}
