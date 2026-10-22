package footballmarket.integrations;

import footballmarket.models.PlayerQuote;
import footballmarket.models.records.CurrentQuote;
import footballmarket.repositories.PlayerQuoteRepository;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Fuente de cotización real, sostenida por el repositorio canónico de cotizaciones.
 *
 * <p>Es el adaptador del puerto {@link QuoteSource} contra la tabla {@code player_quotes}: devuelve
 * la fila vigente del jugador, la de fecha de vigencia máxima y, a igualdad de fecha, la {@code
 * RECOMPUTED}, que fue escrita después que la arrastrada. No inventa datos: un jugador sin
 * cotización se devuelve como ausencia y el mercado la traduce al error de cotización no
 * disponible.
 *
 * <p>El instante que acompaña a la cotización es el instante de cálculo de esa fila, no el de la
 * lectura, de modo que una cotización antigua siga siendo válida mientras sea la última vigente. La
 * fila inicial no tiene estrategia asociada y por eso su versión se conserva nula.
 */
@Component
@RequiredArgsConstructor
public class RepositoryQuoteSource implements QuoteSource {

  private final PlayerQuoteRepository quotes;

  /** {@inheritDoc} */
  @Override
  public Optional<CurrentQuote> currentQuote(Long playerId) {
    return this.quotes.findLatestByPlayerId(playerId).stream()
        .findFirst()
        .map(quote -> this.toCurrentQuote(playerId, quote));
  }

  /**
   * Adquiere en lote la cotización vigente de varios jugadores.
   *
   * <p>Resuelve el mismo lote con una sola consulta, sin llamadas individuales por jugador. Los
   * jugadores sin cotización se omiten del resultado, tal como establece el contrato del puerto.
   *
   * @param playerIds identificadores internos de los jugadores consultados
   * @return mapa de instantáneas vigentes, con las claves omitidas para los jugadores sin
   *     cotización
   */
  @Override
  public Map<Long, CurrentQuote> currentQuotes(Set<Long> playerIds) {
    if (playerIds == null || playerIds.isEmpty()) {
      return Map.of();
    }
    Map<Long, CurrentQuote> acquired = new LinkedHashMap<>();
    for (PlayerQuote quote : this.quotes.findRanking(new ArrayList<>(playerIds))) {
      Long playerId = quote.getPlayer().getId();
      acquired.putIfAbsent(playerId, this.toCurrentQuote(playerId, quote));
    }
    return Map.copyOf(acquired);
  }

  /**
   * Traduce la fila vigente a la instantánea que consume el mercado.
   *
   * @param playerId identificador interno del jugador consultado
   * @param quote fila vigente del jugador
   * @return instantánea de la cotización vigente
   */
  private CurrentQuote toCurrentQuote(Long playerId, PlayerQuote quote) {
    return new CurrentQuote(
        playerId,
        quote.getPrice(),
        quote.getCalculatedAt().atOffset(ZoneOffset.UTC),
        quote.getStrategyVersion());
  }
}
