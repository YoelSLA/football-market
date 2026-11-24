package footballmarket.features.market.presentation;

import footballmarket.common.presentation.dtos.ErrorResponseDTO;
import footballmarket.features.market.application.MarketTrendsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** Expone la consulta autenticada de tendencias agregadas del mercado. */
@RestController
@RequiredArgsConstructor
@Tag(name = "Tendencias del mercado", description = "Consulta agregada de actividad y sentimiento")
@SecurityRequirement(name = "bearerAuth")
public class MarketTrendsController {
  private final MarketTrendsService marketTrendsService;

  /** Devuelve las métricas de mercado de la ventana configurada. */
  @GetMapping("/api/market/trends")
  @Operation(
      summary = "Consultar tendencias del mercado",
      description =
          "Devuelve hasta cinco jugadores con mayor volumen de compra, porcentajes de compras y"
              + " ventas, sesgo, volumen total y período observado. Requiere JWT válido.")
  @ApiResponse(
      responseCode = "200",
      description = "Tendencias obtenidas; porcentajes y sesgo son nulos cuando no hay actividad",
      content = @Content(schema = @Schema(implementation = MarketTrendsResponseDTO.class)))
  @ApiResponse(
      responseCode = "401",
      description = "JWT ausente, inválido o identidad no persistida")
  @ApiResponse(
      responseCode = "500",
      description = "Error interno sin detalles de persistencia",
      content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
  public ResponseEntity<MarketTrendsResponseDTO> getMarketTrends() {
    return ResponseEntity.ok(
        MarketTrendsMapper.toResponse(this.marketTrendsService.getMarketTrends()));
  }
}
