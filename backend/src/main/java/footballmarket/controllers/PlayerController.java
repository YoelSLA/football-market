package footballmarket.controllers;

import footballmarket.controllers.dtos.responses.ErrorResponseDTO;
import footballmarket.controllers.dtos.responses.PlayerSyncResponseDTO;
import footballmarket.controllers.dtos.responses.PlayersPageResponseDTO;
import footballmarket.controllers.exceptions.InvalidPlayerPageException;
import footballmarket.controllers.mappers.PlayerMapper;
import footballmarket.orchestrators.PlayerSynchronizationOrchestrator;
import footballmarket.services.PlayerCatalogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/** Contratos HTTP del catálogo local y su sincronización manual. */
@RestController
@RequestMapping("/api/players")
@RequiredArgsConstructor
@Tag(name = "Jugadores", description = "Catálogo local y sincronización manual")
@SecurityScheme(
    name = "bearerAuth",
    type = SecuritySchemeType.HTTP,
    scheme = "bearer",
    bearerFormat = "JWT")
@SecurityRequirement(name = "bearerAuth")
public class PlayerController {
  private final PlayerCatalogService playerCatalogService;
  private final PlayerSynchronizationOrchestrator playerSynchronizationOrchestrator;

  @PostMapping("/sync")
  @Operation(
      summary = "Sincronizar jugadores",
      description =
          "Sincronización manual de las competiciones configuradas en Football-Data.org (inicialmente PL, BL1, PD, SA y FL1), sin body ni rol adicional. Confirma ligas, equipos y jugadores en un único commit local; conserva identidades y protege presentes inválidos. Tras confirmar, intenta enriquecer referencias de equipos en TheSportsDB sin afectar los cinco contadores ni revertir el catálogo. No obtiene imágenes. Ante 429 de Football-Data respeta Retry-After con hasta tres reintentos.")
  @ApiResponse(responseCode = "200", description = "Foto completa aplicada al catálogo")
  @ApiResponse(responseCode = "401", description = "JWT ausente o inválido", content = @Content)
  @ApiResponse(
      responseCode = "502",
      description =
          "Foto del proveedor incompleta/no disponible o fallo técnico local; sin cambios parciales de la sincronización (FOOTBALL_DATA_UNAVAILABLE)",
      content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
  public ResponseEntity<PlayerSyncResponseDTO> synchronize() {
    return ResponseEntity.ok(
        PlayerMapper.toResponse(this.playerSynchronizationOrchestrator.synchronize()));
  }

  @GetMapping
  @Operation(
      summary = "Consultar jugadores activos",
      description =
          "Consulta paginada exclusivamente local, ordenada por ID interno: solo jugadores activos asociados a Team vigente y League válida. Expone teamId/teamName y leagueId/leagueName derivados de las entidades internas, sin team/league legacy ni referencias externas. Name y position conservan exactamente la presentación persistida; dateOfBirth, nationality, imageUrl y fallbackImageUrl son nullable. Los casos históricos no excluyen por sí solos un jugador válido. Requiere JWT válido.")
  @ApiResponse(responseCode = "200", description = "Página de jugadores activos")
  @ApiResponse(
      responseCode = "400",
      description = "Paginación inválida (INVALID_PLAYER_PAGE o INVALID_PARAMETER_TYPE)",
      content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
  @ApiResponse(responseCode = "401", description = "JWT ausente o inválido", content = @Content)
  public ResponseEntity<PlayersPageResponseDTO> getPlayers(
      @Parameter(
              description = "Página desde cero",
              schema = @Schema(minimum = "0", defaultValue = "0"))
          @RequestParam(defaultValue = "0")
          int page,
      @Parameter(
              description = "Tamaño de página entre 1 y 100",
              schema = @Schema(minimum = "1", maximum = "100", defaultValue = "20"))
          @RequestParam(defaultValue = "20")
          int size) {
    if (page < 0 || size < 1 || size > 100) {
      throw new InvalidPlayerPageException();
    }
    return ResponseEntity.ok(
        PlayerMapper.toResponse(this.playerCatalogService.getActivePlayers(page, size)));
  }
}
