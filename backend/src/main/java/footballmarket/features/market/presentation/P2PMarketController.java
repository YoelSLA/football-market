package footballmarket.features.market.presentation;

import footballmarket.common.presentation.dtos.ErrorResponseDTO;
import footballmarket.features.market.application.P2PMarketCatalogService;
import footballmarket.features.market.application.P2PMarketQuery;
import footballmarket.features.market.domain.records.P2PMarketCatalog;
import footballmarket.features.market.presentation.dtos.request.P2PMarketQueryRequestDTO;
import footballmarket.features.market.presentation.dtos.response.P2PMarketPageResponseDTO;
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
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Controlador REST para la consulta paginada del catálogo del Mercado P2P de la comunidad. */
@RestController
@RequestMapping("/api/market/p2p")
@RequiredArgsConstructor
@Tag(
    name = "Mercado P2P",
    description =
        "Catálogo de publicaciones comunitarias agrupadas por jugador con paginación en servidor")
@SecurityScheme(
    name = "bearerAuth",
    type = SecuritySchemeType.HTTP,
    scheme = "bearer",
    bearerFormat = "JWT")
@SecurityRequirement(name = "bearerAuth")
public class P2PMarketController {

  private final P2PMarketCatalogService p2pMarketCatalogService;

  /** Consulta paginada de jugadores con ofertas activas en el Mercado P2P. */
  @GetMapping
  @Operation(
      summary = "Consultar catálogo del Mercado P2P",
      description =
          "Consulta paginada de jugadores con ofertas activas de la comunidad. Admite filtros por búsqueda de texto (jugador o club), liga, equipo, posición, rangos de precio unitario y ordenamiento por precio o cantidad.")
  @ApiResponse(
      responseCode = "200",
      description = "Catálogo P2P paginado obtenido exitosamente",
      content = @Content(schema = @Schema(implementation = P2PMarketPageResponseDTO.class)))
  @ApiResponse(
      responseCode = "400",
      description =
          "Parámetros de consulta o rangos inválidos (INVALID_PRICE_RANGE, INVALID_PAGINATION, INVALID_PAGE_SIZE, INVALID_SORT_PARAMETER, INVALID_PARAMETER_TYPE)",
      content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
  @ApiResponse(
      responseCode = "401",
      description = "No autenticado o token expirado",
      content = @Content)
  public ResponseEntity<P2PMarketPageResponseDTO> getP2PCatalog(
      @AuthenticationPrincipal Jwt jwt,
      @Valid @ModelAttribute P2PMarketQueryRequestDTO requestDTO,
      HttpServletRequest request) {
    String viewerEmail = jwt != null ? jwt.getSubject() : null;
    String[] rawSort = request.getParameterValues("sort");
    P2PMarketQuery query = P2PMarketMapper.toQuery(requestDTO, rawSort);
    P2PMarketCatalog catalog = this.p2pMarketCatalogService.getP2PCatalog(viewerEmail, query);
    return ResponseEntity.ok(P2PMarketMapper.toResponse(catalog));
  }
}
