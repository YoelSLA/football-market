package footballmarket.features.portfolio.presentation.dtos.response;

import footballmarket.features.portfolio.domain.records.OperationHistoryEntry.OperationType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;
import java.util.List;

/** Historial personal paginado, con agregados del conjunto filtrado. */
public record OperationHistoryResponseDTO(
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) SummaryDTO summary,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) List<TransactionDTO> transactions,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) PaginationDTO pagination) {

  /** Indicadores del conjunto filtrado completo. */
  public record SummaryDTO(
      long totalTransactions,
      long buyTransactions,
      long sellTransactions,
      long boughtTokens,
      long soldTokens,
      @Schema(description = "Volumen comprado en centésimas de crédito.") long purchasedVolume,
      @Schema(description = "Volumen vendido en centésimas de crédito.") long soldVolume) {}

  /** Transacción confirmada desde la perspectiva del usuario autenticado. */
  public record TransactionDTO(
      Long transactionId,
      @Schema(description = "BUY o SELL desde la perspectiva de la sesión.") OperationType type,
      @Schema(description = "Perfil actual; no es una fotografía histórica.") PlayerDTO player,
      int quantity,
      @Schema(description = "Precio histórico en centésimas de crédito.") long unitPrice,
      @Schema(description = "Importe histórico en centésimas de crédito.") long totalAmount,
      OffsetDateTime executedAt) {}

  /** Perfil actual del jugador asociado. */
  public record PlayerDTO(
      Long id,
      String name,
      @Schema(nullable = true) String imageUrl,
      @Schema(nullable = true) String fallbackImageUrl,
      RelatedDTO team,
      RelatedDTO league) {}

  /** Identidad actual de equipo o liga. */
  public record RelatedDTO(Long id, String name) {}

  /** Metadatos de paginación. */
  public record PaginationDTO(int page, int size, long totalElements, int totalPages) {}
}
