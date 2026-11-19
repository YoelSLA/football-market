package footballmarket.features.market.presentation.dtos.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/** Sobre contractual de listados de mercado y publicaciones propias. */
public record TokenListingsResponseDTO(
    @Schema(
            description =
                "Resumen de métricas del usuario; presente en consultas de publicaciones propias.",
            nullable = true)
        UserOrdersSummaryResponseDTO summary,
    @Schema(
            description =
                "Publicaciones visibles según la consulta; colección vacía si no hay resultados.")
        List<TokenListingSummaryResponseDTO> listings,
    @Schema(
            description =
                "Metadatos de paginación; presente en consultas de publicaciones propias.",
            nullable = true)
        MarketPaginationResponseDTO pagination) {

  public TokenListingsResponseDTO(List<TokenListingSummaryResponseDTO> listings) {
    this(null, listings, null);
  }
}
