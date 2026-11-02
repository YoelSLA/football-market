package footballmarket.features.catalog.presentation;

import footballmarket.common.presentation.dtos.ErrorResponseDTO;
import footballmarket.features.catalog.application.PlayerCatalogService;
import footballmarket.features.catalog.application.PlayerSynchronizationOrchestrator;
import footballmarket.features.catalog.presentation.dtos.request.PlayerCatalogQueryRequestDTO;
import footballmarket.features.catalog.presentation.dtos.response.PlayerCatalogFiltersResponseDTO;
import footballmarket.features.catalog.presentation.dtos.response.PlayerCatalogPageResponseDTO;
import footballmarket.features.catalog.presentation.dtos.response.PlayerResponseDTO;
import footballmarket.features.catalog.presentation.dtos.response.PlayerSyncResponseDTO;
import footballmarket.features.catalog.presentation.dtos.response.PlayersPageResponseDTO;
import footballmarket.features.catalog.presentation.exceptions.InvalidPlayerIdException;
import footballmarket.features.statistics.application.PlayerStatisticsQueryService;
import footballmarket.features.statistics.presentation.PlayerStatisticsMapper;
import footballmarket.features.statistics.presentation.dtos.response.PlayerStatisticsResponseDTO;
import footballmarket.features.statistics.presentation.exceptions.InvalidStatisticsRequestException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
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
  private final PlayerStatisticsQueryService playerStatisticsQueryService;

  @GetMapping("/{id}/statistics")
  @Operation(
      summary = "Consultar estadísticas reales de un jugador",
      description =
          "Última extracción confirmada individual, filtrada por seasonKey si se indica. JSON prioritario con respaldo scraping solo del mismo contexto y fecha. Devuelve 60 métricas con ausencias explícitas; no escribe ni calcula valoraciones.")
  @ApiResponse(responseCode = "200", description = "Snapshot disponible, parcial o NO_DATA")
  @ApiResponse(
      responseCode = "400",
      description = "ID o temporada inválidos",
      content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
  @ApiResponse(responseCode = "401", description = "JWT ausente o inválido", content = @Content)
  @ApiResponse(
      responseCode = "404",
      description = "Jugador no encontrado o no visible",
      content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
  @ApiResponse(
      responseCode = "500",
      description = "Fallo de lectura",
      content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
  public ResponseEntity<PlayerStatisticsResponseDTO> getStatistics(
      @Parameter(description = "ID interno positivo", required = true) @PathVariable Long id,
      @Parameter(
              description =
                  "Clave exacta de temporada; sin ella se resuelve el último contexto individual")
          @RequestParam(required = false)
          String seasonKey) {
    if (id <= 0) {
      throw new InvalidPlayerIdException();
    }
    if (seasonKey != null && (seasonKey.isBlank() || seasonKey.length() > 255)) {
      throw new InvalidStatisticsRequestException(
          "INVALID_REQUEST", "La temporada debe ser una clave no vacía de hasta 255 caracteres");
    }
    return ResponseEntity.ok(
        PlayerStatisticsMapper.toResponse(
            this.playerStatisticsQueryService.getStatistics(id, seasonKey)));
  }

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
          "Consulta local de jugadores visibles con filtros globales name, leagueId, teamId, position y nationality antes de paginar. name ignora acentos/mayúsculas y trata comodines literalmente; sort admite id,asc (legado), name,asc o name,desc. includeStatistics=true agrega tres valores reales por lote, con ausencias explícitas; false conserva la respuesta anterior. Tamaño 1–100, predeterminado 20; requiere JWT, no calcula cotizaciones ni consulta proveedores.")
  @ApiResponse(
      responseCode = "200",
      description = "Página visible con enriquecimiento opcional",
      content =
          @Content(
              schema =
                  @Schema(
                      oneOf = {PlayersPageResponseDTO.class, PlayerCatalogPageResponseDTO.class})))
  @ApiResponse(
      responseCode = "400",
      description =
          "Paginación o criterios inválidos (INVALID_PLAYER_PAGE, INVALID_PARAMETER_TYPE o INVALID_REQUEST)",
      content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
  @ApiResponse(responseCode = "401", description = "JWT ausente o inválido", content = @Content)
  public ResponseEntity<?> getPlayers(@Valid @ModelAttribute PlayerCatalogQueryRequestDTO request) {
    int page = request.page() == null ? 0 : request.page();
    int size = request.size() == null ? 20 : request.size();
    if (request.name() == null
        && request.leagueId() == null
        && request.teamId() == null
        && request.position() == null
        && request.nationality() == null
        && request.sort() == null
        && !Boolean.TRUE.equals(request.includeStatistics())) {
      return ResponseEntity.ok(
          PlayerMapper.toResponse(this.playerCatalogService.getActivePlayers(page, size)));
    }
    return ResponseEntity.ok(
        PlayerMapper.toResponse(
            this.playerCatalogService.queryCatalog(PlayerMapper.toCriteria(request), page, size)));
  }

  @GetMapping("/filters")
  @Operation(
      summary = "Opciones globales de filtros de jugadores",
      description =
          "Ligas, equipos, posiciones y nacionalidades únicas del conjunto visible. Independientes de la página y sin cascadas; no incluye datos inventados para opcionales ausentes.")
  @ApiResponse(
      responseCode = "200",
      description = "Opciones globales, listas vacías si no hay jugadores")
  @ApiResponse(responseCode = "401", description = "JWT ausente o inválido", content = @Content)
  @ApiResponse(
      responseCode = "500",
      description = "Fallo de lectura",
      content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
  public ResponseEntity<PlayerCatalogFiltersResponseDTO> filters() {
    return ResponseEntity.ok(PlayerMapper.toResponse(this.playerCatalogService.catalogFilters()));
  }

  @GetMapping("/{id}")
  @Operation(
      summary = "Consultar un jugador visible",
      description =
          "Consulta individual exclusivamente local por identificador interno, con el mismo criterio de visibilidad del catálogo: solo jugadores activos asociados a Team vigente y League válida. Un identificador inexistente, inactivo o no visible responde 404 con el mismo cuerpo, sin revelar la existencia del jugador. Expone los mismos campos que el catálogo, con dateOfBirth, nationality, imageUrl y fallbackImageUrl nullable. Requiere JWT válido.")
  @ApiResponse(responseCode = "200", description = "Jugador visible")
  @ApiResponse(
      responseCode = "400",
      description =
          "Identificador fuera de rango, no entero o no positivo (INVALID_PARAMETER_TYPE)",
      content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
  @ApiResponse(responseCode = "401", description = "JWT ausente o inválido", content = @Content)
  @ApiResponse(
      responseCode = "404",
      description = "Jugador inexistente, inactivo o no visible (PLAYER_NOT_FOUND)",
      content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
  public ResponseEntity<PlayerResponseDTO> getPlayer(
      @Parameter(description = "Identificador interno del jugador", required = true) @PathVariable
          Long id) {
    if (id <= 0) {
      throw new InvalidPlayerIdException();
    }
    return ResponseEntity.ok(
        PlayerMapper.toResponse(this.playerCatalogService.getVisiblePlayerById(id)));
  }
}
