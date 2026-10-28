package footballmarket.features.identity.presentation.dtos.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;

/** Resumen de la ejecución provocada por el POST, sin detalle de auditoría individual. */
@Schema(
    description = "Ejecución terminada; COMPLETED describe finalización, sin umbral de cobertura")
public record PlayerIdentitySyncRunResponseDTO(
    Long id,
    String passType,
    Long sourceRunId,
    Instant startedAt,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Instant finishedAt,
    @Schema(allowableValues = {"COMPLETED", "PARTIAL", "FAILED"}) String status,
    String failureReason,
    PlayerIdentitySyncCountersResponseDTO counters,
    List<PlayerIdentityLeagueDiscoveryResponseDTO> leagues) {}
