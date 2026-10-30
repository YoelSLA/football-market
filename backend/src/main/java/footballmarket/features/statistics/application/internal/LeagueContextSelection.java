package footballmarket.features.statistics.application.internal;

import footballmarket.features.catalog.domain.models.player.Player;
import footballmarket.integrations.whoscored.config.WhoScoredProperties;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * Elige el contexto de un jugador: el de su liga local y, dentro de ella, la temporada vigente.
 *
 * <p>El proveedor devuelve las competiciones en las que el jugador tuvo actividad, incluidas copas
 * y temporadas anteriores. Solo se conserva el contexto de la liga que representa el equipo del
 * jugador, de modo que nunca se suman contextos distintos ni temporadas distintas. Un jugador sin
 * actividad en su liga no se descarta: se persiste sin métricas y con el contexto sin resolver.
 */
@Component
public class LeagueContextSelection {
  private final WhoScoredProperties properties;

  public LeagueContextSelection(WhoScoredProperties properties) {
    this.properties = properties;
  }

  /**
   * Contexto de la liga local del jugador, o vacío si su liga no publicó datos.
   *
   * <p>Cuando la liga tiene más de una temporada se conserva la de clave mayor, que es la vigente.
   */
  public Optional<PlayerMetricsContext> selectForLeague(
      Player player, List<PlayerMetricsContext> contexts) {
    Integer tournament = this.tournamentOf(player);
    if (tournament == null || contexts == null || contexts.isEmpty()) {
      return Optional.empty();
    }
    return contexts.stream()
        .filter(context -> String.valueOf(tournament).equals(context.competitionId()))
        .max(Comparator.comparing(context -> seasonRank(context.seasonKey())));
  }

  /** Identificador de competencia del proveedor equivalente a la liga local del jugador. */
  private Integer tournamentOf(Player player) {
    Long leagueId = leagueIdOf(player);
    if (leagueId == null) {
      return null;
    }
    // Se lee en cada llamada y no una vez por instancia: la correspondencia es configuración de
    // despliegue y puede variar sin que cambie el contexto de la aplicación.
    for (WhoScoredProperties.Competition competition : this.properties.competitions()) {
      if (competition.leagueId().equals(leagueId)) {
        return competition.tournamentId();
      }
    }
    return null;
  }

  private static Long leagueIdOf(Player player) {
    if (player == null || player.getTeam() == null || player.getTeam().getLeague() == null) {
      return null;
    }
    return player.getTeam().getLeague().getId();
  }

  /**
   * Orden de temporada del proveedor.
   *
   * <p>Las claves son numéricas y crecientes con el tiempo. Una clave no numérica se ordena al
   * final para no ganar por error una temporada vigente.
   */
  private static long seasonRank(String seasonKey) {
    if (seasonKey == null) {
      return Long.MIN_VALUE;
    }
    try {
      return Long.parseLong(seasonKey.strip());
    } catch (NumberFormatException notNumeric) {
      return Long.MIN_VALUE;
    }
  }
}
