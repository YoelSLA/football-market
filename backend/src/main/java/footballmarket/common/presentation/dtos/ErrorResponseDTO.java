package footballmarket.common.presentation.dtos;

import java.time.LocalDateTime;

public record ErrorResponseDTO(
    LocalDateTime timestamp, int status, String error, String code, String message, String path) {}
