package footballmarket.controllers;

import footballmarket.controllers.dtos.responses.ErrorResponseDTO;
import footballmarket.controllers.dtos.responses.OperationHistoryResponseDTO;
import footballmarket.controllers.mappers.OperationHistoryMapper;
import footballmarket.services.OperationHistoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Frontera HTTP del historial de operaciones del usuario autenticado. */
@RestController
@RequestMapping("/api/users/me/transactions")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Mercado")
public class OperationHistoryController {

  private final OperationHistoryService operationHistoryService;

  /** Devuelve el historial completo de la identidad obtenida exclusivamente de la sesión. */
  @Operation(
      summary = "Consultar mi historial de operaciones",
      description =
          "Devuelve las compras y ventas confirmadas del usuario autenticado, desde la más reciente. La identidad procede exclusivamente del subject del JWT; la operación no acepta parámetros ni body.")
  @ApiResponse(
      responseCode = "200",
      description = "Historial completo; un usuario sin operaciones recibe un array vacío.",
      content =
          @Content(
              array =
                  @ArraySchema(
                      schema = @Schema(implementation = OperationHistoryResponseDTO.class))))
  @ApiResponse(
      responseCode = "401",
      description = "Sesión ausente, inválida, vencida o sin identidad persistida.",
      content = @Content)
  @ApiResponse(
      responseCode = "500",
      description = "INTERNAL_ERROR: la consulta no pudo completarse.",
      content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
  @GetMapping
  public List<OperationHistoryResponseDTO> history(@AuthenticationPrincipal Jwt jwt) {
    return OperationHistoryMapper.toResponse(
        this.operationHistoryService.history(jwt.getSubject()));
  }
}
