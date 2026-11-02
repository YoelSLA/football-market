package footballmarket.features.catalog.presentation.dtos.response;

import java.util.List;

/** Mismos metadatos del listado legado, con entradas opcionalmente enriquecidas. */
public record PlayerCatalogPageResponseDTO(
    List<PlayerCatalogEntryResponseDTO> content,
    int page,
    int size,
    long totalElements,
    int totalPages) {}
