package footballmarket.controllers;

import footballmarket.controllers.dtos.requests.PlayerStatisticsSyncRequestDTO;
import footballmarket.controllers.dtos.responses.ErrorResponseDTO;
import footballmarket.controllers.dtos.responses.PlayerStatisticsCoverageResponseDTO;
import footballmarket.controllers.dtos.responses.PlayerStatisticsSyncResponseDTO;
import footballmarket.controllers.exceptions.InvalidStatisticsRequestException;
import footballmarket.controllers.mappers.PlayerStatisticsCoverageMapper;
import footballmarket.controllers.mappers.PlayerStatisticsSyncMapper;
import footballmarket.models.enums.MetricSource;
import footballmarket.models.records.PlayerStatisticsSyncRequest;
import footballmarket.services.PlayerStatisticsCoverageService;
import footballmarket.services.PlayerStatisticsSynchronizationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.time.ZoneOffset;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Frontera HTTP de los dos canales de estadísticas y de la consulta de cobertura.
 *
 * <p>La ruta determina el canal: el body solo puede acotar el lote de scraping y nunca cambia el
 * canal. El Controller valida estructura y transporte, y delega el caso de uso en la fachada.
 */
@RestController
@RequestMapping("/api/players/statistics/whoscored")
@Tag(
    name = "Estadísticas WhoScored",
    description = "Sincronización de métricas de rendimiento por scraping o interfaz JSON")
@SecurityRequirement(name = "bearerAuth")
public class PlayerStatisticsController {
  private final PlayerStatisticsSynchronizationService synchronization;
  private final PlayerStatisticsCoverageService coverage;

  public PlayerStatisticsController(
      PlayerStatisticsSynchronizationService synchronization,
      PlayerStatisticsCoverageService coverage) {
    this.synchronization = synchronization;
    this.coverage = coverage;
  }

  @PostMapping("/sync/scraping")
  @Operation(
      summary = "Ejecutar un lote de scraping de estadísticas",
      description =
          "POST síncrono con body obligatorio. Fecha y máximo de jugadores opcionales; el máximo es "
              + "global entre workers y 1 limita el lote a un único pendiente. No acepta identificadores "
              + "de jugadores ni canal: la ruta fija SCRAPING. No confirma datos parciales.")
  @ApiResponse(
      responseCode = "200",
      description =
          "Resumen SCRAPING terminado COMPLETED, PARTIAL, FAILED o INTERRUPTED; 200 no implica "
              + "cobertura completa si quedan pendientes",
      content = @Content(schema = @Schema(implementation = PlayerStatisticsSyncResponseDTO.class)))
  @ApiResponse(
      responseCode = "400",
      description =
          "INVALID_STATISTICS_EXTRACTION_DATE, INVALID_STATISTICS_MAX_PLAYERS o INVALID_REQUEST",
      content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
  @ApiResponse(
      responseCode = "401",
      description = "JWT ausente, expirado o inválido",
      content = @Content)
  @ApiResponse(
      responseCode = "409",
      description = "PLAYER_STATISTICS_SYNC_IN_PROGRESS: ya hay una ejecución activa del canal",
      content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
  @ApiResponse(
      responseCode = "500",
      description = "PLAYER_STATISTICS_SYNC_FAILED",
      content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
  @ApiResponse(
      responseCode = "503",
      description = "HEADED_BROWSER_UNAVAILABLE: sin navegador visible ni sesión gráfica",
      content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
  public ResponseEntity<PlayerStatisticsSyncResponseDTO> synchronizeScraping(
      @Valid @RequestBody PlayerStatisticsSyncRequestDTO request) {
    PlayerStatisticsSyncRequest domainRequest =
        new PlayerStatisticsSyncRequest(request.extractionDate(), request.maxPlayers());
    return ResponseEntity.ok(
        PlayerStatisticsSyncMapper.toResponse(
            this.synchronization.synchronizeScraping(domainRequest)));
  }

  @PostMapping("/sync/json")
  @Operation(
      summary = "Actualizar métricas desde la interfaz JSON del proveedor",
      description =
          "POST síncrono sin query parameters ni body. Cualquier parámetro o body no vacío se rechaza "
              + "con INVALID_REQUEST sin invocar el caso de uso. La fecha UTC se fija al iniciar.")
  @ApiResponse(
      responseCode = "200",
      description = "Resumen JSON terminado de la campaña del día UTC",
      content = @Content(schema = @Schema(implementation = PlayerStatisticsSyncResponseDTO.class)))
  @ApiResponse(
      responseCode = "400",
      description = "INVALID_REQUEST: entrada no permitida en esta ruta",
      content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
  @ApiResponse(
      responseCode = "401",
      description = "JWT ausente, expirado o inválido",
      content = @Content)
  @ApiResponse(
      responseCode = "409",
      description = "PLAYER_STATISTICS_SYNC_IN_PROGRESS: ya hay una ejecución activa del canal",
      content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
  @ApiResponse(
      responseCode = "500",
      description = "PLAYER_STATISTICS_SYNC_FAILED",
      content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
  public ResponseEntity<PlayerStatisticsSyncResponseDTO> synchronizeJson(
      HttpServletRequest request) {
    this.rejectJsonInput(request);
    return ResponseEntity.ok(
        PlayerStatisticsSyncMapper.toResponse(this.synchronization.synchronizeJson()));
  }

  @GetMapping("/coverage")
  @Operation(
      summary = "Consultar la cobertura de la campaña",
      description =
          "Consulta cobertura por canal y fecha sin acceder al proveedor. Los fallidos permanecen "
              + "incluidos en los pendientes. Una fecha sin ejecuciones devuelve el catálogo vigente.")
  @ApiResponse(
      responseCode = "200",
      description = "Cobertura del catálogo para la fecha y el canal consultados",
      content =
          @Content(schema = @Schema(implementation = PlayerStatisticsCoverageResponseDTO.class)))
  @ApiResponse(
      responseCode = "400",
      description = "INVALID_STATISTICS_SOURCE o INVALID_STATISTICS_EXTRACTION_DATE",
      content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
  @ApiResponse(
      responseCode = "401",
      description = "JWT ausente, expirado o inválido",
      content = @Content)
  public ResponseEntity<PlayerStatisticsCoverageResponseDTO> coverage(
      @RequestParam String source,
      @org.springframework.web.bind.annotation.RequestParam(required = false)
          LocalDate extractionDate) {
    LocalDate date = extractionDate == null ? LocalDate.now(ZoneOffset.UTC) : extractionDate;
    if (date.isAfter(LocalDate.now(ZoneOffset.UTC))) {
      throw new InvalidStatisticsRequestException(
          "INVALID_STATISTICS_EXTRACTION_DATE", "La fecha de extracción no puede ser futura");
    }
    return ResponseEntity.ok(
        PlayerStatisticsCoverageMapper.toResponse(
            this.coverage.coverage(date, this.channel(source))));
  }

  private MetricSource channel(String source) {
    try {
      return MetricSource.valueOf(source.strip().toUpperCase(java.util.Locale.ROOT));
    } catch (RuntimeException invalid) {
      throw new InvalidStatisticsRequestException(
          "INVALID_STATISTICS_SOURCE", "El canal debe ser SCRAPING o JSON");
    }
  }

  /** La entrada JSON no admite parámetros ni body; el rechazo ocurre antes de la fachada. */
  private void rejectJsonInput(HttpServletRequest request) {
    boolean parameters = request.getParameterMap() != null && !request.getParameterMap().isEmpty();
    boolean body =
        request.getContentLengthLong() > 0 || request.getHeader("Transfer-Encoding") != null;
    if (parameters || body) {
      throw new InvalidStatisticsRequestException(
          "INVALID_REQUEST", "Esta operación no admite parámetros ni cuerpo");
    }
  }
}
