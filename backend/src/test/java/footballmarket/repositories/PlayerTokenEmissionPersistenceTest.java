package footballmarket.repositories;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import footballmarket.features.auth.domain.User;
import footballmarket.features.catalog.application.PlayerCatalogService;
import footballmarket.features.catalog.domain.records.LeagueCandidate;
import footballmarket.features.catalog.domain.records.PlayerCandidate;
import footballmarket.features.catalog.domain.records.PlayerSnapshot;
import footballmarket.features.catalog.domain.records.PlayerSynchronizationResult;
import footballmarket.features.catalog.domain.records.TeamCandidate;
import footballmarket.features.market.application.BuyOrderService;
import footballmarket.features.market.domain.records.BuyOrderRequest;
import footballmarket.features.quote.application.QuoteSource;
import footballmarket.features.quote.domain.records.CurrentQuote;
import footballmarket.support.MarketTestData;
import footballmarket.support.ResetPlayerCatalogListener;
import footballmarket.support.TestcontainersConfiguration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * Emisión e inventario persistidos al crear y resolver jugadores mediante el catálogo real.
 *
 * <p>El contexto se renueva antes de esta clase y se cierra después de cada prueba; el listener del
 * catálogo reprovisiona únicamente PostgreSQL de Testcontainers entre casos. Así las fotos
 * completas no desactivan jugadores de otras pruebas. No hay transacción de test que oculte los
 * commits reales.
 */
@SpringBootTest
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
@TestExecutionListeners(
    listeners = ResetPlayerCatalogListener.class,
    mergeMode = TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS)
@org.junit.jupiter.api.Tag("integration")
class PlayerTokenEmissionPersistenceTest {
  private static final String EXTERNAL_ID = "h05-player";
  private static final long PRICE = 100L;

  @Autowired private PlayerCatalogService catalog;
  @Autowired private BuyOrderService orders;
  @Autowired private MarketTestData market;
  @Autowired private DataSource dataSource;
  @MockitoBean private QuoteSource quotes;

  private JdbcTemplate jdbc;
  private User seller;

  @BeforeEach
  void prepareSeller() {
    jdbc = new JdbcTemplate(dataSource);
    seller = market.registerSuperuser();
  }

  private PlayerSnapshot snapshot(String nationality) {
    LeagueCandidate league = new LeagueCandidate("h05-league", "Liga H05");
    TeamCandidate team = new TeamCandidate("h05-team", "Equipo H05", "h05-league", true);
    PlayerCandidate candidate =
        new PlayerCandidate(
            EXTERNAL_ID,
            "Jugador H05",
            "Equipo H05",
            "Liga H05",
            "Forward",
            null,
            nationality,
            "h05-team");
    return new PlayerSnapshot(List.of(candidate), 1, 0, List.of(league), List.of(team), List.of());
  }

  private List<Map<String, Object>> identities() {
    return jdbc.queryForList(
        "SELECT * FROM player_external_references"
            + " WHERE provider = 'FOOTBALL_DATA' AND external_id = ? ORDER BY id",
        EXTERNAL_ID);
  }

  private List<Map<String, Object>> emissions(long playerId) {
    return jdbc.queryForList("SELECT * FROM player_token_emissions WHERE player_id = ?", playerId);
  }

  private List<Map<String, Object>> holdings(long playerId) {
    return jdbc.queryForList(
        "SELECT * FROM player_tokens WHERE player_id = ? ORDER BY id", playerId);
  }

  private List<Map<String, Object>> audits(long playerId) {
    return jdbc.queryForList(
        "SELECT * FROM market_transactions WHERE player_id = ? ORDER BY id", playerId);
  }

  @Nested
  @DisplayName("Emisión por creación y resolución productiva")
  class CatalogEmission {
    @Test
    @DirtiesContext(methodMode = DirtiesContext.MethodMode.AFTER_METHOD)
    @DisplayName("Crear un jugador emite 100 tokens y repetir la foto conserva la emisión")
    void emiteUnaSolaVezDesdeElCatalogo() {
      // Arrange
      PlayerSnapshot photo = snapshot("Argentina");

      // Act: la transacción productiva REQUIRES_NEW concluye antes de las consultas JDBC.
      PlayerSynchronizationResult created = catalog.applySynchronization(photo);

      // Assert / Verify
      assertThat(created.created()).isEqualTo(1);
      List<Map<String, Object>> referencesBefore = identities();
      assertThat(referencesBefore).hasSize(1);
      long playerId = ((Number) referencesBefore.get(0).get("player_id")).longValue();
      assertThat(jdbc.queryForObject("SELECT count(*) FROM players", Long.class)).isEqualTo(1L);
      List<Map<String, Object>> emissionBefore = emissions(playerId);
      List<Map<String, Object>> holdingsBefore = holdings(playerId);
      assertThat(emissionBefore).hasSize(1);
      assertThat(emissionBefore.get(0))
          .containsEntry("player_id", playerId)
          .containsEntry("owner_user_id", seller.getId())
          .containsEntry("initial_quantity", 100);
      assertThat(emissionBefore.get(0).get("emitted_at")).isNotNull();
      assertThat(holdingsBefore).hasSize(1);
      assertThat(holdingsBefore.get(0))
          .containsEntry("owner_user_id", seller.getId())
          .containsEntry("quantity", 100);
      assertThat(audits(playerId)).isEmpty();

      // Act
      PlayerSynchronizationResult repeated = catalog.applySynchronization(photo);

      // Verify: filas completas, incluidos identificadores y timestamps.
      assertThat(repeated.created()).isZero();
      assertThat(repeated.updated()).isEqualTo(1);
      assertThat(identities()).isEqualTo(referencesBefore);
      assertThat(jdbc.queryForObject("SELECT count(*) FROM players", Long.class)).isEqualTo(1L);
      assertThat(emissions(playerId)).isEqualTo(emissionBefore);
      assertThat(holdings(playerId)).isEqualTo(holdingsBefore);
      assertThat(audits(playerId)).isEmpty();
    }

    @Test
    @DirtiesContext(methodMode = DirtiesContext.MethodMode.AFTER_METHOD)
    @DisplayName("Actualizar un jugador después de comprar no repone tokens ni altera finanzas")
    void actualizaElCatalogoSinReponerInventarioVendido() {
      // Arrange: el fixture solo aprovisiona usuarios; jugador e inventario nacen del catálogo.
      User buyer = market.registerBuyer();
      PlayerSnapshot initial = snapshot("Argentina");
      catalog.applySynchronization(initial);
      List<Map<String, Object>> referencesBefore = identities();
      assertThat(referencesBefore).hasSize(1);
      long playerId = ((Number) referencesBefore.get(0).get("player_id")).longValue();
      OffsetDateTime calculatedAt = OffsetDateTime.parse("2026-10-14T10:30:00Z");
      CurrentQuote quote = new CurrentQuote(playerId, PRICE, calculatedAt, "h05-strategy");
      when(quotes.currentQuote(playerId)).thenReturn(Optional.of(quote));
      // El mercado debe existir: sin fila de origen INITIAL la compra se rechaza antes de leer la
      // cotización.
      market.initializeMarket(playerId);
      long buyerBefore =
          jdbc.queryForObject(
              "SELECT available_credits FROM user_credits WHERE user_id = ?",
              Long.class,
              buyer.getId());
      long sellerBefore =
          jdbc.queryForObject(
              "SELECT available_credits FROM user_credits WHERE user_id = ?",
              Long.class,
              seller.getId());

      // Act: compra real confirmada antes de observar el estado.
      orders.buy(buyer.getEmail(), new BuyOrderRequest(playerId, 10));

      // Verify
      List<Map<String, Object>> holdingsAfterSale = holdings(playerId);
      assertThat(holdingsAfterSale).hasSize(2);
      assertThat(
              jdbc.queryForObject(
                  "SELECT quantity FROM player_tokens WHERE player_id = ? AND owner_user_id = ?",
                  Integer.class,
                  playerId,
                  seller.getId()))
          .isEqualTo(90);
      assertThat(
              jdbc.queryForObject(
                  "SELECT quantity FROM player_tokens WHERE player_id = ? AND owner_user_id = ?",
                  Integer.class,
                  playerId,
                  buyer.getId()))
          .isEqualTo(10);
      assertThat(
              jdbc.queryForObject(
                  "SELECT sum(quantity) FROM player_tokens WHERE player_id = ?",
                  Long.class,
                  playerId))
          .isEqualTo(100L);
      List<Map<String, Object>> accountsAfterSale =
          jdbc.queryForList("SELECT * FROM user_credits ORDER BY user_id");
      assertThat(
              jdbc.queryForObject(
                  "SELECT available_credits FROM user_credits WHERE user_id = ?",
                  Long.class,
                  buyer.getId()))
          .isEqualTo(buyerBefore - 1000L);
      assertThat(
              jdbc.queryForObject(
                  "SELECT available_credits FROM user_credits WHERE user_id = ?",
                  Long.class,
                  seller.getId()))
          .isEqualTo(sellerBefore + 1000L);
      List<Map<String, Object>> emissionAfterSale = emissions(playerId);
      assertThat(emissionAfterSale).hasSize(1);
      List<Map<String, Object>> auditAfterSale = audits(playerId);
      assertThat(auditAfterSale).hasSize(1);
      assertThat(auditAfterSale.get(0))
          .containsEntry("buyer_id", buyer.getId())
          .containsEntry("seller_id", seller.getId())
          .containsEntry("quantity", 10)
          .containsEntry("unit_price", PRICE)
          .containsEntry("total_amount", 1000L);

      // Arrange: misma identidad, cambia un dato opcional permitido por el catálogo.
      PlayerSnapshot changed = snapshot("Uruguay");

      // Act
      PlayerSynchronizationResult updated = catalog.applySynchronization(changed);

      // Verify: la actualización realmente ocurrió; no basta con una sincronización abstida.
      assertThat(updated.created()).isZero();
      assertThat(updated.updated()).isEqualTo(1);
      assertThat(identities()).isEqualTo(referencesBefore);
      assertThat(
              jdbc.queryForObject(
                  "SELECT nationality FROM players WHERE id = ?", String.class, playerId))
          .isEqualTo("Uruguay");
      assertThat(emissions(playerId)).isEqualTo(emissionAfterSale);
      assertThat(holdings(playerId)).isEqualTo(holdingsAfterSale);
      assertThat(jdbc.queryForList("SELECT * FROM user_credits ORDER BY user_id"))
          .isEqualTo(accountsAfterSale);
      assertThat(audits(playerId)).isEqualTo(auditAfterSale);
      assertThat(
              jdbc.queryForObject(
                  "SELECT sum(quantity) FROM player_tokens WHERE player_id = ?",
                  Long.class,
                  playerId))
          .isEqualTo(100L);
    }
  }
}
