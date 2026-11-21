package footballmarket.features.dashboard.presentation;

import footballmarket.common.presentation.dtos.ErrorResponseDTO;
import footballmarket.features.dashboard.application.DashboardOverviewService;
import footballmarket.features.dashboard.application.DashboardPerformanceService;
import footballmarket.features.dashboard.presentation.dtos.response.DashboardOverviewResponseDTO;
import footballmarket.features.dashboard.presentation.dtos.response.DashboardPerformanceResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Frontera HTTP del Dashboard interactivo y resumen ejecutivo. */
@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(
    name = "Dashboard",
    description = "Cockpit financiero y deportivo integrado estilo Bloomberg y Sorare.")
public class DashboardController {

  private final DashboardOverviewService overviewService;
  private final DashboardPerformanceService performanceService;

  @Operation(
      summary = "Consultar resumen consolidado del dashboard",
      description =
          "Devuelve las métricas consolidadas del usuario (Net Worth, P&L, liquidez, custodia), distribución de cartera, pulso de mercado, top movers, value picks, mejores ofertas P2P y transacciones recientes en una sola llamada optimizada.")
  @ApiResponse(
      responseCode = "200",
      description = "Resumen del dashboard generado exitosamente.",
      content = @Content(schema = @Schema(implementation = DashboardOverviewResponseDTO.class)))
  @ApiResponse(
      responseCode = "401",
      description = "Sesión no autenticada, ausente o inválida.",
      content = @Content)
  @ApiResponse(
      responseCode = "500",
      description = "INTERNAL_ERROR: no fue posible consolidar las métricas.",
      content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
  @GetMapping("/overview")
  public ResponseEntity<DashboardOverviewResponseDTO> overview(@AuthenticationPrincipal Jwt jwt) {
    return ResponseEntity.ok(this.overviewService.getOverview(jwt.getSubject()));
  }

  @Operation(
      summary = "Consultar series temporales de rendimiento y benchmark FMI",
      description =
          "Devuelve la serie cronológica del valor del portfolio del usuario comparada contra el Football Market Index (FMI base 100) según el rango temporal solicitado.")
  @ApiResponse(
      responseCode = "200",
      description = "Series de rendimiento y benchmark generadas.",
      content = @Content(schema = @Schema(implementation = DashboardPerformanceResponseDTO.class)))
  @ApiResponse(responseCode = "401", description = "Sesión no autenticada.", content = @Content)
  @GetMapping("/performance")
  public ResponseEntity<DashboardPerformanceResponseDTO> performance(
      @AuthenticationPrincipal Jwt jwt,
      @Parameter(description = "Rango temporal: 1M, 3M o ALL.")
          @RequestParam(required = false, defaultValue = "1M")
          String range) {
    return ResponseEntity.ok(this.performanceService.getPerformance(jwt.getSubject(), range));
  }
}
