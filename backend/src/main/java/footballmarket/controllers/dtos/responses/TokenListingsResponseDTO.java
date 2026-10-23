package footballmarket.controllers.dtos.responses;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/** Sobre contractual de listados de mercado y publicaciones propias. */
public record TokenListingsResponseDTO(
    @Schema(
            description =
                "Publicaciones visibles según la consulta; colección vacía si no hay resultados.")
        List<TokenListingSummaryResponseDTO> listings) {}
