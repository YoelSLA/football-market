package footballmarket.controllers;

import footballmarket.controllers.dtos.requests.InitializeQuotesRequestDTO;
import footballmarket.controllers.dtos.requests.RecalculateQuotesRequestDTO;
import footballmarket.controllers.dtos.responses.ErrorResponseDTO;
import footballmarket.controllers.dtos.responses.InitializeMarketResponseDTO;
import footballmarket.controllers.dtos.responses.QuoteExecutionResponseDTO;
import footballmarket.controllers.dtos.responses.QuoteExecutionsResponseDTO;
import footballmarket.controllers.mappers.QuoteExecutionMapper;
import footballmarket.controllers.mappers.QuoteMapper;
import footballmarket.services.QuoteMarketInitializationService;
import footballmarket.services.QuoteRecalculationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.LocalDate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Frontera HTTP de la inicialización del mercado de cotizaciones.
 *
 * <p>La operación exige un JWT válido, igual que el resto de las rutas de la API, y delega el caso
 * de uso en el servicio de inicialización.
 */
@RestController
@RequestMapping("/api/quotes")
@Tag(name = "Cotizaciones", description = "Inicialización y recálculo del mercado de cotizaciones")
@SecurityRequirement(name = "bearerAuth")
public class QuoteController {
  private final QuoteMarketInitializationService initializationService;
  private final QuoteRecalculationService recalculationService;

  public QuoteController(
      QuoteMarketInitializationService initializationService,
      QuoteRecalculationService recalculationService) {
    this.initializationService = initializationService;
    this.recalculationService = recalculationService;
  }

  @PostMapping("/initialize")
  @Operation(
      summary = "Inicializar el mercado de cotizaciones",
      description =
          "Fija la primera fecha de mercado y deja una cotización inicial de 100 centésimas a cada "
              + "jugador cotizable. Es idempotente en la misma fecha de mercado y se rechaza en "
              + "cualquier otra fecha.")
  @ApiResponse(
      responseCode = "200",
      description = "Estado del mercado tras la inicialización",
      content = @Content(schema = @Schema(implementation = InitializeMarketResponseDTO.class)))
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
      description = "MARKET_ALREADY_INITIALIZED",
      content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
  @ApiResponse(
      responseCode = "500",
      description = "INTERNAL_ERROR",
      content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
  public ResponseEntity<InitializeMarketResponseDTO> initialize(
      @Valid @RequestBody(required = false) InitializeQuotesRequestDTO request) {
    return ResponseEntity.ok(
        QuoteMapper.toInitializeResponse(
            this.initializationService.initialize(request == null ? null : request.marketDate())));
  }

  @PostMapping("/recalculate")
  @Operation(
      summary = "Recalcular las cotizaciones",
      description =
          "Ejecuta una única corrida para la fecha UTC actual con la estrategia y los límites "
              + "indicados. Todos los parámetros salvo la estrategia tienen valores por defecto.")
  @ApiResponse(
      responseCode = "200",
      description = "Resumen persistido de la corrida",
      content = @Content(schema = @Schema(implementation = QuoteExecutionResponseDTO.class)))
  @ApiResponse(
      responseCode = "400",
      description =
          "UNKNOWN_QUOTE_STRATEGY, INVALID_ADJUSTMENT_FACTOR, INVALID_VARIATION_CAP o "
              + "INVALID_MIN_PRICE_FLOOR",
      content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
  @ApiResponse(
      responseCode = "409",
      description = "MARKET_NOT_INITIALIZED o EXECUTION_ALREADY_EXISTS",
      content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
  @ApiResponse(responseCode = "401", description = "JWT ausente o inválido", content = @Content)
  @ApiResponse(
      responseCode = "500",
      description = "INTERNAL_ERROR",
      content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
  public ResponseEntity<QuoteExecutionResponseDTO> recalculate(
      @Valid @RequestBody RecalculateQuotesRequestDTO request) {
    return ResponseEntity.ok(
        QuoteExecutionMapper.toResponse(
            this.recalculationService.recalculate(
                request == null ? null : request.strategy(),
                request == null ? null : request.adjustmentFactor(),
                request == null ? null : request.variationCap(),
                request == null ? null : request.minPriceFloor())));
  }

  @GetMapping("/executions")
  @Operation(
      summary = "Consultar ejecuciones por fecha de cotización",
      description =
          "Devuelve el resumen de las ejecuciones de la fecha consultada. Como máximo hay una "
              + "ejecución por fecha de cotización; una fecha sin ejecuciones devuelve una lista "
              + "vacía, no un error.")
  @ApiResponse(
      responseCode = "200",
      description = "Ejecuciones de la fecha",
      content = @Content(schema = @Schema(implementation = QuoteExecutionsResponseDTO.class)))
  @ApiResponse(
      responseCode = "400",
      description = "INVALID_REQUEST: fecha ausente o malformada",
      content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
  @ApiResponse(responseCode = "401", description = "JWT ausente o inválido", content = @Content)
  public ResponseEntity<QuoteExecutionsResponseDTO> executions(
      @RequestParam("date") LocalDate date) {
    return ResponseEntity.ok(
        QuoteExecutionMapper.toResponse(this.recalculationService.executions(date)));
  }
}
