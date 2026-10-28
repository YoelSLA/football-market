package footballmarket.support;

import footballmarket.features.auth.domain.User;
import footballmarket.features.catalog.domain.models.player.Player;
import footballmarket.features.market.domain.models.PlayerToken;
import footballmarket.features.market.domain.models.TokenListing;
import footballmarket.features.market.infraestructure.PlayerTokenRepository;
import footballmarket.features.market.infraestructure.TokenListingRepository;
import footballmarket.features.quote.domain.records.CurrentQuote;
import java.time.OffsetDateTime;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Aprovisionamiento de estado de mercado para las pruebas del portfolio.
 *
 * <p>Es infraestructure de pruebas: prepara tenencias, operaciones confirmadas y publicaciones
 * vigentes, y únicamente en Arrange. Las afirmaciones sobre el comportamiento observable las hace
 * el test de la categoría Service, que solo usa la API pública del caso de uso.
 */
@Component
public class PortfolioTestData {

  private final MarketTestData market;
  private final FinancialTestData financial;
  private final PlayerTokenRepository holdings;
  private final TokenListingRepository listings;

  public PortfolioTestData(
      MarketTestData market,
      FinancialTestData financial,
      PlayerTokenRepository holdings,
      TokenListingRepository listings) {
    this.market = market;
    this.financial = financial;
    this.holdings = holdings;
    this.listings = listings;
  }

  /** Usuario comprador con saldo inicial. */
  public User registerBuyer() {
    return this.market.registerBuyer();
  }

  /** Superusuario propietario del inventario inicial. */
  public User registerSuperuser() {
    return this.market.registerSuperuser();
  }

  /** Jugador activo y visible en el catálogo. */
  public Player createVisiblePlayer() {
    return this.market.createVisiblePlayer();
  }

  /** Emite el inventario inicial del jugador en poder del superusuario. */
  @Transactional
  public void emitInitialTokens(Player player, User superuser) {
    this.market.emitInitialTokens(player, superuser);
  }

  /**
   * Compra confirmada: registra la operación financiera y transfiere los tokens al comprador.
   *
   * @param buyer comprador
   * @param seller vendedor de los tokens
   * @param player jugador comprado
   * @param quantity tokens transferidos
   * @param price precio unitario en centésimas
   * @param executedAt instante de confirmación de la operación
   */
  @Transactional
  public void buy(
      User buyer, User seller, Player player, int quantity, long price, OffsetDateTime executedAt) {
    this.financial.record(buyer, seller, player, quantity, price, executedAt);
    this.transfer(seller, buyer, player, quantity);
  }

  /**
   * Venta confirmada: registra la operación financiera y transfiere los tokens al comprador.
   *
   * @param seller vendedor
   * @param buyer comprador de los tokens
   * @param player jugador vendido
   * @param quantity tokens transferidos
   * @param price precio unitario en centésimas
   * @param executedAt instante de confirmación de la operación
   */
  @Transactional
  public void sell(
      User seller, User buyer, Player player, int quantity, long price, OffsetDateTime executedAt) {
    this.financial.record(buyer, seller, player, quantity, price, executedAt);
    this.transfer(seller, buyer, player, quantity);
  }

  /**
   * Publicación vigente que reserva tokens del propietario sin transferirlos.
   *
   * @param seller propietario de la publicación
   * @param player jugador publicado
   * @param quantity tokens reservados
   * @param now instante de creación de la publicación
   */
  @Transactional
  public void publish(User seller, Player player, int quantity, OffsetDateTime now) {
    CurrentQuote quote = new CurrentQuote(player.getId(), 100L, now, "fixture");
    this.listings.save(TokenListing.publish(seller, player, quantity, quote, now, now.plusDays(1)));
  }

  /**
   * Prepara una tenencia que no coincide con las operaciones confirmadas, para comprobar que la
   * consulta detecta la divergencia en lugar de devolver cifras inconsistentes.
   *
   * <p>La tenencia existente se sobrescribe con otra cantidad: el par jugador y propietario es
   * único, de modo que insertar una tenencia nueva violaría esa unicidad en lugar de simular la
   * divergencia buscada.
   *
   * @param owner propietario de la tenencia
   * @param player jugador de la tenencia
   * @param quantity cantidad persistida, ajena a las operaciones
   */
  @Transactional
  public void breakHolding(User owner, Player player, int quantity) {
    PlayerToken existing =
        this.holdings.findByPlayerIdAndOwnerId(player.getId(), owner.getId()).orElseThrow();
    this.holdings.save(
        PlayerToken.restore(existing.getId(), player, owner, quantity, existing.getUpdatedAt()));
  }

  private void transfer(User from, User to, Player player, int quantity) {
    PlayerToken source = this.holding(from, player);
    PlayerToken target = this.holding(to, player);
    source.transferTo(quantity, target);
    this.holdings.save(source);
    this.holdings.save(target);
  }

  private PlayerToken holding(User owner, Player player) {
    return this.holdings
        .findByPlayerIdAndOwnerId(player.getId(), owner.getId())
        .orElseGet(() -> this.holdings.save(PlayerToken.emptyHolding(player, owner)));
  }
}
