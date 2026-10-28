package footballmarket.controllers.dtos.responses;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Contrato aditivo de lectura de las sesenta métricas canónicas de un jugador visible. */
@Schema(description = "Snapshot individual confirmado; ausencia no equivale a cero")
public record PlayerStatisticsResponseDTO(
    long playerId,
    @Schema(nullable = true) String requestedSeasonKey,
    @Schema(allowableValues = {"AVAILABLE", "PARTIAL", "NO_DATA"}) String status,
    @Schema(nullable = true) String seasonKey,
    @Schema(nullable = true) String seasonLabel,
    @Schema(nullable = true) LocalDate extractionDate,
    @Schema(nullable = true) String competitionKey,
    @Schema(nullable = true) String competitionName,
    @Schema(description = "Exactamente 60 claves únicas, en orden de MetricCatalog")
        List<MetricDTO> metrics) {
  /** Unidad técnica, valor original y canal efectivo; source es null cuando value es null. */
  public record MetricDTO(
      String key,
      @Schema(allowableValues = {"SEASON_TOTAL", "PER_MATCH", "PERCENTAGE", "AVERAGE_RATING"})
          String unit,
      @Schema(nullable = true) BigDecimal value,
      @Schema(
              nullable = true,
              allowableValues = {"JSON", "SCRAPING"})
          String source) {}
}
