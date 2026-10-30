package footballmarket.features.identity.presentation.dtos.response;

import io.swagger.v3.oas.annotations.media.Schema;

/** Contadores exclusivos de jugadores, separados de IDs externos, ligas y filas. */
@Schema(
    description =
        "Resultados locales exclusivos; providerErrors cuenta ligas y discardedRows filas, no jugadores")
public record PlayerIdentitySyncCountersResponseDTO(
    long selectedPlayers,
    long discoveredUnique,
    long resolved,
    long alreadyReferenced,
    long conflicts,
    long noMatch,
    long ambiguous,
    long localDataErrors,
    long discoveryErrors,
    long persistenceErrors,
    long interruptedPlayers,
    @Schema(description = "Siempre cero en una respuesta de ejecución terminada")
        long pendingPlayers,
    long providerErrors,
    long discardedRows) {}
