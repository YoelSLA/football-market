package footballmarket.models;

import footballmarket.features.auth.domain.User;
import footballmarket.features.auth.domain.enums.UserRole;
import footballmarket.features.catalog.domain.models.league.League;
import footballmarket.features.catalog.domain.models.player.Player;
import footballmarket.features.catalog.domain.models.team.Team;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Construcciones mínimas de entidades para pruebas de dominio puras.
 *
 * <p>Solo se usan en tests unitarios sin persistencia: no acceden a la base ni a Spring, y producen
 * entidades válidas según las invariantes de cada modelo.
 */
public final class TestFixtures {

  private TestFixtures() {}

  /**
   * Construye un jugador activo asociado a un equipo vigente.
   *
   * @param id identificador interno-simulado del jugador
   * @return jugador válido
   */
  public static Player player(long id) {
    Player player = new Player("Jugador " + id, team(), "Delantero");
    ReflectionTestUtils.setField(player, "id", id);
    return player;
  }

  /**
   * Construye un jugador asociado a un equipo y liga con los identificadores indicados.
   *
   * @param id identificador del jugador
   * @param teamId identificador del equipo
   * @param leagueId identificador de la liga
   * @return jugador válido
   */
  public static Player player(long id, long teamId, long leagueId) {
    Player player = new Player("Jugador " + id, team(teamId, leagueId), "Delantero");
    ReflectionTestUtils.setField(player, "id", id);
    return player;
  }

  /**
   * Construye un equipo vigente con su liga.
   *
   * @return equipo válido
   */
  public static Team team() {
    return team(1L, 1L);
  }

  /**
   * Construye un equipo vigente con los identificadores indicados.
   *
   * @param teamId identificador del equipo
   * @param leagueId identificador de la liga
   * @return equipo válido y vigente
   */
  public static Team team(long teamId, long leagueId) {
    return new Team("Equipo " + teamId, new League("Liga " + leagueId), true);
  }

  /**
   * Construye un usuario normal.
   *
   * @param id identificador interno-simulado del usuario
   * @return usuario con rol USER
   */
  public static User user(long id) {
    return new User(id, "usuario" + id, "user" + id + "@example.com", "password123", UserRole.USER);
  }

  /**
   * Construye un superusuario.
   *
   * @param id identificador interno-simulado del usuario
   * @return usuario con rol SUPERUSER
   */
  public static User superuser(long id) {
    return new User(
        id, "superuser" + id, "superuser" + id + "@example.com", "password123", UserRole.SUPERUSER);
  }
}
