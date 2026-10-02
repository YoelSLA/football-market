package footballmarket.controllers.dtos.responses;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/** Página de resúmenes sin detalles individuales. */
public record PlayerImageSyncRunPageResponseDTO(
    List<PlayerImageSyncRunResponseDTO> content,
    @Schema(description = "Página desde cero") int page,
    @Schema(description = "Tamaño solicitado", minimum = "1", maximum = "100") int size,
    long totalElements,
    int totalPages) {}
