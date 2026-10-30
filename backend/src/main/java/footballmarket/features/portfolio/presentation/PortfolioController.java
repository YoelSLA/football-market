package footballmarket.features.portfolio.presentation;

import footballmarket.common.presentation.dtos.ErrorResponseDTO;
import footballmarket.features.portfolio.application.PortfolioService;
import footballmarket.features.portfolio.presentation.dtos.response.PortfolioResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Frontera HTTP de la consulta del portfolio personal; no ejecuta operaciones de mercado. */
@RestController
@RequestMapping("/api/portfolio")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Mercado")
public class PortfolioController {
  private final PortfolioService portfolioService;

  @Operation(
      summary = "Consultar mi portfolio",
      description =
          "Devuelve el resumen global del portfolio, las posiciones de la página solicitada y los metadatos de esa paginación. La identidad se toma de la sesión: cada usuario consulta únicamente el suyo. El resumen representa siempre el portfolio completo y es independiente del filtro y de la página. Una posición abierta sin cotización vigente se informa con sus campos de cotización nulos y la valoración se marca como incompleta.")
  @ApiResponse(
      responseCode = "200",
      description = "Portfolio del usuario con resumen, posiciones y paginación.")
  @ApiResponse(
      responseCode = "400",
      description = "INVALID_PARAMETER_TYPE: page, size o status inválidos.",
      content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
  @ApiResponse(
      responseCode = "401",
      description = "Sesión ausente, inválida o vencida.",
      content = @Content)
  @ApiResponse(
      responseCode = "500",
      description = "INTERNAL_ERROR: el estado del mercado no permite reconstruir el portfolio.",
      content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
  @ApiResponse(
      responseCode = "503",
      description = "QUOTE_SOURCE_UNAVAILABLE: la fuente de cotizaciones no está disponible.",
      content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
  @GetMapping("/me")
  public PortfolioResponseDTO mine(
      @AuthenticationPrincipal Jwt jwt,
      @Parameter(description = "Número de página desde cero.") @RequestParam(defaultValue = "0")
          int page,
      @Parameter(description = "Tamaño de página, entre 1 y 100.")
          @RequestParam(defaultValue = "20")
          int size,
      @Parameter(description = "Filtro opcional de estado: OPEN o CLOSED.")
          @RequestParam(required = false)
          String status) {
    return PortfolioMapper.toResponse(
        this.portfolioService.portfolio(jwt.getSubject(), page, size, status));
  }
}
