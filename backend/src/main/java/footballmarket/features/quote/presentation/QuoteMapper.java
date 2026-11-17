package footballmarket.features.quote.presentation;

import footballmarket.features.market.domain.records.MarketInitializationResult;
import footballmarket.features.quote.domain.models.PlayerQuote;
import footballmarket.features.quote.domain.records.PlayerRankingEntry;
import footballmarket.features.quote.domain.records.QuoteRankingPage;
import footballmarket.features.quote.domain.records.QuoteRankingQuery;
import footballmarket.features.quote.presentation.dtos.request.QuoteRankingQueryRequestDTO;
import footballmarket.features.quote.presentation.dtos.response.InitializeMarketResponseDTO;
import footballmarket.features.quote.presentation.dtos.response.PlayerRankingResponseDTO;
import footballmarket.features.quote.presentation.dtos.response.QuoteHistoryResponseDTO;
import footballmarket.features.quote.presentation.dtos.response.QuoteRankingPageResponseDTO;
import footballmarket.features.quote.presentation.dtos.response.QuoteResponseDTO;
import footballmarket.features.quote.presentation.dtos.response.RankingResponseDTO;
import java.util.List;

/**
 * Transformación estructural de las cotizaciones persistidas al contrato HTTP.
 *
 * <p>El mapeo no aplica reglas de negocio: traduce los campos de auditoría de cada fila.
 */
public final class QuoteMapper {
  private QuoteMapper() {}

  public static QuoteRankingQuery toRankingQuery(QuoteRankingQueryRequestDTO request) {
    return new QuoteRankingQuery(
        request.page(), request.size(), request.name(), request.leagueId(), request.position());
  }

  public static QuoteRankingPageResponseDTO toRankingPageResponse(QuoteRankingPage page) {
    return new QuoteRankingPageResponseDTO(
        page.content().stream()
            .map(
                entry ->
                    new QuoteRankingPageResponseDTO.Entry(
                        entry.position(),
                        entry.playerId(),
                        entry.price(),
                        entry.effectiveDate(),
                        entry.origin().name(),
                        entry.calculatedAt(),
                        toPlayerIdentity(entry.player())))
            .toList(),
        page.page(),
        page.size(),
        page.totalElements(),
        page.totalPages(),
        new QuoteRankingPageResponseDTO.FilterOptions(
            page.leagues().stream()
                .map(league -> new QuoteRankingPageResponseDTO.League(league.id(), league.name()))
                .toList(),
            page.positions()));
  }

  private static QuoteRankingPageResponseDTO.PlayerIdentity toPlayerIdentity(
      QuoteRankingPage.PlayerIdentity player) {
    return new QuoteRankingPageResponseDTO.PlayerIdentity(
        player.name(),
        player.position(),
        player.imageUrl(),
        player.team() == null
            ? null
            : new QuoteRankingPageResponseDTO.Team(
                player.team().id(), player.team().name(), player.team().crestUrl()),
        player.league() == null
            ? null
            : new QuoteRankingPageResponseDTO.League(player.league().id(), player.league().name()));
  }

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
   * <p>La posición procede del caso de uso, sin renumerarla en presentación.
   *
   * @param entries entradas del ranking, de mayor a menor precio
   * @return ranking del contrato de consulta
   */
  public static PlayerRankingResponseDTO toRankingResponse(List<PlayerRankingEntry> entries) {
    return new PlayerRankingResponseDTO(
        entries.stream()
            .map(
                entry ->
                    new RankingResponseDTO(
                        entry.position(),
                        entry.playerId(),
                        entry.price(),
                        entry.effectiveDate(),
                        entry.origin().name(),
                        entry.calculatedAt()))
            .toList());
  }
}
