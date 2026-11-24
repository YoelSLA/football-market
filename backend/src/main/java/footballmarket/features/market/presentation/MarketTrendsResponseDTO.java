package footballmarket.features.market.presentation;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;

/** Respuesta HTTP con la ventana y métricas de tendencia agregadas. */
@Schema(description = "Métricas agregadas de actividad del mercado en una ventana temporal")
public record MarketTrendsResponseDTO(
    WindowDTO window,
    List<HotPlayerDTO> topHotPlayers,
    Double buyTransactionsPercentage,
    Double sellTransactionsPercentage,
    String marketBias,
    long totalTransactions,
    long totalTradedTokens,
    long totalTradedCredits) {

  /** Límites UTC inclusivo/exclusivo de la consulta. */
  @Schema(description = "Ventana observada, inclusiva al inicio y exclusiva al final")
  public record WindowDTO(Instant startsAt, Instant endsAt) {}

  /** Jugador ordenado por volumen de compra en la ventana observada. */
  @Schema(description = "Jugador incluido en el ranking de volumen de compra")
  public record HotPlayerDTO(Long playerId, String playerName, long boughtTokens, long buyVolume) {}
}
