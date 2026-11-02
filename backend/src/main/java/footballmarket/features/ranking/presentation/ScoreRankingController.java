package footballmarket.features.ranking.presentation;

import footballmarket.common.presentation.dtos.ErrorResponseDTO;
import footballmarket.features.ranking.application.ScoreRankingQueryService;
import footballmarket.features.ranking.presentation.dtos.response.ScoreRankingResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Frontera HTTP del ranking de jugadores por score. */
@RestController
@RequestMapping("/api/players")
@Tag(name = "Ranking por score", description = "Clasificación según la última estrategia cotizada")
@SecurityRequirement(name = "bearerAuth")
public class ScoreRankingController {
  private final ScoreRankingQueryService queryService;

  public ScoreRankingController(ScoreRankingQueryService queryService) {
    this.queryService = queryService;
  }

  @GetMapping("/ranking/score")
  @Operation(
      summary = "Consultar el ranking de jugadores por score",
      description =
          "Devuelve el score final más reciente de la estrategia registrada en la última "
              + "ejecución de cotización, sin recalcular ni modificar datos.")
  @ApiResponse(
      responseCode = "200",
      description = "Ranking por score o respuesta vacía cuando no hay información",
      content = @Content(schema = @Schema(implementation = ScoreRankingResponseDTO.class)))
  @ApiResponse(responseCode = "401", description = "JWT ausente o inválido", content = @Content)
  @ApiResponse(
      responseCode = "500",
      description = "INTERNAL_ERROR: no fue posible recuperar el ranking",
      content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
  public ResponseEntity<ScoreRankingResponseDTO> ranking() {
    return ResponseEntity.ok(ScoreRankingMapper.toResponse(this.queryService.findScoreRanking()));
  }
}
