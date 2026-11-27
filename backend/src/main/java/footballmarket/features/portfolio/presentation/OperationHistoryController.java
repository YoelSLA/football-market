package footballmarket.features.portfolio.presentation;

import footballmarket.common.presentation.dtos.ErrorResponseDTO;
import footballmarket.features.portfolio.application.OperationHistoryService;
import footballmarket.features.portfolio.presentation.dtos.response.OperationHistoryResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.OffsetDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Frontera HTTP del historial de operaciones del usuario autenticado. */
@RestController
@RequestMapping("/api/users/me/transactions")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Mercado")
public class OperationHistoryController {

  private final OperationHistoryService operationHistoryService;

  /** Devuelve el historial filtrado de la identidad obtenida exclusivamente de la sesión. */
  @Operation(
      summary = "Consultar mi historial de operaciones",
      description =
          "Devuelve una página de compras y ventas confirmadas, indicadores del conjunto filtrado y el perfil actual del jugador. Los valores financieros permanecen históricos.")
  @ApiResponse(
      responseCode = "200",
      description = "Historial paginado; sin coincidencias, transactions es un array vacío.")
  @ApiResponse(
      responseCode = "400",
      description = "INVALID_PARAMETER_TYPE o INVALID_DATE_RANGE.",
      content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
  @ApiResponse(responseCode = "401", description = "Sesión ausente, inválida o vencida.")
  @ApiResponse(
      responseCode = "500",
      description = "INTERNAL_ERROR: la consulta no pudo completarse.",
      content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
  @GetMapping
  public OperationHistoryResponseDTO history(
      @AuthenticationPrincipal Jwt jwt,
      @Parameter(description = "Página basada en cero.") @RequestParam(defaultValue = "0") int page,
      @Parameter(description = "Tamaño entre 1 y 100.") @RequestParam(defaultValue = "20") int size,
      @Parameter(description = "Búsqueda opcional por nombre actual del jugador.")
          @RequestParam(required = false)
          String search,
      @Parameter(description = "Tipo opcional: BUY o SELL.") @RequestParam(required = false)
          String type,
      @Parameter(description = "Instante inicial inclusivo.")
          @RequestParam(required = false)
          @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
          OffsetDateTime executedFrom,
      @Parameter(description = "Instante final exclusivo.")
          @RequestParam(required = false)
          @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
          OffsetDateTime executedBefore,
      @Parameter(description = "NEWEST, OLDEST, AMOUNT_DESC o AMOUNT_ASC.")
          @RequestParam(defaultValue = "NEWEST")
          String sort) {
    return OperationHistoryMapper.toResponse(
        this.operationHistoryService.history(
            jwt.getSubject(), page, size, search, type, executedFrom, executedBefore, sort));
  }
}
