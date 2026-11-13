package footballmarket.features.market.presentation;

import footballmarket.common.presentation.dtos.ErrorResponseDTO;
import footballmarket.features.market.application.OfficialMarketCatalogService;
import footballmarket.features.market.application.OfficialMarketQuery;
import footballmarket.features.market.domain.records.OfficialMarketPlayer;
import footballmarket.features.market.presentation.dtos.request.OfficialMarketQueryRequestDTO;
import footballmarket.features.market.presentation.dtos.response.OfficialMarketPageResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controlador REST para la consulta del catálogo del Mercado Oficial con cotizaciones integradas.
 */
@RestController
@RequestMapping("/api/market/official")
@RequiredArgsConstructor
@Tag(
    name = "Mercado Oficial",
    description = "Catálogo primario de emisión con cotizaciones consolidadas")
@SecurityScheme(
    name = "bearerAuth",
    type = SecuritySchemeType.HTTP,
    scheme = "bearer",
    bearerFormat = "JWT")
@SecurityRequirement(name = "bearerAuth")
public class OfficialMarketController {

  private final OfficialMarketCatalogService officialMarketCatalogService;

  /**
   * Consulta paginada del catálogo de emisión oficial del mercado primario.
   *
   * @param requestDTO parámetros de filtrado, ordenamiento y paginación
   * @param request petición HTTP en curso
   * @return catálogo paginado con identidad deportiva y cotizaciones vigentes
   */
  @GetMapping
  @Operation(
      summary = "Consultar catálogo del Mercado Oficial",
      description =
          "Consulta paginada de jugadores elegibles para emisión con su cotización consolidada. Admite filtros deportivos (name con trim, leagueId, teamId, position, nationality), disponibilidad de cotización (hasQuote), rangos de precio (minPrice, maxPrice) y ordenamiento con NULLS LAST y desempate por id ASC. No expone stock ni balance del superusuario.")
  @ApiResponse(
      responseCode = "200",
      description = "Catálogo paginado obtenido exitosamente",
      content = @Content(schema = @Schema(implementation = OfficialMarketPageResponseDTO.class)))
  @ApiResponse(
      responseCode = "400",
      description =
          "Parámetros de consulta o rangos inválidos (INVALID_PRICE_RANGE, INVALID_PAGINATION, INVALID_PAGE_SIZE, INVALID_SORT_PARAMETER, INVALID_PARAMETER_TYPE)",
      content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
  @ApiResponse(
      responseCode = "401",
      description = "No autenticado o token expirado",
      content = @Content)
  @ApiResponse(
      responseCode = "500",
      description = "Error interno del servidor",
      content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
  public ResponseEntity<OfficialMarketPageResponseDTO> getOfficialCatalog(
      @Valid @ModelAttribute OfficialMarketQueryRequestDTO requestDTO, HttpServletRequest request) {
    String[] rawSort = request.getParameterValues("sort");
    OfficialMarketQuery query = OfficialMarketMapper.toQuery(requestDTO, rawSort);
    Page<OfficialMarketPlayer> pagedPlayers =
        this.officialMarketCatalogService.getOfficialCatalog(query);
    return ResponseEntity.ok(OfficialMarketMapper.toResponse(pagedPlayers));
  }
}
