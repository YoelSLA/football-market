package footballmarket.features.businessmetrics.presentation;

import footballmarket.features.businessmetrics.application.BusinessMetricsService;
import footballmarket.features.businessmetrics.presentation.dtos.response.BusinessMetricsResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Expone la consulta autenticada de agregados históricos de negocio. */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/business-metrics")
@Tag(name = "Métricas de negocio", description = "Agregados de actividad comercial")
@SecurityRequirement(name = "bearerAuth")
public class BusinessMetricsController {
  private final BusinessMetricsService businessMetricsService;

  /** Devuelve los agregados históricos calculados desde los registros persistidos. */
  @GetMapping("/historical")
  @Operation(
      summary = "Consultar métricas históricas de negocio",
      description =
          "Calcula compras, ventas, publicaciones canceladas, tokens y créditos desde registros"
              + " persistidos. Requiere autenticación Bearer; cualquier cuenta autenticada puede"
              + " acceder. Comparte el contrato con la métrica de ejecución actual: compras,"
              + " ventas y publicaciones canceladas son conteos; tokens transferidos es un"
              + " volumen numérico; créditos negociados se expresa en centésimas; queriedAt es"
              + " el instante UTC de consulta. No se exponen identidades individuales.")
  @ApiResponse(
      responseCode = "200",
      description = "Métricas históricas consultadas",
      content =
          @io.swagger.v3.oas.annotations.media.Content(
              schema = @Schema(implementation = BusinessMetricsResponseDTO.class)))
  @ApiResponse(responseCode = "401", description = "Autenticación ausente, inválida o no vigente")
  public ResponseEntity<BusinessMetricsResponseDTO> getHistoricalMetrics() {
    return ResponseEntity.ok(
        BusinessMetricsMapper.toResponse(this.businessMetricsService.getHistoricalMetrics()));
  }

  /** Devuelve los agregados acumulados durante la ejecución actual de la aplicación. */
  @GetMapping("/current")
  @Operation(
      summary = "Consultar métricas de la ejecución actual",
      description =
          "Lee compras, ventas, publicaciones canceladas, tokens y créditos acumulados por"
              + " Micrometer desde el arranque del proceso. Requiere autenticación Bearer;"
              + " cualquier cuenta autenticada puede acceder. Comparte el contrato con la métrica"
              + " histórica: compras, ventas y publicaciones canceladas son conteos; tokens"
              + " transferidos es un volumen numérico; créditos negociados se expresa en"
              + " centésimas; queriedAt es el instante UTC de consulta. No se exponen"
              + " identidades individuales.")
  @ApiResponse(
      responseCode = "200",
      description = "Métricas de la ejecución actual consultadas",
      content =
          @io.swagger.v3.oas.annotations.media.Content(
              schema = @Schema(implementation = BusinessMetricsResponseDTO.class)))
  @ApiResponse(responseCode = "401", description = "Autenticación ausente, inválida o no vigente")
  public ResponseEntity<BusinessMetricsResponseDTO> getCurrentMetrics() {
    return ResponseEntity.ok(
        BusinessMetricsMapper.toResponse(this.businessMetricsService.getCurrentMetrics()));
  }
}
