package footballmarket.controllers;

import footballmarket.controllers.dtos.responses.ErrorResponseDTO;
import footballmarket.controllers.dtos.responses.PlayerRankingResponseDTO;
import footballmarket.controllers.dtos.responses.QuoteHistoryResponseDTO;
import footballmarket.controllers.dtos.responses.QuoteResponseDTO;
import footballmarket.controllers.mappers.QuoteMapper;
import footballmarket.services.QuoteQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Frontera HTTP de las consultas de cotización de un jugador.
 *
 * <p>Expone el historial completo, la cotización a una fecha dada, la cotización vigente y el
 * ranking de jugadores por precio vigente. Todas las rutas exigen un JWT válido, igual que el resto
 * de la API, y las consultas se resuelven por acceso directo al historial persistido, sin
 * recalcular precios.
 */
@RestController
@RequestMapping("/api/players")
@Tag(
    name = "Cotizaciones de jugadores",
    description = "Historial, cotización vigente y ranking por precio de los jugadores")
@SecurityRequirement(name = "bearerAuth")
public class PlayerQuoteController {
  private final QuoteQueryService queryService;

  public PlayerQuoteController(QuoteQueryService queryService) {
    this.queryService = queryService;
  }

  @GetMapping(value = "/{id}/quotes", params = "!date")
  @Operation(
      summary = "Consultar el historial de cotizaciones de un jugador",
      description =
          "Devuelve todas las filas persistidas del jugador, de la más reciente a la más antigua. "
              + "Un jugador sin cotizaciones devuelve una lista vacía, no un error. A igualdad de "
              + "fecha de vigencia, la fila recalculada se devuelve antes que la arrastrada y es la "
              + "cotización vigente del jugador.")
  @ApiResponse(
      responseCode = "200",
      description = "Historial de cotizaciones del jugador",
      content = @Content(schema = @Schema(implementation = QuoteHistoryResponseDTO.class)))
  @ApiResponse(
      responseCode = "400",
      description = "INVALID_PARAMETER_TYPE: identificador con formato inválido",
      content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
  @ApiResponse(responseCode = "401", description = "JWT ausente o inválido", content = @Content)
  public ResponseEntity<QuoteHistoryResponseDTO> history(
      @Parameter(description = "Identificador interno del jugador", required = true) @PathVariable
          Long id) {
    return ResponseEntity.ok(QuoteMapper.toHistoryResponse(id, this.queryService.findHistory(id)));
  }

  @GetMapping(value = "/{id}/quotes", params = "date")
  @Operation(
      summary = "Consultar la cotización de un jugador a una fecha dada",
      description =
          "Devuelve la última cotización cuya fecha de vigencia no supera la fecha consultada. Si el "
              + "jugador no tenía cotización anterior o igual a la fecha, responde 404 "
              + "QUOTE_NOT_FOUND.")
  @ApiResponse(
      responseCode = "200",
      description = "Cotización vigente a la fecha consultada",
      content = @Content(schema = @Schema(implementation = QuoteResponseDTO.class)))
  @ApiResponse(
      responseCode = "400",
      description = "INVALID_PARAMETER_TYPE: identificador o fecha con formato inválido",
      content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
  @ApiResponse(responseCode = "401", description = "JWT ausente o inválido", content = @Content)
  @ApiResponse(
      responseCode = "404",
      description = "QUOTE_NOT_FOUND: el jugador no tiene cotización a esa fecha",
      content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
  public ResponseEntity<QuoteResponseDTO> quoteOnDate(
      @Parameter(description = "Identificador interno del jugador", required = true) @PathVariable
          Long id,
      @Parameter(description = "Fecha consultada", required = true, example = "2026-10-20")
          @RequestParam
          LocalDate date) {
    return ResponseEntity.ok(QuoteMapper.toQuoteResponse(this.queryService.findOnDate(id, date)));
  }

  @GetMapping("/{id}/quotes/current")
  @Operation(
      summary = "Consultar la cotización vigente de un jugador",
      description =
          "Devuelve la última cotización persistida del jugador. El instante de cálculo es el de "
              + "esa cotización y nunca el de la lectura. Si el jugador nunca fue cotizado, responde "
              + "404 QUOTE_NOT_FOUND.")
  @ApiResponse(
      responseCode = "200",
      description = "Cotización vigente del jugador",
      content = @Content(schema = @Schema(implementation = QuoteResponseDTO.class)))
  @ApiResponse(
      responseCode = "400",
      description = "INVALID_PARAMETER_TYPE: identificador con formato inválido",
      content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
  @ApiResponse(responseCode = "401", description = "JWT ausente o inválido", content = @Content)
  @ApiResponse(
      responseCode = "404",
      description = "QUOTE_NOT_FOUND: el jugador no tiene cotización vigente",
      content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
  public ResponseEntity<QuoteResponseDTO> currentQuote(
      @Parameter(description = "Identificador interno del jugador", required = true) @PathVariable
          Long id) {
    return ResponseEntity.ok(QuoteMapper.toQuoteResponse(this.queryService.findCurrent(id)));
  }

  @GetMapping("/ranking")
  @Operation(
      summary = "Consultar el ranking de jugadores por cotización vigente",
      description =
          "Devuelve los jugadores cotizables ordenados por su precio vigente, de mayor a menor, con "
              + "una sola entrada por jugador. Un jugador cuyo precio vigente sea arrastrado aparece "
              + "con su precio repetido. Los jugadores sin métricas no aparecen porque no son "
              + "cotizables.")
  @ApiResponse(
      responseCode = "200",
      description = "Ranking de jugadores por cotización vigente",
      content = @Content(schema = @Schema(implementation = PlayerRankingResponseDTO.class)))
  @ApiResponse(responseCode = "401", description = "JWT ausente o inválido", content = @Content)
  public ResponseEntity<PlayerRankingResponseDTO> ranking() {
    return ResponseEntity.ok(QuoteMapper.toRankingResponse(this.queryService.findRanking()));
  }
}
