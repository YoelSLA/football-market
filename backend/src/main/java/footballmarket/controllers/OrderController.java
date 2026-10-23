package footballmarket.controllers;

import footballmarket.controllers.dtos.requests.OrderBuyRequestDTO;
import footballmarket.controllers.dtos.requests.OrderSellRequestDTO;
import footballmarket.controllers.dtos.responses.ErrorResponseDTO;
import footballmarket.controllers.dtos.responses.OrderBuyResponseDTO;
import footballmarket.controllers.dtos.responses.OrderSellResponseDTO;
import footballmarket.controllers.mappers.ListingMapper;
import footballmarket.controllers.mappers.OrderMapper;
import footballmarket.services.BuyOrderService;
import footballmarket.services.TokenListingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Expone la compra de tokens de un jugador. */
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@Tag(name = "Mercado", description = "Endpoints relacionados con la compra de tokens de jugadores.")
public class OrderController {

  private final BuyOrderService buyOrderService;
  private final TokenListingService tokenListingService;

  /** Publica una venta sin transferir recursos; el vendedor procede del JWT. */
  @Operation(
      summary = "Publicar tokens para la venta",
      description =
          "Reserva tokens propios a precio congelado en centésimas, sin mover créditos ni tokens ni generar transacción. Solo USER persistido.")
  @SecurityRequirement(name = "bearerAuth")
  @ApiResponse(
      responseCode = "201",
      description = "Publicación creada con precio congelado y vencimiento.")
  @ApiResponse(
      responseCode = "400",
      description =
          "INVALID_REQUEST por estructura o INVALID_QUANTITY por cantidad fuera de 1–100.",
      content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
  @ApiResponse(
      responseCode = "401",
      description = "Sesión ausente, inválida o vencida.",
      content = @Content)
  @ApiResponse(
      responseCode = "403",
      description = "SUPERUSER_CANNOT_SELL.",
      content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
  @ApiResponse(
      responseCode = "404",
      description = "PLAYER_NOT_FOUND o PLAYER_NOT_VISIBLE.",
      content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
  @ApiResponse(
      responseCode = "409",
      description =
          "INSUFFICIENT_STOCK (tenencia menos reservas vigentes), QUOTE_UNAVAILABLE o INVALID_QUOTE_PRICE. No revela existencias.",
      content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
  @ApiResponse(
      responseCode = "500",
      description = "INTERNAL_ERROR; reversión completa.",
      content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
  @PostMapping("/sell")
  public ResponseEntity<OrderSellResponseDTO> sell(
      @AuthenticationPrincipal Jwt jwt, @Valid @RequestBody OrderSellRequestDTO request) {
    return ResponseEntity.status(201)
        .body(
            ListingMapper.toSellResponse(
                this.tokenListingService.publish(
                    jwt.getSubject(), ListingMapper.toSellRequest(request))));
  }

  /**
   * Compra tokens directamente o liquida una publicación completa con los créditos del usuario.
   *
   * <p>El comprador es siempre el sujeto de la sesión y su rol se resuelve desde la persistencia.
   * La compra directa no es idempotente: cada solicitud válida representa una compra nueva, de modo
   * que repetirla confirma otra compra siempre que haya créditos y stock suficientes.
   *
   * @param jwt principal autenticado por Spring Security
   * @param request solicitud de compra validada
   * @return resultado de la compra confirmada
   */
  @Operation(
      summary = "Comprar tokens de un jugador",
      description =
          "Sin listingId compra directamente al superusuario con playerId y quantity obligatorios."
              + " Con listingId liquida la publicación completa de otro usuario al precio congelado,"
              + " ignorando playerId y quantity; el precio nunca lo elige el cliente. Transfiere tokens"
              + " y créditos y registra auditoría atómicamente. Importes en centésimas. La compra directa"
              + " es nueva por solicitud; una publicación solo se liquida una vez. El superusuario no compra.")
  @SecurityRequirement(name = "bearerAuth")
  @ApiResponse(
      responseCode = "200",
      description = "Compra confirmada con el detalle de la operación.")
  @ApiResponse(
      responseCode = "400",
      description =
          "El cuerpo es ilegible, no hay listingId ni datos completos de compra directa, o un campo tiene formato inválido. listingId es Long entero JSON sin coerción. En modalidad directa quantity es nula, de tipo incorrecto, decimal,"
              + " exponencial o un entero no representable como Integer (INVALID_REQUEST). No se"
              + " aceptan strings numéricos ni coerciones. Un literal entero JSON representable como"
              + " Integer fuera de 1–100 produce INVALID_QUANTITY.",
      content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
  @ApiResponse(
      responseCode = "401",
      description = "La sesión no está autenticada.",
      content = @Content)
  @ApiResponse(
      responseCode = "403",
      description =
          "El superusuario no puede comprar tokens (SUPERUSER_CANNOT_BUY), con independencia de su"
              + " saldo y del stock del jugador. CANNOT_BUY_OWN_LISTING al comprar una publicación propia.",
      content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
  @ApiResponse(
      responseCode = "404",
      description =
          "El identificador no corresponde a ningún jugador (PLAYER_NOT_FOUND) o el jugador existe"
              + " pero no cumple el predicado de visibilidad (PLAYER_NOT_VISIBLE). En modalidad secundaria,"
              + " LISTING_NOT_FOUND tanto para publicación inexistente como para jugador no visible, sin revelar existencia.",
      content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
  @ApiResponse(
      responseCode = "409",
      description =
          "Rechazo de negocio: no hay tokens suficientes (INSUFFICIENT_STOCK), no hay créditos"
              + " suficientes (INSUFFICIENT_CREDITS), no hay cotización vigente (QUOTE_UNAVAILABLE) o"
              + " el precio no es positivo (INVALID_QUOTE_PRICE). Ninguna de estas respuestas revela"
              + " existencias. LISTING_NOT_AVAILABLE en publicaciones vendidas, canceladas, vencidas"
              + " o sin cuenta receptora válida; los motivos son indistinguibles.",
      content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
  @ApiResponse(
      responseCode = "500",
      description =
          "Fallo interno de la compra. La operación se revierte completa y la respuesta es genérica.",
      content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
  @PostMapping("/buy")
  public ResponseEntity<OrderBuyResponseDTO> buy(
      @AuthenticationPrincipal Jwt jwt, @Valid @RequestBody OrderBuyRequestDTO request) {

    if (request.listingId() != null) {
      return ResponseEntity.ok(
          OrderMapper.toBuyResponse(
              this.buyOrderService.buyListing(
                  jwt.getSubject(), OrderMapper.toBuyListingRequest(request))));
    }
    return ResponseEntity.ok(
        OrderMapper.toBuyResponse(
            this.buyOrderService.buy(jwt.getSubject(), OrderMapper.toBuyRequest(request))));
  }
}
