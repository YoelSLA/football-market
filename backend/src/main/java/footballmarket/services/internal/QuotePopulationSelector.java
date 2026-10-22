package footballmarket.services.internal;

import footballmarket.models.Player;
import footballmarket.repositories.PlayerRepository;
import footballmarket.repositories.PlayerStatisticsRepository;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Población cotizable del mercado.
 *
 * <p>Es la de los jugadores con al menos una métrica informada en su último registro de métricas
 * persistidas. No depende de la cobertura de las valuaciones: un jugador nunca valuado sigue siendo
 * cotizable y un jugador sin métricas no entra al mercado.
 */
@Component
public class QuotePopulationSelector {

  private final PlayerStatisticsRepository statistics;
  private final PlayerRepository players;

  public QuotePopulationSelector(PlayerStatisticsRepository statistics, PlayerRepository players) {
    this.statistics = statistics;
    this.players = players;
  }

  /**
   * Jugadores cotizables, ordenados por identificador para que la ejecución sea determinista.
   *
   * @return jugadores con métricas informadas en su último registro
   */
  @Transactional(readOnly = true)
  public List<Player> pricablePlayers() {
    List<Long> ids = this.statistics.findPricablePlayerIds();
    if (ids.isEmpty()) {
      return List.of();
    }
    return this.players.findAllById(ids).stream()
        .sorted(Comparator.comparing(Player::getId))
        .toList();
  }
}
