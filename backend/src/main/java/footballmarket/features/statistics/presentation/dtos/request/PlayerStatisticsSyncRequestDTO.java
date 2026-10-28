package footballmarket.features.statistics.presentation.dtos.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import footballmarket.features.statistics.presentation.dtos.validation.NotFutureUtc;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Positive;
import java.time.LocalDate;
import tools.jackson.databind.annotation.JsonDeserialize;

/** Entrada exclusiva del lote de scraping. */
public record PlayerStatisticsSyncRequestDTO(
    @Schema(
            description = "Fecha UTC de extracción; omitida usa la fecha actual",
            example = "2026-10-15")
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
        @JsonDeserialize(using = StatisticsExtractionDateDeserializer.class)
        @NotFutureUtc
        LocalDate extractionDate,
    @Schema(
            description = "Máximo global de jugadores del lote; omitido no limita la cantidad",
            example = "1",
            minimum = "1")
        @JsonDeserialize(using = StatisticsMaxPlayersDeserializer.class)
        @Positive(message = "El máximo de jugadores debe ser positivo")
        Integer maxPlayers) {}
