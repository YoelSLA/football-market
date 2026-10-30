package footballmarket.features.market.presentation;

import footballmarket.common.presentation.dtos.ErrorResponseDTO;
import footballmarket.features.market.application.TokenListingService;
import footballmarket.features.market.presentation.dtos.response.TokenListingResponseDTO;
import footballmarket.features.market.presentation.dtos.response.TokenListingsResponseDTO;
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
import org.springframework.web.bind.annotation.*;

/** Frontera HTTP de las consultas y cancelación; no crea publicaciones. */
@RestController
@RequestMapping("/api/listings")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Mercado")
public class ListingController {
  private final TokenListingService listings;

  @Operation(
      summary = "Cancelar publicación propia",
      description =
          "Solo el propietario puede cancelar una publicación activa y vigente. Registra CANCELLED y closedAt y libera la reserva sin tocar créditos ni tenencias.")
  @ApiResponse(responseCode = "200", description = "Detalle de la publicación cancelada.")
  @ApiResponse(
      responseCode = "401",
      description = "Sesión ausente, inválida o vencida.",
      content = @Content)
  @ApiResponse(
      responseCode = "403",
      description = "ACCESS_DENIED: publicación ajena.",
      content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
  @ApiResponse(
      responseCode = "404",
      description = "LISTING_NOT_FOUND.",
      content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
  @ApiResponse(
      responseCode = "409",
      description = "LISTING_NOT_AVAILABLE: vendida, cancelada o vencida.",
      content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
  @PostMapping("/{id}/cancel")
  public TokenListingResponseDTO cancel(
      @AuthenticationPrincipal Jwt jwt,
      @Parameter(description = "Identificador de la publicación a cancelar.") @PathVariable
          Long id) {
    return ListingMapper.toResponse(this.listings.cancel(jwt.getSubject(), id));
  }

  @Operation(
      summary = "Listar el mercado",
      description =
          "Solo publicaciones activas y vigentes de otros usuarios sobre jugadores visibles; sin paginación en esta feature.")
  @ApiResponse(
      responseCode = "200",
      description = "Publicaciones de mercado; lista vacía si no hay resultados.")
  @ApiResponse(
      responseCode = "401",
      description = "Sesión ausente, inválida o vencida.",
      content = @Content)
  @GetMapping
  public TokenListingsResponseDTO market(@AuthenticationPrincipal Jwt jwt) {
    return ListingMapper.toResponses(this.listings.market(jwt.getSubject()));
  }

  @Operation(
      summary = "Listar publicaciones propias",
      description =
          "Incluye cualquier estado efectivo y jugadores no visibles. El filtro considera solo el estado efectivo.")
  @ApiResponse(responseCode = "200", description = "Publicaciones propias.")
  @ApiResponse(
      responseCode = "400",
      description = "INVALID_PARAMETER_TYPE: estado desconocido, vacío o capitalización distinta.",
      content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
  @ApiResponse(
      responseCode = "401",
      description = "Sesión ausente, inválida o vencida.",
      content = @Content)
  @GetMapping("/mine")
  public TokenListingsResponseDTO mine(
      @AuthenticationPrincipal Jwt jwt,
      @Parameter(
              description = "Forma exacta y sensible a mayúsculas.",
              schema = @Schema(allowableValues = {"ACTIVE", "SOLD", "CANCELLED", "EXPIRED"}))
          @RequestParam(required = false)
          String status) {
    return ListingMapper.toResponses(this.listings.mine(jwt.getSubject(), status));
  }

  @Operation(
      summary = "Consultar publicación",
      description =
          "El dueño consulta cualquier estado; terceros solo publicaciones activas y vigentes sobre jugadores visibles. Los rechazos no revelan existencia.")
  @ApiResponse(
      responseCode = "200",
      description = "Detalle con estado efectivo y closedAt nullable.")
  @ApiResponse(
      responseCode = "401",
      description = "Sesión ausente, inválida o vencida.",
      content = @Content)
  @ApiResponse(
      responseCode = "404",
      description = "LISTING_NOT_FOUND: inexistente o no accesible al solicitante.",
      content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
  @GetMapping("/{id}")
  public TokenListingResponseDTO detail(
      @AuthenticationPrincipal Jwt jwt,
      @Parameter(
              description =
                  "Identificador de publicación. Un entero no positivo no corresponde a ninguna.")
          @PathVariable
          Long id) {
    return ListingMapper.toResponse(this.listings.detail(jwt.getSubject(), id));
  }
}
