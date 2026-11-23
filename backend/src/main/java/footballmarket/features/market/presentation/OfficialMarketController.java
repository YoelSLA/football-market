package footballmarket.features.market.presentation;

import footballmarket.common.presentation.dtos.ErrorResponseDTO;
import footballmarket.features.market.application.OfficialBuybackService;
import footballmarket.features.market.application.OfficialMarketCatalogService;
import footballmarket.features.market.application.OfficialMarketQuery;
import footballmarket.features.market.domain.records.OfficialMarketCatalog;
import footballmarket.features.market.presentation.dtos.request.OfficialBuybackRequestDTO;
import footballmarket.features.market.presentation.dtos.request.OfficialMarketQueryRequestDTO;
import footballmarket.features.market.presentation.dtos.response.OfficialBuybackResponseDTO;
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
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controlador REST del Mercado Oficial.
 *
 * <p>Expone la consulta del catálogo de emisión y la venta oficial inmediata al superusuario. Las
 * rutas se declaran completas en cada método porque la venta pertenece al espacio de órdenes.
 */
@RestController
@RequiredArgsConstructor
@Tag(
    name = "Mercado Oficial",
    description =
        "Catálogo primario de emisión con cotizaciones consolidadas y venta oficial inmediata")
@SecurityScheme(
    name = "bearerAuth",
    type = SecuritySchemeType.HTTP,
    scheme = "bearer",
    bearerFormat = "JWT")
@SecurityRequirement(name = "bearerAuth")
public class OfficialMarketController {

  private final OfficialMarketCatalogService officialMarketCatalogService;
  private final OfficialBuybackService officialBuybackService;

  /**
   * Consulta paginada del catálogo de emisión oficial del mercado primario.
   *
   * @param requestDTO parámetros de filtrado, ordenamiento y paginación
   * @param request petición HTTP en curso
   * @return catálogo paginado con identidad deportiva y cotizaciones vigentes
   */
  @GetMapping("/api/market/official")
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
    OfficialMarketCatalog catalog = this.officialMarketCatalogService.getOfficialCatalog(query);
    return ResponseEntity.ok(OfficialMarketMapper.toResponse(catalog));
  }

  /**
   * Vende tokens libres del usuario autenticado al superusuario con la comisión configurada entre
   * 10% y 99% (10% por defecto). Si el precio neto redondeado es cero, rechaza la venta sin
   * cambios.
   *
   * <p>La operación transfiere créditos y tokens y registra la venta en una única transacción. El
   * vendedor es siempre el sujeto de la sesión y no se aceptan partes en el cuerpo.
   *
   * @param jwt principal autenticado por Spring Security
   * @param request solicitud validada con jugador y cantidad
   * @return comprobante de la venta confirmada
   */
  @PostMapping("/api/orders/sell-official")
  @Operation(
      summary = "Vender tokens al superusuario",
      description =
          "Vende tokens libres del usuario autenticado al superusuario, excluyendo los reservados en"
              + " publicaciones P2P activas. Aplica la comisión configurada entre 10% y 99% (10%"
              + " por defecto) sobre la cotización vigente, con redondeo half-up del precio neto"
              + " por token e importes exactos en centésimas. Si el precio neto redondeado es cero,"
              + " rechaza la venta. Transfiere créditos y tokens y registra auditoría atómicamente;"
              + " un rechazo o un fallo no deja cambios. El superusuario no puede vender.")
  @SecurityRequirement(name = "bearerAuth")
  @ApiResponse(
      responseCode = "200",
      description = "Venta confirmada con el comprobante de la operación.",
      content = @Content(schema = @Schema(implementation = OfficialBuybackResponseDTO.class)))
  @ApiResponse(
      responseCode = "400",
      description =
          "El cuerpo es ilegible o un campo tiene formato no válido (INVALID_REQUEST); una cantidad"
              + " entera fuera de 1–100 produce INVALID_QUANTITY. No se aceptan strings numéricos,"
              + " fracciones ni coerciones.",
      content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
  @ApiResponse(
      responseCode = "401",
      description = "La sesión no está autenticada.",
      content = @Content)
  @ApiResponse(
      responseCode = "403",
      description = "El superusuario no puede vender tokens (SUPERUSER_CANNOT_SELL).",
      content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
  @ApiResponse(
      responseCode = "404",
      description = "El jugador no existe (PLAYER_NOT_FOUND) o no es visible (PLAYER_NOT_VISIBLE).",
      content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
  @ApiResponse(
      responseCode = "409",
      description =
          "INSUFFICIENT_STOCK (tenencia libre tras reservas), QUOTE_UNAVAILABLE, INVALID_QUOTE_PRICE"
              + " (cotización no positiva o precio neto redondeado a cero) o INSUFFICIENT_CREDITS del"
              + " superusuario. Ninguna respuesta revela existencias ni saldos.",
      content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
  @ApiResponse(
      responseCode = "500",
      description =
          "Fallo de cálculo, persistencia o liquidación. La operación se revierte completa.",
      content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
  public ResponseEntity<OfficialBuybackResponseDTO> sellOfficial(
      @AuthenticationPrincipal Jwt jwt, @Valid @RequestBody OfficialBuybackRequestDTO request) {
    return ResponseEntity.ok(
        OfficialMarketMapper.toBuybackResponse(
            this.officialBuybackService.sellToSuperuser(
                jwt.getSubject(), OfficialMarketMapper.toBuybackRequest(request))));
  }
}
