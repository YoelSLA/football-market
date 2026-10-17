package footballmarket.support;

import footballmarket.models.League;
import footballmarket.models.Player;
import footballmarket.models.PlayerToken;
import footballmarket.models.PlayerTokenEmission;
import footballmarket.models.Team;
import footballmarket.models.User;
import footballmarket.models.UserCredit;
import footballmarket.models.enums.UserRole;
import footballmarket.repositories.LeagueRepository;
import footballmarket.repositories.PlayerRepository;
import footballmarket.repositories.PlayerTokenEmissionRepository;
import footballmarket.repositories.PlayerTokenRepository;
import footballmarket.repositories.TeamRepository;
import footballmarket.repositories.UserCreditRepository;
import footballmarket.repositories.UserRepository;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Aprovisionamiento técnico de estado de mercado para pruebas.
 *
 * <p>Es infraestructura de pruebas y no un componente del dominio: existe para que los tests
 * funcionales no dependan de la persistencia. Los tests la usan únicamente en Arrange, y las
 * afirmaciones sobre estado durable se hacen en la categoría Repository, que sí puede observar la
 * base de datos, conforme a {@code docs/backend/testing.md} §5 y §6.
 */
@Component
public class MarketTestData {

  private final UserRepository users;
  private final LeagueRepository leagues;
  private final TeamRepository teams;
  private final PlayerRepository players;
  private final UserCreditRepository credits;
  private final PlayerTokenRepository holdings;
  private final PlayerTokenEmissionRepository emissions;

  private final AtomicLong sequence = new AtomicLong(900_000L);

  /**
   * Aprovisiona el acceso a la persistencia que necesitan las pruebas.
   *
   * <p>Los parámetros se nombran distinto del campo para asignarlos sin calificar con 'this', que
   * {@code docs/backend/testing.md} §16 prohíbe en todo el código de pruebas, incluidos los
   * helpers. Lombok no forma parte de las convenciones de prueba del proyecto.
   *
   * @param usersRepo repositorio de usuarios
   * @param leaguesRepo repositorio de ligas
   * @param teamsRepo repositorio de equipos
   * @param playersRepo repositorio de jugadores
   * @param creditsRepo repositorio de saldos
   * @param holdingsRepo repositorio de tenencias
   * @param emissionsRepo repositorio de emisiones
   */
  public MarketTestData(
      UserRepository usersRepo,
      LeagueRepository leaguesRepo,
      TeamRepository teamsRepo,
      PlayerRepository playersRepo,
      UserCreditRepository creditsRepo,
      PlayerTokenRepository holdingsRepo,
      PlayerTokenEmissionRepository emissionsRepo) {
    users = usersRepo;
    leagues = leaguesRepo;
    teams = teamsRepo;
    players = playersRepo;
    credits = creditsRepo;
    holdings = holdingsRepo;
    emissions = emissionsRepo;
  }

  /**
   * Crea un usuario normal con su cuenta de créditos inicial de 100 créditos.
   *
   * @return usuario persistido
   */
  @Transactional
  public User registerBuyer() {
    long id = sequence.incrementAndGet();
    User buyer = users.save(new User("buyer" + id, "buyer" + id + "@example.com", "password123"));
    credits.save(UserCredit.forUser(buyer.getId()));
    return buyer;
  }

  /**
   * Devuelve el superusuario del mercado, creándolo con saldo cero la primera vez.
   *
   * <p>El aprovisionamiento es idempotente porque la migración V7 impone un único SUPERUSER
   * mediante el índice {@code idx_users_single_superuser}. Crear uno por prueba violaría esa
   * invariante del dominio, de modo que se reutiliza el existente y se asegura su cuenta de
   * créditos.
   *
   * @return superusuario persistido
   */
  @Transactional
  public User registerSuperuser() {
    java.util.Optional<User> existing = users.findByRole(UserRole.SUPERUSER);
    if (existing.isPresent()) {
      User superuser = existing.get();
      if (!credits.existsById(superuser.getId())) {
        credits.save(UserCredit.forSuperuser(superuser.getId()));
      }
      return superuser;
    }

    long id = sequence.incrementAndGet();
    User superuser =
        users.save(
            new User(
                null,
                "owner" + id,
                "owner" + id + "@example.com",
                "password123",
                UserRole.SUPERUSER));
    credits.save(UserCredit.forSuperuser(superuser.getId()));
    return superuser;
  }

  /**
   * Acredita saldo a una cuenta ya existente.
   *
   * <p>Existe para que una prueba pueda satisfacer la precondición financiera que su criterio de
   * aceptación exige de forma expresa, por ejemplo comprar con «créditos suficientes y stock
   * inferior» para alcanzar el rechazo por inventario. No modifica el saldo inicial de producción:
   * {@link UserCredit#INITIAL_USER_CREDITS} sigue siendo el saldo con el que se encuentra el
   * comprador real, y la suma solo se realiza dentro de la prueba que la invoca.
   *
   * @param user usuario cuya cuenta se acredita
   * @param centesimas importe a acreditar en centésimas de crédito
   */
  @Transactional
  public void fund(User user, long centesimas) {
    UserCredit account = credits.findById(user.getId()).orElseThrow();
    account.credit(centesimas);
    credits.saveAndFlush(account);
  }

  /**
   * Crea un jugador activo y visible, con equipo y liga vigentes.
   *
   * @return jugador persistido
   */
  @Transactional
  public Player createVisiblePlayer() {
    return createPlayer(true, true);
  }

  /**
   * Crea un jugador existente pero no visible, con su equipo retirado del catálogo.
   *
   * @return jugador persistido e inactivo para el catálogo
   */
  @Transactional
  public Player createNotVisiblePlayer() {
    return createPlayer(false, false);
  }

  /**
   * Emite la tenencia inicial completa de un jugador en poder del superusuario.
   *
   * @param player jugador emitido
   * @param superuser propietario inicial del inventario
   */
  @Transactional
  public void emitInitialTokens(Player player, User superuser) {
    if (emissions.existsById(player.getId())) {
      return;
    }
    emissions.save(PlayerTokenEmission.issue(player.getId(), superuser));
    holdings.save(PlayerToken.issued(player, superuser, PlayerTokenEmission.EMITTED_TOKENS));
  }

  private Player createPlayer(boolean active, boolean currentTeam) {
    long id = sequence.incrementAndGet();
    League league = leagues.save(new League("Liga " + id));
    Team team = teams.save(new Team("Equipo " + id, league, currentTeam));

    Player player = new Player("Jugador " + id, team, "Delantero");
    if (!active) {
      player.deactivate();
    }
    return players.save(player);
  }
}
