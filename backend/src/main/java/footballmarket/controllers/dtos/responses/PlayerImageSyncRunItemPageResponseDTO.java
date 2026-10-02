package footballmarket.controllers.dtos.responses;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/** Página de resultados individuales ordenados por identificador de item. */
public record PlayerImageSyncRunItemPageResponseDTO(
    List<PlayerImageSyncRunItemResponseDTO> content,
    @Schema(description = "Página desde cero") int page,
    @Schema(description = "Tamaño solicitado", minimum = "1", maximum = "100") int size,
    long totalElements,
    int totalPages) {}
