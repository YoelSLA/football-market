package footballmarket.controllers;

import footballmarket.controllers.dtos.responses.ErrorResponseDTO;
import footballmarket.controllers.dtos.responses.ValuationEvaluationResponseDTO;
import footballmarket.controllers.exceptions.InvalidValuationRequestException;
import footballmarket.controllers.mappers.ValuationMapper;
import footballmarket.services.ValuationEvaluationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Frontera HTTP para iniciar la evaluación estadística de toda la población. */
@RestController
@RequestMapping("/api/valuations")
@Tag(name = "Valoraciones", description = "Evaluación estadística poblacional versionada")
@SecurityRequirement(name = "bearerAuth")
public class ValuationController {
  private final ValuationEvaluationService service;

  public ValuationController(ValuationEvaluationService service) {
    this.service = service;
  }

  @PostMapping("/evaluate")
  @Operation(
      summary = "Evaluar toda la población vigente",
      description = "Operación síncrona sin body ni query que publica ambas estrategias v1.")
  @ApiResponse(
      responseCode = "200",
      description = "Resumen agregado de la evaluación",
      content = @Content(schema = @Schema(implementation = ValuationEvaluationResponseDTO.class)))
  @ApiResponse(
      responseCode = "400",
      description = "INVALID_REQUEST",
      content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
  @ApiResponse(
      responseCode = "401",
      description = "JWT ausente, expirado, inválido o usuario no persistido",
      content = @Content)
  @ApiResponse(
      responseCode = "409",
      description = "VALUATION_SNAPSHOT_UNAVAILABLE",
      content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
  @ApiResponse(
      responseCode = "500",
      description = "VALUATION_EVALUATION_FAILED",
      content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
  public ResponseEntity<ValuationEvaluationResponseDTO> evaluate(HttpServletRequest request) {
    boolean parameters = request.getParameterMap() != null && !request.getParameterMap().isEmpty();
    boolean body =
        request.getContentLengthLong() > 0 || request.getHeader("Transfer-Encoding") != null;
    if (parameters || body) {
      throw new InvalidValuationRequestException();
    }
    return ResponseEntity.ok(ValuationMapper.toResponse(this.service.evaluate()));
  }
}
