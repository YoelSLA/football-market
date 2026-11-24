package footballmarket.features.market.presentation;

import footballmarket.features.market.domain.records.MarketTrends;
import java.util.List;

/** Convierte el resultado de dominio al contrato HTTP de tendencias. */
public final class MarketTrendsMapper {
  private MarketTrendsMapper() {}

  /** Mapea métricas de mercado a la respuesta pública. */
  public static MarketTrendsResponseDTO toResponse(MarketTrends trends) {
    List<MarketTrendsResponseDTO.HotPlayerDTO> players =
        trends.topHotPlayers().stream()
            .map(
                player ->
                    new MarketTrendsResponseDTO.HotPlayerDTO(
                        player.playerId(),
                        player.playerName(),
                        player.boughtTokens(),
                        player.buyVolume()))
            .toList();
    return new MarketTrendsResponseDTO(
        new MarketTrendsResponseDTO.WindowDTO(
            trends.startsAt().toInstant(), trends.endsAt().toInstant()),
        players,
        trends.buyTransactionsPercentage(),
        trends.sellTransactionsPercentage(),
        trends.marketBias(),
        trends.totalTransactions(),
        trends.totalTradedTokens(),
        trends.totalTradedCredits());
  }
}
