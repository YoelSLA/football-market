package footballmarket.controllers;

import footballmarket.controllers.dtos.requests.OrderBuyRequestDTO;
import footballmarket.controllers.dtos.responses.ErrorResponseDTO;
import footballmarket.controllers.dtos.responses.OrderBuyResponseDTO;
import footballmarket.controllers.mappers.OrderMapper;
import footballmarket.models.MarketTransaction;
import footballmarket.services.BuyOrderService;
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

  /**
   * Compra tokens de un jugador con los créditos del usuario autenticado.
   *
   * <p>El comprador es siempre el sujeto de la sesión y su rol se resuelve desde la persistencia.
   * La operación no es idempotente: cada solicitud válida representa una compra nueva, de modo que
   * repetirla confirma otra compra siempre que haya créditos y stock suficientes.
   *
   * @param jwt principal autenticado por Spring Security
   * @param request solicitud de compra validada
   * @return resultado de la compra confirmada
   */
  @Operation(
      summary = "Comprar tokens de un jugador",
      description =
          "Transfiere de forma atómica créditos y tokens entre el usuario autenticado y el"
              + " superusuario, y registra la operación financiera. Los importes se expresan en"
              + " centésimas de crédito. La operación no es idempotente: cada solicitud válida es una"
              + " compra nueva. El superusuario no puede comprar.")
  @SecurityRequirement(name = "bearerAuth")
  @ApiResponse(
      responseCode = "200",
      description = "Compra confirmada con el detalle de la operación.")
  @ApiResponse(
      responseCode = "400",
      description =
          "El cuerpo es ilegible, falta un campo o quantity es nula, de tipo incorrecto, decimal,"
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
              + " saldo y del stock del jugador.",
      content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
  @ApiResponse(
      responseCode = "404",
      description =
          "El identificador no corresponde a ningún jugador (PLAYER_NOT_FOUND) o el jugador existe"
              + " pero no cumple el predicado de visibilidad (PLAYER_NOT_VISIBLE).",
      content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
  @ApiResponse(
      responseCode = "409",
      description =
          "Rechazo de negocio: no hay tokens suficientes (INSUFFICIENT_STOCK), no hay créditos"
              + " suficientes (INSUFFICIENT_CREDITS), no hay cotización vigente (QUOTE_UNAVAILABLE) o"
              + " el precio no es positivo (INVALID_QUOTE_PRICE). Ninguna de estas respuestas revela"
              + " existencias.",
      content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
  @ApiResponse(
      responseCode = "500",
      description =
          "Fallo interno de la compra. La operación se revierte completa y la respuesta es genérica.",
      content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
  @PostMapping("/buy")
  public ResponseEntity<OrderBuyResponseDTO> buy(
      @AuthenticationPrincipal Jwt jwt, @Valid @RequestBody OrderBuyRequestDTO request) {

    MarketTransaction transaction =
        this.buyOrderService.buy(jwt.getSubject(), OrderMapper.toBuyRequest(request));

    return ResponseEntity.ok(OrderMapper.toBuyResponse(transaction));
  }
}
