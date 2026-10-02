package footballmarket.controllers;

import footballmarket.controllers.dtos.responses.ErrorResponseDTO;
import footballmarket.controllers.dtos.responses.PlayerImageSyncRunItemPageResponseDTO;
import footballmarket.controllers.dtos.responses.PlayerImageSyncRunPageResponseDTO;
import footballmarket.controllers.dtos.responses.PlayerImageSyncRunResponseDTO;
import footballmarket.controllers.exceptions.InvalidPlayerPageException;
import footballmarket.controllers.mappers.PlayerImageMapper;
import footballmarket.services.PlayerImageAuditService;
import footballmarket.services.PlayerImageSynchronizationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Frontera HTTP de sincronización manual y auditoría del catálogo de imágenes. */
@RestController
@RequestMapping("/api/players/images")
@Tag(name = "Imágenes de jugadores", description = "Sincronización y auditoría autenticadas")
@SecurityRequirement(name = "bearerAuth")
public class PlayerImageController {
  private final PlayerImageSynchronizationService synchronization;
  private final PlayerImageAuditService audit;

  public PlayerImageController(
      PlayerImageSynchronizationService synchronization, PlayerImageAuditService audit) {
    this.synchronization = synchronization;
    this.audit = audit;
  }

  @PostMapping("/sync")
  @Operation(
      summary = "Sincronizar imágenes manualmente",
      description =
          "Ejecución síncrona sin body, disponible para cualquier usuario autenticado. "
              + "force omite la elegibilidad pero no repite el matching de una referencia existente.")
  @ApiResponse(
      responseCode = "200",
      description = "Resumen COMPLETED o PARTIAL sin items",
      content = @Content(schema = @Schema(implementation = PlayerImageSyncRunResponseDTO.class)))
  @ApiResponse(responseCode = "401", description = "JWT ausente o inválido", content = @Content)
  @ApiResponse(
      responseCode = "409",
      description = "Sincronización activa (PLAYER_IMAGE_SYNC_IN_PROGRESS)",
      content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
  @ApiResponse(
      responseCode = "502",
      description =
          "Fallo global con resultados previos conservados o configuración inválida antes de iniciar el run (PLAYER_IMAGE_SYNC_FAILED)",
      content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
  public ResponseEntity<PlayerImageSyncRunResponseDTO> synchronize(
      @Parameter(description = "Fuerza el refresco de activos sin rematching")
          @RequestParam(defaultValue = "false")
          boolean force) {
    return ResponseEntity.ok(PlayerImageMapper.toResponse(this.synchronization.synchronize(force)));
  }

  @GetMapping("/sync-runs")
  @Operation(
      summary = "Consultar historial de sincronizaciones",
      description =
          "Devuelve resúmenes locales en orden de inicio descendente e ID descendente, sin items. Requiere JWT Bearer.")
  @ApiResponse(
      responseCode = "200",
      description = "Historial paginado",
      content =
          @Content(schema = @Schema(implementation = PlayerImageSyncRunPageResponseDTO.class)))
  @ApiResponse(
      responseCode = "400",
      description = "Paginación inválida (INVALID_PLAYER_PAGE o INVALID_PARAMETER_TYPE)",
      content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
  @ApiResponse(responseCode = "401", description = "JWT ausente o inválido", content = @Content)
  public ResponseEntity<PlayerImageSyncRunPageResponseDTO> getRuns(
      @Parameter(
              description = "Página desde cero",
              schema = @Schema(minimum = "0", defaultValue = "0"))
          @RequestParam(defaultValue = "0")
          int page,
      @Parameter(
              description = "Tamaño de 1 a 100",
              schema = @Schema(minimum = "1", maximum = "100", defaultValue = "20"))
          @RequestParam(defaultValue = "20")
          int size) {
    this.validatePage(page, size);
    return ResponseEntity.ok(PlayerImageMapper.toRunPage(this.audit.getRuns(page, size)));
  }

  @GetMapping("/sync-runs/{id}")
  @Operation(
      summary = "Consultar resumen de una sincronización",
      description = "Consulta local del run por identificador sin items; requiere JWT Bearer.")
  @ApiResponse(
      responseCode = "200",
      description = "Resumen del run",
      content = @Content(schema = @Schema(implementation = PlayerImageSyncRunResponseDTO.class)))
  @ApiResponse(
      responseCode = "400",
      description = "ID inválido (INVALID_PARAMETER_TYPE)",
      content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
  @ApiResponse(responseCode = "401", description = "JWT ausente o inválido", content = @Content)
  @ApiResponse(
      responseCode = "404",
      description = "Run inexistente (PLAYER_IMAGE_SYNC_RUN_NOT_FOUND)",
      content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
  public ResponseEntity<PlayerImageSyncRunResponseDTO> getRun(
      @Parameter(description = "Identificador interno del run") @PathVariable Long id) {
    return ResponseEntity.ok(PlayerImageMapper.toResponse(this.audit.getRun(id)));
  }

  @GetMapping("/sync-runs/{id}/items")
  @Operation(
      summary = "Consultar detalle de una sincronización",
      description = "Items locales en orden ascendente de identificador; requiere JWT Bearer.")
  @ApiResponse(
      responseCode = "200",
      description = "Detalle paginado",
      content =
          @Content(schema = @Schema(implementation = PlayerImageSyncRunItemPageResponseDTO.class)))
  @ApiResponse(
      responseCode = "400",
      description = "ID o paginación inválidos (INVALID_PLAYER_PAGE o INVALID_PARAMETER_TYPE)",
      content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
  @ApiResponse(responseCode = "401", description = "JWT ausente o inválido", content = @Content)
  @ApiResponse(
      responseCode = "404",
      description = "Run inexistente (PLAYER_IMAGE_SYNC_RUN_NOT_FOUND)",
      content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
  public ResponseEntity<PlayerImageSyncRunItemPageResponseDTO> getItems(
      @Parameter(description = "Identificador interno del run") @PathVariable Long id,
      @Parameter(
              description = "Página desde cero",
              schema = @Schema(minimum = "0", defaultValue = "0"))
          @RequestParam(defaultValue = "0")
          int page,
      @Parameter(
              description = "Tamaño de 1 a 100",
              schema = @Schema(minimum = "1", maximum = "100", defaultValue = "20"))
          @RequestParam(defaultValue = "20")
          int size) {
    this.validatePage(page, size);
    return ResponseEntity.ok(PlayerImageMapper.toItemPage(this.audit.getItems(id, page, size)));
  }

  private void validatePage(int page, int size) {
    if (page < 0 || size < 1 || size > 100) {
      throw new InvalidPlayerPageException();
    }
  }
}
