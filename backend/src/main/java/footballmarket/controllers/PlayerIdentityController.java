package footballmarket.controllers;

import footballmarket.controllers.dtos.responses.ErrorResponseDTO;
import footballmarket.controllers.dtos.responses.PlayerIdentitySyncRunResponseDTO;
import footballmarket.controllers.exceptions.InvalidPlayerIdentityIdException;
import footballmarket.controllers.mappers.PlayerIdentityMapper;
import footballmarket.services.PlayerIdentitySynchronizationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Frontera de ejecución actual e histórica; la auditoría no tiene interfaz HTTP de consulta. */
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
      summary = "Identificar jugadores desde el catálogo actual",
      description =
          "POST síncrono sin body ni parámetros. Requiere JWT y usuario persistido. Cada invocación crea una ejecución nueva; conserva referencias confirmadas y devuelve su resumen terminado, incluso parcial o fallido.")
  @ApiResponse(
      responseCode = "200",
      description =
          "Resumen CURRENT COMPLETED, PARTIAL o FAILED; vacío sin red cuando no hay jugadores",
      content = @Content(schema = @Schema(implementation = PlayerIdentitySyncRunResponseDTO.class)))
  @ApiResponse(
      responseCode = "401",
      description = "JWT ausente, expirado o inválido",
      content = @Content)
  @ApiResponse(
      responseCode = "500",
      description = "PLAYER_IDENTITY_SYNC_FAILED; resultados anteriores conservados",
      content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
  public ResponseEntity<PlayerIdentitySyncRunResponseDTO> synchronizeCurrent() {
    return ResponseEntity.ok(
        PlayerIdentityMapper.toResponse(this.synchronization.synchronizeCurrent()));
  }

  @PostMapping("/sync-runs/{id}/historical")
  @Operation(
      summary = "Resolver pendientes desde el catálogo histórico",
      description =
          "POST síncrono sin body. Requiere JWT y usuario persistido. Origen CURRENT terminado COMPLETED o PARTIAL; solo NO_MATCH/AMBIGUOUS todavía existentes y sin referencia WhoScored. Captura atributos nuevos y conserva la auditoría de origen. Cero elegibles completa sin red con ámbitos SKIPPED. Cada invocación crea otro id y devuelve sourceRunId.")
  @ApiResponse(
      responseCode = "200",
      description =
          "Resumen HISTORICAL terminado COMPLETED, PARTIAL o FAILED, incluso vacío sin red",
      content = @Content(schema = @Schema(implementation = PlayerIdentitySyncRunResponseDTO.class)))
  @ApiResponse(
      responseCode = "400",
      description = "INVALID_PARAMETER_TYPE o INVALID_PLAYER_IDENTITY_ID",
      content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
  @ApiResponse(
      responseCode = "401",
      description = "JWT ausente, expirado o inválido",
      content = @Content)
  @ApiResponse(
      responseCode = "404",
      description = "PLAYER_IDENTITY_RUN_NOT_FOUND: origen inexistente",
      content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
  @ApiResponse(
      responseCode = "409",
      description = "PLAYER_IDENTITY_SOURCE_NOT_ELIGIBLE: no CURRENT terminado COMPLETED/PARTIAL",
      content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
  @ApiResponse(
      responseCode = "500",
      description = "PLAYER_IDENTITY_SYNC_FAILED",
      content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
  public ResponseEntity<PlayerIdentitySyncRunResponseDTO> synchronizeHistorical(
      @Parameter(
              description = "ID positivo del pase actual de origen",
              required = true,
              schema = @Schema(type = "integer", format = "int64", minimum = "1"))
          @PathVariable
          Long id) {
    if (id < 1) {
      throw new InvalidPlayerIdentityIdException();
    }
    return ResponseEntity.ok(
        PlayerIdentityMapper.toResponse(this.synchronization.synchronizeHistorical(id)));
  }
}
