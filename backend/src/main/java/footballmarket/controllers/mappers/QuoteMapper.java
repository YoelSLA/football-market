package footballmarket.controllers.mappers;

import footballmarket.controllers.dtos.responses.InitializeMarketResponseDTO;
import footballmarket.controllers.dtos.responses.PlayerRankingResponseDTO;
import footballmarket.controllers.dtos.responses.QuoteHistoryResponseDTO;
import footballmarket.controllers.dtos.responses.QuoteResponseDTO;
import footballmarket.controllers.dtos.responses.RankingResponseDTO;
import footballmarket.models.PlayerQuote;
import footballmarket.models.records.MarketInitializationResult;
import footballmarket.models.records.PlayerRankingEntry;
import java.util.ArrayList;
import java.util.List;

/**
 * Transformación estructural de las cotizaciones persistidas al contrato HTTP.
 *
 * <p>El mapeo no aplica reglas de negocio: traduce los campos de auditoría de cada fila.
 */
public final class QuoteMapper {
  private QuoteMapper() {}

  /**
   * Mapea el resultado de la inicialización a su respuesta HTTP.
   *
   * @param result resultado observable de la inicialización
   * @return respuesta del contrato de inicialización
   */
  public static InitializeMarketResponseDTO toInitializeResponse(
      MarketInitializationResult result) {
    return new InitializeMarketResponseDTO(
        result.marketDate(), result.initializedPlayers(), result.alreadyInitialized());
  }

  /**
   * Mapea una fila del historial a su respuesta HTTP.
   *
   * @param quote fila persistida de la cotización
   * @return cotización del contrato de consulta
   */
  public static QuoteResponseDTO toQuoteResponse(PlayerQuote quote) {
    return new QuoteResponseDTO(
        quote.getEffectiveDate(),
        quote.getPrice(),
        quote.getPreviousPrice(),
        quote.getMultiplier(),
        quote.getScore(),
        quote.getStrategyName() == null ? null : quote.getStrategyName().name(),
        quote.getStrategyVersion(),
        quote.getSourceValuationDate(),
        quote.getOrigin().name(),
        quote.getCalculatedAt());
  }

  /**
   * Mapea el historial completo de un jugador a su respuesta HTTP.
   *
   * @param playerId identificador interno del jugador
   * @param quotes filas del historial, de la más reciente a la más antigua
   * @return historial del contrato de consulta
   */
  public static QuoteHistoryResponseDTO toHistoryResponse(Long playerId, List<PlayerQuote> quotes) {
    return new QuoteHistoryResponseDTO(
        playerId, quotes.stream().map(QuoteMapper::toQuoteResponse).toList());
  }

  /**
   * Mapea el ranking de jugadores por cotización vigente a su respuesta HTTP.
   *
   * <p>La posición es el orden de la entrada, desde uno.
   *
   * @param entries entradas del ranking, de mayor a menor precio
   * @return ranking del contrato de consulta
   */
  public static PlayerRankingResponseDTO toRankingResponse(List<PlayerRankingEntry> entries) {
    List<RankingResponseDTO> ranking = new ArrayList<>(entries.size());
    for (int index = 0; index < entries.size(); index++) {
      PlayerRankingEntry entry = entries.get(index);
      ranking.add(
          new RankingResponseDTO(
              index + 1,
              entry.playerId(),
              entry.price(),
              entry.effectiveDate(),
              entry.origin().name(),
              entry.calculatedAt()));
    }
    return new PlayerRankingResponseDTO(List.copyOf(ranking));
  }
}
