package footballmarket.features.statistics.presentation.dtos.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.time.LocalDate;

/** Resumen HTTP de una ejecución, sin valores individuales del proveedor. */
public record PlayerStatisticsSyncResponseDTO(
    @Schema(description = "Identificador de ejecución") Long runId,
    @Schema(
            description = "Canal ejecutado",
            allowableValues = {"SCRAPING", "JSON"})
        String source,
    @Schema(description = "Fecha UTC de campaña") LocalDate extractionDate,
    @Schema(description = "Resultado de la ejecución") String status,
    @Schema(description = "Inicio UTC") Instant startedAt,
    @Schema(description = "Fin UTC") Instant finishedAt,
    @Schema(description = "Contadores de población y resultados") Players players,
    @Schema(description = "Conteos agregados de disponibilidad") Metrics metrics,
    @Schema(description = "Navegaciones o consultas externas") long providerCalls,
    @Schema(description = "No quedan elegibles pendientes para fecha y canal")
        boolean coverageComplete) {
  /** Contadores de jugadores. */
  public record Players(
      long eligible,
      long excludedWithoutReference,
      long selected,
      long completed,
      long withoutData,
      long failed,
      long pending) {}

  /** Disponibilidad agregada de métricas. */
  public record Metrics(long present, long absent, long notDerivable) {}
}
