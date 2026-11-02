package footballmarket.features.statistics.presentation.dtos.request;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonFormat;
import footballmarket.features.statistics.presentation.dtos.validation.NotFutureUtc;
import footballmarket.features.statistics.presentation.exceptions.InvalidStatisticsRequestException;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import tools.jackson.databind.annotation.JsonDeserialize;

/** Entrada exclusiva del recorrido síncrono de scraping. */
public record PlayerStatisticsSyncRequestDTO(
    @Schema(
            description = "Fecha UTC de extracción; omitida usa la fecha actual",
            example = "2026-10-15")
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
        @JsonDeserialize(using = StatisticsExtractionDateDeserializer.class)
        @NotFutureUtc
        LocalDate extractionDate) {
  /** Rechaza el límite retirado incluso cuando su valor es null. */
  @JsonAnySetter
  public void rejectRetiredField(String name, Object value) {
    if ("maxPlayers".equals(name)) {
      throw new InvalidStatisticsRequestException(
          "INVALID_REQUEST", "La operación ya no admite maxPlayers");
    }
  }
}
