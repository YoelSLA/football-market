package footballmarket.controllers.dtos.requests;

import jakarta.validation.constraints.*;

/** Parámetros opcionales del listado, compatibles con los valores predeterminados anteriores. */
public record PlayerCatalogQueryRequestDTO(
    @Min(0) Integer page,
    @Min(1) @Max(100) Integer size,
    @Size(max = 255) String name,
    @Positive Long leagueId,
    @Positive Long teamId,
    @Size(max = 255) String position,
    @Size(max = 255) String nationality,
    @Pattern(regexp = "id,asc|name,asc|name,desc") String sort,
    Boolean includeStatistics) {}
