package footballmarket.controllers.dtos.responses;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDate;

/** Identidad compatible y resumen adicional solo cuando se solicita explícitamente. */
@Schema(description = "Identidad visible original; statistics se omite sin includeStatistics=true")
public record PlayerCatalogEntryResponseDTO(
    Long id,
    String name,
    Long teamId,
    String teamName,
    Long leagueId,
    String leagueName,
    String position,
    LocalDate dateOfBirth,
    String nationality,
    String imageUrl,
    String fallbackImageUrl,
    @JsonInclude(JsonInclude.Include.NON_NULL) @Schema(nullable = true) Statistics statistics) {
  /** Valores originales nullable: no se imputa cero cuando no hay estadística. */
  @Schema(
      description =
          "Tres valores originales de la misma extracción confirmada. Null indica ausencia, cero es dato válido")
  public record Statistics(
      String seasonKey,
      String seasonLabel,
      LocalDate extractionDate,
      String competitionKey,
      BigDecimal appearancesTotal,
      BigDecimal goalsTotal,
      BigDecimal assistsTotal,
      Sources sources) {}

  /** Fuente por valor; null cuando ese valor no fue publicado. */
  public record Sources(String appearancesTotal, String goalsTotal, String assistsTotal) {}
}
