package footballmarket.features.identity.presentation;

import footballmarket.common.presentation.dtos.ErrorResponseDTO;
import footballmarket.features.identity.application.PlayerIdentitySynchronizationService;
import footballmarket.features.identity.presentation.dtos.response.PlayerIdentitySyncRunResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Frontera HTTP de la sincronización completa de referencias WhoScored. */
@RestController
@RequestMapping("/api/players/identities/whoscored")
@Tag(name = "Identidad WhoScored", description = "Identificación manual del catálogo persistido")
@SecurityRequirement(name = "bearerAuth")
public class PlayerIdentityController {
  private final PlayerIdentitySynchronizationService synchronization;

  public PlayerIdentityController(PlayerIdentitySynchronizationService synchronization) {
    this.synchronization = synchronization;
  }

  @PostMapping("/sync")
  @Operation(
      summary = "Obtener referencias WhoScored del catálogo local",
      description =
          "POST síncrono sin body requerido ni parámetros propios. Requiere JWT y usuario persistido. "
              + "Ejecuta el catálogo actual y después el histórico para pendientes elegibles; un actual FAILED omite el histórico. "
              + "Devuelve un resultado final por jugador local sin desglose por liga ni auditoría persistida. Relanzar conserva referencias existentes.")
  @ApiResponse(
      responseCode = "200",
      description =
          "Resultado global COMPLETED, PARTIAL o FAILED; totales locales sin doble conteo",
      content = @Content(schema = @Schema(implementation = PlayerIdentitySyncRunResponseDTO.class)))
  @ApiResponse(
      responseCode = "401",
      description = "JWT ausente, expirado o inválido",
      content = @Content)
  @ApiResponse(
      responseCode = "500",
      description = "PLAYER_IDENTITY_SYNC_FAILED; referencias confirmadas conservadas",
      content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
  public ResponseEntity<PlayerIdentitySyncRunResponseDTO> synchronize() {
    return ResponseEntity.ok(PlayerIdentityMapper.toResponse(this.synchronization.synchronize()));
  }
}
