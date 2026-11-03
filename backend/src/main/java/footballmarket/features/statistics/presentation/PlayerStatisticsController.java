package footballmarket.features.statistics.presentation;

import footballmarket.common.presentation.dtos.ErrorResponseDTO;
import footballmarket.features.statistics.application.PlayerStatisticsSynchronizationService;
import footballmarket.features.statistics.domain.records.PlayerStatisticsSyncRequest;
import footballmarket.features.statistics.presentation.dtos.request.PlayerStatisticsSyncRequestDTO;
import footballmarket.features.statistics.presentation.dtos.response.PlayerStatisticsSyncResponseDTO;
import footballmarket.features.statistics.presentation.exceptions.InvalidStatisticsRequestException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Frontera HTTP de los dos canales de sincronización de estadísticas.
 *
 * <p>La ruta determina el canal: el body solo puede fijar la fecha de scraping y nunca cambia el
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

  public PlayerStatisticsController(PlayerStatisticsSynchronizationService synchronization) {
    this.synchronization = synchronization;
  }

  @PostMapping("/sync/scraping")
  @Operation(
      summary = "Actualizar métricas desde la interfaz web de Whoscored",
      description =
          "POST síncrono con body obligatorio y fecha opcional. Sin máximo ni deadline total; "
              + "intenta una vez cada pendiente inicial. maxPlayers, incluso null, se rechaza. No acepta identificadores "
              + "de jugadores ni canal: la ruta fija SCRAPING. No confirma datos parciales.")
  @ApiResponse(
      responseCode = "200",
      description =
          "Resumen SCRAPING terminado COMPLETED, PARTIAL, FAILED o INTERRUPTED; 200 no implica "
              + "cobertura completa si quedan pendientes",
      content = @Content(schema = @Schema(implementation = PlayerStatisticsSyncResponseDTO.class)))
  @ApiResponse(
      responseCode = "400",
      description = "INVALID_STATISTICS_EXTRACTION_DATE o INVALID_REQUEST",
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
        new PlayerStatisticsSyncRequest(request.extractionDate());
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
