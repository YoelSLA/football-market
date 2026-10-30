package footballmarket.features.identity.presentation.dtos.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

/** Estado y metadatos del ámbito capturado; totales no observados permanecen nulos. */
@Schema(description = "Ámbito local y competición externa; rowsRead cuenta filas originales")
public record PlayerIdentityLeagueDiscoveryResponseDTO(
    Long leagueId,
    String leagueName,
    int tournamentId,
    String status,
    Instant startedAt,
    Instant finishedAt,
    long pagesRead,
    Integer lastPage,
    Integer totalPages,
    Long totalResults,
    long rowsRead,
    long discardedRows,
    String errorCode,
    String errorMessage,
    Integer failedPage,
    Integer httpStatus) {}
