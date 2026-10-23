package footballmarket.repositories;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import footballmarket.integrations.QuoteSource;
import footballmarket.models.MarketTransaction;
import footballmarket.models.Player;
import footballmarket.models.PlayerToken;
import footballmarket.models.PlayerTokenEmission;
import footballmarket.models.User;
import footballmarket.models.UserCredit;
import footballmarket.models.records.BuyOrderRequest;
import footballmarket.models.records.CurrentQuote;
import footballmarket.services.AuthenticationService;
import footballmarket.services.BuyOrderService;
import footballmarket.services.exceptions.InsufficientCreditsException;
import footballmarket.services.exceptions.InsufficientStockException;
import footballmarket.support.MarketTestData;
import footballmarket.support.TestcontainersConfiguration;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Categoría Repository del dominio de mercado.
 *
 * <p>Es la única categoría que puede observar el estado durable: créditos, tenencias, auditoría,
 * restricciones, idempotencia y concurrencia. Invoca el Service real para provocar las operaciones
 * y afirma sobre la persistencia, conforme a {@code docs/backend/testing.md} §4. No sustituye a las
 * pruebas funcionales del Service.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
@org.junit.jupiter.api.Tag("integration")
class MarketDomainPersistenceTest {
  private static final int QUANTITY = 10;
  private static final long PRICE = 100L;
  private static final int EMITTED = PlayerTokenEmission.EMITTED_TOKENS;

  /** Espera acotada para liberar a la vez las dos transacciones de una carrera. */
  private static final long BARRIER_TIMEOUT_SECONDS = 15L;

  /** Espera acotada para obtener el resultado de cada transacción concurrente. */
  private static final long OUTCOME_TIMEOUT_SECONDS = 30L;

  /**
   * Resultado observado de una compra concurrente.
   *
   * <p>Cada valor procede de una captura explícita del tipo de excepción, nunca de su nombre: dos
   * excepciones distintas pueden llamarse igual y una comparación de texto aceptaría como previsto
   * un fallo que el contrato no contempla.
   */
  private enum Outcome {
    /** Compra confirmada. */
    CONFIRMED,
    /** Rechazo por inventario insuficiente del superusuario. */
    REJECTED_STOCK,
    /** Rechazo por saldo insuficiente del comprador. */
    REJECTED_CREDITS
  }

  @Autowired private BuyOrderService buyOrderService;
  @Autowired private footballmarket.services.TokenListingService listingService;
  @Autowired private TokenListingRepository listings;
  @Autowired private AuthenticationService authenticationService;
  @Autowired private MarketTestData market;
  @Autowired private UserCreditRepository credits;
  @Autowired private PlayerTokenRepository holdings;
  @Autowired private PlayerTokenEmissionRepository emissions;
  @Autowired private MarketTransactionRepository transactions;
  @Autowired private PlatformTransactionManager transactionManager;
  @Autowired private javax.sql.DataSource dataSource;

  @MockitoBean private QuoteSource quotes;

  private User buyer;
  private User superuser;
  private Player player;

  @BeforeEach
  void arrange() {
    buyer = market.registerBuyer();
    superuser = market.registerSuperuser();
    player = market.createVisiblePlayer();
    market.emitInitialTokens(player, superuser);
    market.initializeMarket(player);
    givenQuote(PRICE);
  }

  private void givenQuote(long price) {
    org.mockito.Mockito.when(quotes.currentQuote(org.mockito.ArgumentMatchers.any()))
        .thenAnswer(
            invocation -> {
              Long playerId = invocation.getArgument(0);
              return Optional.of(
                  new CurrentQuote(
                      playerId, price, OffsetDateTime.now(ZoneOffset.UTC), "strategy-v1"));
            });
  }

  /** Sustituye la cotización de `arrange` por una ausencia, para examinar AC06. */
  private void givenNoQuote() {
    org.mockito.Mockito.when(quotes.currentQuote(org.mockito.ArgumentMatchers.any()))
        .thenReturn(Optional.empty());
  }

  private void buy(Player target, int quantity) {
    buyOrderService.buy(buyer.getEmail(), new BuyOrderRequest(target.getId(), quantity));
  }

  private org.springframework.jdbc.core.JdbcTemplate jdbc() {
    return new org.springframework.jdbc.core.JdbcTemplate(dataSource);
  }

  private long creditOf(User user) {
    return credits.findById(user.getId()).orElseThrow().getAvailableCredits();
  }

  private int holdingOf(Player target, User owner) {
    return holdings
        .findByPlayerIdAndOwnerId(target.getId(), owner.getId())
        .orElseThrow()
        .getQuantity();
  }

  /**
   * Tenencia del comprador en poder de ese jugador, presente o no.
   *
   * <p>La primera compra crea la fila del comprador, de modo que un escenario que no compra debe
   * observar su ausencia en lugar de exigir una cantidad. La ausencia y una tenencia en cero son
   * estados distintos y la aserción los distingue.
   */
  private Optional<PlayerToken> buyerHoldingOf(Player target) {
    return holdings.findByPlayerIdAndOwnerId(target.getId(), buyer.getId());
  }

  @Nested
  @DisplayName("Liquidación secundaria y reversión durable")
  class SecondarySettlement {
    private User secondarySeller;
    private footballmarket.models.TokenListing listing;

    @BeforeEach
    void prepareSecondaryListing() {
      secondarySeller = market.registerBuyer();
      buyOrderService.buy(secondarySeller.getEmail(), new BuyOrderRequest(player.getId(), 20));
      listing =
          listingService.publish(
              secondarySeller.getEmail(),
              new footballmarket.models.records.SellOrderRequest(player.getId(), 5));
    }

    private Map<String, List<Map<String, Object>>> snapshot() {
      return Map.of(
          "credits",
              jdbc()
                  .queryForList(
                      "select * from user_credits where user_id in (?, ?) order by user_id",
                      buyer.getId(),
                      secondarySeller.getId()),
          "tokens",
              jdbc()
                  .queryForList(
                      "select * from player_tokens where player_id = ? order by owner_user_id",
                      player.getId()),
          "audits",
              jdbc()
                  .queryForList(
                      "select * from market_transactions where player_id = ? order by id",
                      player.getId()),
          "listing",
              jdbc().queryForList("select * from token_listings where id = ?", listing.getId()));
    }

    @Test
    @DisplayName(
        "La compra secundaria conserva créditos y tokens y deja una única auditoría referenciada")
    void conservaRecursos() {
      // Arrange
      long buyerBefore = creditOf(buyer);
      long sellerBefore = creditOf(secondarySeller);
      // Act
      MarketTransaction result =
          buyOrderService.buyListing(
              buyer.getEmail(),
              new footballmarket.models.records.BuyListingRequest(listing.getId()));
      // Assert
      assertThat(result.getListing().getId()).isEqualTo(listing.getId());
      // Verify
      assertThat(creditOf(buyer)).isEqualTo(buyerBefore - 500);
      assertThat(creditOf(secondarySeller)).isEqualTo(sellerBefore + 500);
      assertThat(creditOf(buyer) + creditOf(secondarySeller)).isEqualTo(buyerBefore + sellerBefore);
      assertThat(holdingOf(player, buyer)).isEqualTo(5);
      assertThat(holdingOf(player, secondarySeller)).isEqualTo(15);
      assertThat(totalHeld(player)).isEqualTo(100);
      assertThat(listings.findById(listing.getId()).orElseThrow().getStatus().name())
          .isEqualTo("SOLD");
      assertThat(
              jdbc()
                  .queryForObject(
                      "select count(*) from market_transactions where listing_id = ?",
                      Long.class,
                      listing.getId()))
          .isEqualTo(1);
      assertThat(
              jdbc()
                  .queryForObject(
                      "select count(*) from market_transactions where player_id = ? and listing_id is null",
                      Long.class,
                      player.getId()))
          .isEqualTo(1);
    }

    @Test
    @DisplayName(
        "Un rechazo real de PostgreSQL al insertar auditoría revierte saldos, tenencias y SOLD")
    void revierteFalloDeAuditoria() {
      // Arrange
      Map<String, List<Map<String, Object>>> before = snapshot();
      String constraint = "reject_secondary_" + listing.getId();
      jdbc()
          .execute(
              "alter table market_transactions add constraint "
                  + constraint
                  + " check (listing_id is distinct from "
                  + listing.getId()
                  + ") not valid");
      try {
        // Act
        Throwable failure =
            org.assertj.core.api.Assertions.catchThrowable(
                () ->
                    buyOrderService.buyListing(
                        buyer.getEmail(),
                        new footballmarket.models.records.BuyListingRequest(listing.getId())));
        // Assert
        assertThat(failure).isNotNull();
        boolean checkViolation = false;
        for (Throwable cause = failure; cause != null; cause = cause.getCause()) {
          if (cause instanceof SQLException sql && "23514".equals(sql.getSQLState())) {
            checkViolation = true;
          }
        }
        assertThat(checkViolation).isTrue();
        // Verify
        assertThat(snapshot()).isEqualTo(before);
      } finally {
        jdbc().execute("alter table market_transactions drop constraint " + constraint);
      }
    }

    @Test
    @DisplayName(
        "Comprar un jugador desactivado responde como publicación inexistente y no altera estado durable")
    void ocultaJugadorNoVisible() {
      // Arrange
      jdbc().update("update players set active = false where id = ?", player.getId());
      Map<String, List<Map<String, Object>>> before = snapshot();
      // Act / Assert
      assertThatThrownBy(
              () ->
                  buyOrderService.buyListing(
                      buyer.getEmail(),
                      new footballmarket.models.records.BuyListingRequest(listing.getId())))
          .isInstanceOf(footballmarket.services.exceptions.ListingNotFoundException.class);
      // Verify
      assertThat(snapshot()).isEqualTo(before);
      assertThat(listingService.detail(secondarySeller.getEmail(), listing.getId()).listingId())
          .isEqualTo(listing.getId());
    }
  }

  private long totalHeld(Player target) {
    return holdings.findAll().stream()
        .filter(token -> token.getPlayer().getId().equals(target.getId()))
        .mapToLong(token -> token.getQuantity())
        .sum();
  }

  private List<MarketTransaction> auditOf(Player target) {
    return transactions.findByPlayerIdOrderByExecutedAtDescIdDesc(target.getId());
  }

  /**
   * Comprueba que una escritura es rechazada por la restricción CHECK del esquema.
   *
   * <p>Se afirma el SQLState 23514, que es <code>check_violation</code>, y no la clase de excepción
   * que traduzca Spring: la traducción de SQLException depende de la configuración del {@code
   * JdbcTemplate}, mientras que el estado devuelto por el servidor no. Así se prueba que fue la
   * restricción CHECK la que rechazó la escritura y no otra forma de violación de integridad.
   *
   * @param write escritura que se espera rechazada
   */
  private void assertThatCheckViolation(Runnable write) {
    org.springframework.dao.DataAccessException thrown =
        org.assertj.core.api.Assertions.catchThrowableOfType(
            write::run, org.springframework.dao.DataAccessException.class);

    assertThat(thrown).as("PostgreSQL debe rechazar la escritura").isNotNull();

    Throwable reported = thrown.getMostSpecificCause();
    assertThat(reported)
        .as("la causa más específica debe ser el error informado por el servidor")
        .isInstanceOf(java.sql.SQLException.class);

    assertThat(((java.sql.SQLException) reported).getSQLState())
        .as("23514 es check_violation: la restricción CHECK del esquema rechazó la escritura")
        .isEqualTo("23514");
  }

  /** Cuenta las filas de saldo de una cuenta, y no las del resto de la base. */
  private long countCreditsOf(User user) {
    Long count =
        jdbc()
            .queryForObject(
                "SELECT count(*) FROM user_credits WHERE user_id = ?", Long.class, user.getId());
    return count == null ? 0L : count;
  }

  /** Cuenta las emisiones de un jugador, y no las del resto de la base. */
  private long countEmissionsOf(Player target) {
    Long count =
        jdbc()
            .queryForObject(
                "SELECT count(*) FROM player_token_emissions WHERE player_id = ?",
                Long.class,
                target.getId());
    return count == null ? 0L : count;
  }

  /** Cuenta las tenencias de un jugador, y no las del resto de la base. */
  private long countHoldingsOf(Player target) {
    Long count =
        jdbc()
            .queryForObject(
                "SELECT count(*) FROM player_tokens WHERE player_id = ?",
                Long.class,
                target.getId());
    return count == null ? 0L : count;
  }

  @Nested
  @DisplayName("Alta de usuario y saldo inicial")
  class UserRegistrationCredits {

    @Test
    @DisplayName("El alta confirma a la vez el usuario persistido y su cuenta de créditos inicial")
    void elAltaConfirmaUsuarioYSaldoInicial() {
      // Arrange: el alta se provoca por el Service productivo, no por un fixture de
      // repositorios. El correo deriva del identificador del comprador de `arrange`, único en cada
      // ejecución, de modo que la prueba sea repetible sobre la misma base.
      String email = "ac18-" + buyer.getId() + "@example.com";

      // Act
      authenticationService.register(new User("ac18user", email, "password123"));

      // Assert: AC10 y FR-015 exigen 10000 centésimas, el valor de producción y no un literal, y
      // una sola cuenta por usuario. El estado se afirma sobre la base, como en el resto de la
      // categoría Repository.
      assertThat(
              jdbc()
                  .queryForObject("SELECT count(*) FROM users WHERE email = ?", Long.class, email))
          .isEqualTo(1L);
      assertThat(
              jdbc()
                  .queryForObject(
                      "SELECT count(*) FROM user_credits"
                          + " WHERE user_id = (SELECT id FROM users WHERE email = ?)",
                      Long.class,
                      email))
          .isEqualTo(1L);
      assertThat(
              jdbc()
                  .queryForObject(
                      "SELECT available_credits FROM user_credits"
                          + " WHERE user_id = (SELECT id FROM users WHERE email = ?)",
                      Long.class,
                      email))
          .isEqualTo(UserCredit.INITIAL_USER_CREDITS);
    }

    @Test
    @DisplayName("Si PostgreSQL rechaza la cuenta de créditos, el alta completa se revierte")
    void revierteElAltaCuandoPostgresRechazaLaCuentaDeCreditos() {
      // Arrange: el contexto de esta clase usa exclusivamente el PostgreSQL desechable de
      // Testcontainers. La restricción temporal simula un fallo real de la base, no un mock,
      // y se retira en el finally para no contaminar las pruebas siguientes. El identificador
      // proviene de una secuencia positiva, de modo que la restricción rechaza toda inserción real.
      // Se declara
      // NOT VALID porque la tabla ya contiene saldos válidos y sin esa cláusula el propio ALTER
      // fallaría contra las filas existentes; NOT VALID omite validar esas filas y sigue rechazando
      // toda inserción posterior, que es exactamente lo que la prueba necesita.
      long creditsRowsBefore =
          jdbc().queryForObject("SELECT count(*) FROM user_credits", Long.class);
      long usersRowsBefore = jdbc().queryForObject("SELECT count(*) FROM users", Long.class);
      String email = "ac18rollback-" + buyer.getId() + "@example.com";
      try {
        jdbc()
            .execute(
                "ALTER TABLE user_credits ADD CONSTRAINT test_ac18_reject_credit"
                    + " CHECK (user_id < 0) NOT VALID");

        // Act
        Throwable failure =
            org.assertj.core.api.Assertions.catchThrowable(
                () ->
                    authenticationService.register(new User("ac18rollback", email, "password123")));

        // Assert: AC18 exige que el rechazo provenga de la base y no de una regla de negocio, así
        // que se exige una SQLException de PostgreSQL con SQLSTATE de violación de CHECK. Una
        // validación de dominio —por ejemplo la longitud mínima de la contraseña— también fallaría
        // aquí, de modo que la cadena real se imprime en el mensaje para que un rechazo de negocio
        // sea distinguible de un rechazo de la base sin depender de una ejecución con depuración.
        assertThat(failure).as("el alta debe fallar cuando la base rechaza la cuenta").isNotNull();
        StringBuilder chain = new StringBuilder();
        List<String> sqlStates = new ArrayList<>();
        for (Throwable cause = failure; cause != null; cause = cause.getCause()) {
          chain
              .append(cause.getClass().getName())
              .append(": ")
              .append(cause.getMessage())
              .append('\n');
          if (cause instanceof java.sql.SQLException sqlFailure) {
            sqlStates.add(sqlFailure.getSQLState());
          }
        }
        assertThat(sqlStates)
            .as(
                "se esperaba una violación de CHECK (23514) de PostgreSQL; SQLSTATEs hallados: %s;"
                    + " cadena:%n%s",
                sqlStates, chain)
            .contains("23514");

        // Verify: consultas nuevas en autocommit, fuera de la transacción ya revertida. AC18 exige
        // que el alta no se confirme si el saldo no puede asignarse: ni el usuario ni la cuenta
        // sobreviven al rechazo.
        assertThat(
                jdbc()
                    .queryForObject(
                        "SELECT count(*) FROM users WHERE email = ?", Long.class, email))
            .isZero();
        assertThat(jdbc().queryForObject("SELECT count(*) FROM users", Long.class))
            .isEqualTo(usersRowsBefore);
        assertThat(jdbc().queryForObject("SELECT count(*) FROM user_credits", Long.class))
            .isEqualTo(creditsRowsBefore);
      } finally {
        jdbc()
            .execute("ALTER TABLE user_credits DROP CONSTRAINT IF EXISTS test_ac18_reject_credit");
      }

      // La restricción debe quedar ausente: una fuga alteraría el esquema de las pruebas
      // siguientes.
      assertThat(
              jdbc()
                  .queryForObject(
                      "SELECT count(*) FROM pg_constraint WHERE conrelid = 'user_credits'::regclass"
                          + " AND conname = 'test_ac18_reject_credit'",
                      Long.class))
          .isZero();
    }
  }

  @Nested
  @DisplayName("Restricciones de integridad")
  class Constraints {
    @Test
    @DisplayName("Un saldo de créditos negativo es rechazado por la restricción de PostgreSQL")
    void rechazaSaldoNegativo() {
      // Arrange
      long creditsBefore = creditOf(buyer);

      // Act / Assert: la escritura va por JDBC porque el modelo rechaza el saldo negativo antes de
      // que exista una sentencia que enviar. Se alcanza así la restricción CHECK del esquema, que
      // es la que debe impedir que un saldo negativo llegue a persistirse por cualquier vía.
      assertThatCheckViolation(
          () ->
              jdbc()
                  .update(
                      "UPDATE user_credits SET available_credits = -100 WHERE user_id = ?",
                      buyer.getId()));

      // La escritura rechazada no alteró el saldo persistido.
      assertThat(creditOf(buyer)).isEqualTo(creditsBefore);
    }

    @Test
    @DisplayName("El modelo rechaza un saldo de créditos negativo antes de persistirlo")
    void elModeloRechazaUnSaldoNegativo() {
      // Act / Assert: invariante del dominio, independiente de la restricción del esquema.
      assertThatThrownBy(() -> UserCredit.restore(buyer.getId(), -100L))
          .isInstanceOf(footballmarket.models.exceptions.InvalidCreditAmountException.class);
    }

    @Test
    @DisplayName("Una emisión con una cantidad distinta de la fijada es rechazada")
    void rechazaUnaEmisionConCantidadDistinta() {
      // Assert
      assertThatThrownBy(
              () ->
                  emissions.saveAndFlush(
                      PlayerTokenEmission.restore(
                          player.getId(), superuser, 42, OffsetDateTime.now(ZoneOffset.UTC))))
          .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("Una tenencia duplicada para el mismo jugador y propietario es rechazada")
    void rechazaUnaTenenciaDuplicada() {
      // Arrange
      buy(player, QUANTITY);

      // Assert
      assertThatThrownBy(
              () ->
                  holdings.saveAndFlush(
                      footballmarket.models.PlayerToken.restore(
                          null, player, buyer, 5, OffsetDateTime.now(ZoneOffset.UTC))))
          .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("Una cantidad de tokens negativa es rechazada por la restricción de PostgreSQL")
    void rechazaCantidadNegativa() {
      // Arrange
      long heldBefore = totalHeld(player);

      // Act / Assert: por JDBC, porque el modelo rechaza la cantidad negativa en su constructor.
      assertThatCheckViolation(
          () ->
              jdbc()
                  .update(
                      "INSERT INTO player_tokens (player_id, owner_user_id, quantity, updated_at)"
                          + " VALUES (?, ?, -3, now())",
                      player.getId(),
                      buyer.getId()));

      // La escritura rechazada no creó ninguna tenencia.
      assertThat(totalHeld(player)).isEqualTo(heldBefore);
    }

    @Test
    @DisplayName("El modelo rechaza una cantidad de tokens negativa antes de persistirla")
    void elModeloRechazaUnaCantidadNegativa() {
      // Act / Assert: invariante del dominio, independiente de la restricción del esquema.
      assertThatThrownBy(
              () ->
                  footballmarket.models.PlayerToken.restore(
                      null, player, buyer, -3, OffsetDateTime.now(ZoneOffset.UTC)))
          .isInstanceOf(footballmarket.models.exceptions.InvalidTokenQuantityException.class);
    }
  }

  @Nested
  @DisplayName("Rechazos y reversión")
  class RejectionsAndRollback {
    @Test
    @DisplayName("Una compra rechazada por stock no modifica créditos, tenencias ni auditoría")
    void unaCompraRechazadaNoDejaRastro() {
      // Arrange
      buy(player, EMITTED);

      // AC03 exige «créditos suficientes y stock inferior a lo solicitado». Comprar la emisión
      // completa agota el saldo inicial, así que se acredita lo mínimo para que el rechazo sea por
      // inventario. El escenario contiguo `unaCompraSinCreditosNoDejaRastro` cubre el contrario,
      // con
      // el saldo agotado y sin fondeo, y por eso ambos no pueden coexistir sin esta acreditación.
      market.fund(buyer, PRICE);

      long creditsBefore = creditOf(buyer);
      int buyerTokensBefore = holdingOf(player, buyer);
      int sellerTokensBefore = holdingOf(player, superuser);
      int auditBefore = auditOf(player).size();

      // Act
      assertThatThrownBy(() -> buy(player, 1))
          .isInstanceOf(footballmarket.services.exceptions.InsufficientStockException.class);

      // Assert
      assertThat(creditOf(buyer)).isEqualTo(creditsBefore);
      assertThat(holdingOf(player, buyer)).isEqualTo(buyerTokensBefore);
      assertThat(holdingOf(player, superuser)).isEqualTo(sellerTokensBefore);
      assertThat(auditOf(player)).hasSize(auditBefore);
    }

    @Test
    @DisplayName("Una compra rechazada por créditos no registra auditoría ni mueve tokens")
    void unaCompraSinCreditosNoDejaRastro() {
      // Arrange
      buy(player, EMITTED);
      long sellerTokensBefore = holdingOf(player, superuser);
      int auditBefore = auditOf(player).size();

      // Act
      assertThatThrownBy(() -> buy(player, 1))
          .isInstanceOf(footballmarket.services.exceptions.InsufficientCreditsException.class);

      // Assert
      assertThat(holdingOf(player, superuser)).isEqualTo(sellerTokensBefore);
      assertThat(auditOf(player)).hasSize(auditBefore);
    }

    @Test
    @DisplayName("Una compra sin vendedor identificable no deja movimientos ni auditoría")
    void unaCompraSinVendedorNoDejaRastro() {
      // Arrange
      long creditsBefore = creditOf(buyer);
      int auditBefore = auditOf(player).size();

      // Act
      removeSuperuser();
      assertThatThrownBy(() -> buy(player, QUANTITY))
          .isInstanceOf(footballmarket.services.exceptions.SellerUnavailableException.class);

      // Assert
      assertThat(creditOf(buyer)).isEqualTo(creditsBefore);
      assertThat(auditOf(player)).hasSize(auditBefore);
      assertThat(totalHeld(player)).isEqualTo(EMITTED);
    }

    @ParameterizedTest(name = "cantidad = {0}")
    @ValueSource(ints = {0, 101})
    @DisplayName(
        "Una compra con cantidad fuera de rango no modifica créditos, tenencias ni auditoría")
    void unaCompraConCantidadInvalidaNoDejaRastro(int quantity) {
      // Arrange: la cantidad se valida antes de adquirir cualquier bloqueo, de modo que el rechazo
      // no puede alcanzar los recursos. La prueba fija ese orden. El comprador todavía no tiene
      // tenencia de este jugador porque la primera compra la crea el Service, así que su estado
      // previo se observa como Option y no como cantidad.
      long creditsBefore = creditOf(buyer);
      Optional<PlayerToken> buyerTokensBefore = buyerHoldingOf(player);
      int sellerTokensBefore = holdingOf(player, superuser);
      int auditBefore = auditOf(player).size();

      // Act
      assertThatThrownBy(() -> buy(player, quantity))
          .isInstanceOf(footballmarket.services.exceptions.InvalidQuantityException.class);

      // Assert
      assertThat(creditOf(buyer)).isEqualTo(creditsBefore);
      assertThat(buyerHoldingOf(player)).isEqualTo(buyerTokensBefore);
      assertThat(holdingOf(player, superuser)).isEqualTo(sellerTokensBefore);
      assertThat(totalHeld(player)).isEqualTo(EMITTED);
      assertThat(auditOf(player)).hasSize(auditBefore);
    }

    @Test
    @DisplayName("Una compra de un jugador no visible no deja movimientos ni crea su tenencia")
    void unaCompraDeJugadorNoVisibleNoDejaRastro() {
      // Arrange: AC12 exige 404 PLAYER_NOT_VISIBLE para un jugador existente pero no elegible.
      Player notVisible = market.createNotVisiblePlayer();
      long creditsBefore = creditOf(buyer);
      Optional<PlayerToken> buyerTokensBefore = buyerHoldingOf(player);
      int sellerTokensBefore = holdingOf(player, superuser);
      int auditBefore = auditOf(player).size();

      // Act
      assertThatThrownBy(() -> buy(notVisible, QUANTITY))
          .isInstanceOf(footballmarket.services.exceptions.PlayerNotVisibleException.class);

      // Assert: el jugador rechazado nunca estuvo en el mercado y sigue sin estarlo.
      assertThat(totalHeld(notVisible)).isZero();
      assertThat(auditOf(notVisible)).isEmpty();
      assertThat(holdings.findByPlayerIdAndOwnerId(notVisible.getId(), buyer.getId())).isEmpty();

      // Assert: el jugador válido tampoco se movió.
      assertThat(creditOf(buyer)).isEqualTo(creditsBefore);
      assertThat(buyerHoldingOf(player)).isEqualTo(buyerTokensBefore);
      assertThat(holdingOf(player, superuser)).isEqualTo(sellerTokensBefore);
      assertThat(totalHeld(player)).isEqualTo(EMITTED);
      assertThat(auditOf(player)).hasSize(auditBefore);
    }

    @Test
    @DisplayName("Una compra sin cotización vigente no modifica créditos, tenencias ni auditoría")
    void unaCompraSinCotizacionNoDejaRastro() {
      // Arrange: la fuente no devuelve cotización, que AC06 traduce a 409 QUOTE_UNAVAILABLE.
      givenNoQuote();
      long creditsBefore = creditOf(buyer);
      Optional<PlayerToken> buyerTokensBefore = buyerHoldingOf(player);
      int sellerTokensBefore = holdingOf(player, superuser);
      int auditBefore = auditOf(player).size();

      // Act
      assertThatThrownBy(() -> buy(player, QUANTITY))
          .isInstanceOf(footballmarket.services.exceptions.QuoteUnavailableException.class);

      // Assert
      assertThat(creditOf(buyer)).isEqualTo(creditsBefore);
      assertThat(buyerHoldingOf(player)).isEqualTo(buyerTokensBefore);
      assertThat(holdingOf(player, superuser)).isEqualTo(sellerTokensBefore);
      assertThat(totalHeld(player)).isEqualTo(EMITTED);
      assertThat(auditOf(player)).hasSize(auditBefore);
    }

    @Test
    @DisplayName("Una compra con precio de cotización inválido no modifica nada")
    void unaCompraConPrecioDeCotizacionInvalidoNoDejaRastro() {
      // Arrange: AC15 exige 409 INVALID_QUOTE_PRICE ante precio cero o negativo, sin movimientos.
      givenQuote(0L);
      long creditsBefore = creditOf(buyer);
      Optional<PlayerToken> buyerTokensBefore = buyerHoldingOf(player);
      int sellerTokensBefore = holdingOf(player, superuser);
      int auditBefore = auditOf(player).size();

      // Act
      assertThatThrownBy(() -> buy(player, QUANTITY))
          .isInstanceOf(footballmarket.services.exceptions.InvalidQuotePriceException.class);

      // Assert
      assertThat(creditOf(buyer)).isEqualTo(creditsBefore);
      assertThat(buyerHoldingOf(player)).isEqualTo(buyerTokensBefore);
      assertThat(holdingOf(player, superuser)).isEqualTo(sellerTokensBefore);
      assertThat(totalHeld(player)).isEqualTo(EMITTED);
      assertThat(auditOf(player)).hasSize(auditBefore);
    }

    /**
     * Deja al superusuario sin el rol que lo identifica como vendedor.
     *
     * <p>Se retira el rol y no la fila: la emisión inicial referencia al propietario sin cascada,
     * de modo que borrar la identidad dejaría referencias colgantes que nada de esta SPEC resuelve.
     */
    private void removeSuperuser() {
      new TransactionTemplate(transactionManager)
          .executeWithoutResult(
              status ->
                  jdbc().update("UPDATE users SET role = 'USER' WHERE id = ?", superuser.getId()));
    }
  }

  @Nested
  @DisplayName("Inicialización idempotente")
  class IdempotentInitialization {
    @Test
    @DisplayName("Reejecutar el backfill de créditos no restaura el saldo consumido")
    void reejecutarCreditosNoRestauraSaldo() {
      // Arrange
      buy(player, QUANTITY);
      long spent = creditOf(buyer);
      long rowsBefore = countCreditsOf(buyer);

      // Act: se reejecuta la sentencia de backfill de saldos de V9, no la migración versionada.
      rerunCreditBackfill(buyer.getId());

      // Assert: se compara contra el saldo realmente consumido y contra el número de filas de esa
      // misma cuenta, de modo que la afirmación probar la idempotencia y no la mera ausencia de
      // excepciones. Un `ON CONFLICT DO NOTHING` ausente devolvería el saldo inicial.
      assertThat(spent).isLessThan(UserCredit.INITIAL_USER_CREDITS);
      assertThat(creditOf(buyer)).isEqualTo(spent);
      assertThat(countCreditsOf(buyer)).isEqualTo(1L);
      assertThat(countCreditsOf(buyer)).isEqualTo(rowsBefore);
    }

    @Test
    @DisplayName("Reejecutar el backfill de emisiones no repone el inventario vendido")
    void reejecutarElBackfillDeEmisionesNoReponeInventario() {
      // Arrange
      buy(player, QUANTITY);
      int sellerAfterSale = holdingOf(player, superuser);
      long heldAfterSale = totalHeld(player);
      long emissionsBefore = countEmissionsOf(player);
      long rowsBefore = countHoldingsOf(player);

      // Act: se reejecutan las dos sentencias de backfill de V9 para emisiones y tenencias, sin
      // reejecutar la migración versionada mediante Flyway.
      rerunEmissionBackfill(player.getId());
      rerunHoldingBackfill(player.getId());

      // Assert
      assertThat(countEmissionsOf(player)).isEqualTo(emissionsBefore);
      assertThat(countHoldingsOf(player)).isEqualTo(rowsBefore);
      assertThat(holdingOf(player, superuser)).isEqualTo(sellerAfterSale);
      assertThat(totalHeld(player)).isEqualTo(heldAfterSale);
      assertThat(totalHeld(player)).isEqualTo(EMITTED);
    }

    /** Reejecuta el backfill de saldos de V9 acotado a una cuenta. */
    private void rerunCreditBackfill(Long userId) {
      jdbc()
          .update(
              "INSERT INTO user_credits (user_id, available_credits)"
                  + " SELECT u.id, CASE WHEN u.role = 'SUPERUSER' THEN 0 ELSE 10000 END"
                  + " FROM users u WHERE u.id = ?"
                  + " ON CONFLICT (user_id) DO NOTHING",
              userId);
    }

    /** Reejecuta el backfill de emisiones de V9 acotado a un jugador. */
    private void rerunEmissionBackfill(Long playerId) {
      jdbc()
          .update(
              "INSERT INTO player_token_emissions (player_id, owner_user_id, initial_quantity)"
                  + " SELECT p.id, su.id, 100 FROM players p"
                  + " CROSS JOIN (SELECT id FROM users WHERE role = 'SUPERUSER'"
                  + " ORDER BY id LIMIT 1) su"
                  + " WHERE p.id = ?"
                  + " ON CONFLICT (player_id) DO NOTHING",
              playerId);
    }

    /** Reejecuta el backfill de tenencias de V9 acotado a un jugador. */
    private void rerunHoldingBackfill(Long playerId) {
      jdbc()
          .update(
              "INSERT INTO player_tokens (player_id, owner_user_id, quantity)"
                  + " SELECT e.player_id, e.owner_user_id, e.initial_quantity"
                  + " FROM player_token_emissions e WHERE e.player_id = ?"
                  + " ON CONFLICT (player_id, owner_user_id) DO NOTHING",
              playerId);
    }
  }

  @Nested
  @DisplayName("Rollback durable ante rechazo PostgreSQL de la auditoría")
  class AuditPersistenceRollback {
    @ParameterizedTest(name = "Tenencia previa del comprador: {0}")
    @ValueSource(booleans = {false, true})
    @DisplayName("El rechazo de auditoría revierte la compra y permite una compra posterior válida")
    void revierteLaCompraCuandoPostgresRechazaLaAuditoria(boolean existingHolding) {
      // Arrange: sin transacción de test. La compra preparatoria, si corresponde, se confirma
      // mediante el mismo proxy productivo antes de capturar el estado durable.
      if (existingHolding) {
        buy(player, 1);
      }
      int buyerTokensBefore = existingHolding ? 1 : 0;
      int sellerTokensBefore = EMITTED - buyerTokensBefore;
      List<Map<String, Object>> accountsBefore = creditRows();
      List<Map<String, Object>> holdingsBefore = holdingRows();
      List<Map<String, Object>> auditsBefore = auditRows();
      long buyerCreditsBefore =
          jdbc()
              .queryForObject(
                  "SELECT available_credits FROM user_credits WHERE user_id = ?",
                  Long.class,
                  buyer.getId());
      long sellerCreditsBefore =
          jdbc()
              .queryForObject(
                  "SELECT available_credits FROM user_credits WHERE user_id = ?",
                  Long.class,
                  superuser.getId());
      assertThat(buyerCreditsBefore).isGreaterThanOrEqualTo(PRICE);
      assertThat(holdingsBefore).hasSize(existingHolding ? 2 : 1);
      assertThat(auditsBefore).hasSize(existingHolding ? 1 : 0);

      OffsetDateTime calculatedAt = OffsetDateTime.parse("2026-10-14T10:30:00Z");
      CurrentQuote rejectedQuote =
          new CurrentQuote(player.getId(), PRICE, calculatedAt, "x".repeat(65));
      org.mockito.Mockito.when(quotes.currentQuote(player.getId()))
          .thenReturn(Optional.of(rejectedQuote));

      // Act: al salir la excepción del proxy, terminó la transacción de compra, incluido rollback.
      Throwable failure = org.assertj.core.api.Assertions.catchThrowable(() -> buy(player, 1));

      // Assert: no basta una excepción Hibernate o de validación. Se exige el error JDBC concreto
      // de PostgreSQL por longitud de VARCHAR(64), sin depender del texto del mensaje.
      assertThat(failure).isNotNull();
      boolean postgresLengthFailure = false;
      for (Throwable cause = failure; cause != null; cause = cause.getCause()) {
        if (cause instanceof SQLException sqlFailure && "22001".equals(sqlFailure.getSQLState())) {
          postgresLengthFailure = true;
          break;
        }
      }
      assertThat(postgresLengthFailure)
          .as("PostgreSQL debe rechazar el INSERT de auditoría con SQLSTATE 22001")
          .isTrue();

      // Verify: nuevas lecturas JDBC fuera de la transacción fallida. Las filas completas incluyen
      // saldos, cantidades y timestamps; también se conservan las auditorías anteriores.
      assertThat(creditRows()).isEqualTo(accountsBefore);
      assertThat(holdingRows()).isEqualTo(holdingsBefore);
      assertThat(auditRows()).isEqualTo(auditsBefore);
      if (!existingHolding) {
        assertThat(
                jdbc()
                    .queryForObject(
                        "SELECT count(*) FROM player_tokens"
                            + " WHERE player_id = ? AND owner_user_id = ?",
                        Long.class,
                        player.getId(),
                        buyer.getId()))
            .isZero();
      }

      // Arrange del control positivo: solo cambia la longitud de la versión. Las comprobaciones
      // de rollback anteriores ya concluyeron y no dependen del éxito de esta segunda operación.
      String acceptedVersion = "x".repeat(64);
      CurrentQuote acceptedQuote =
          new CurrentQuote(player.getId(), PRICE, calculatedAt, acceptedVersion);
      org.mockito.Mockito.when(quotes.currentQuote(player.getId()))
          .thenReturn(Optional.of(acceptedQuote));

      // Act: el retorno del proxy confirma esta nueva transacción independiente.
      MarketTransaction confirmed =
          buyOrderService.buy(buyer.getEmail(), new BuyOrderRequest(player.getId(), 1));

      // Verify: movimientos y auditoría confirmados en PostgreSQL, no solo en el Model retornado.
      assertThat(
              jdbc()
                  .queryForObject(
                      "SELECT available_credits FROM user_credits WHERE user_id = ?",
                      Long.class,
                      buyer.getId()))
          .isEqualTo(buyerCreditsBefore - PRICE);
      assertThat(
              jdbc()
                  .queryForObject(
                      "SELECT available_credits FROM user_credits WHERE user_id = ?",
                      Long.class,
                      superuser.getId()))
          .isEqualTo(sellerCreditsBefore + PRICE);
      assertThat(
              jdbc()
                  .queryForObject(
                      "SELECT quantity FROM player_tokens"
                          + " WHERE player_id = ? AND owner_user_id = ?",
                      Integer.class,
                      player.getId(),
                      buyer.getId()))
          .isEqualTo(buyerTokensBefore + 1);
      assertThat(
              jdbc()
                  .queryForObject(
                      "SELECT quantity FROM player_tokens"
                          + " WHERE player_id = ? AND owner_user_id = ?",
                      Integer.class,
                      player.getId(),
                      superuser.getId()))
          .isEqualTo(sellerTokensBefore - 1);
      List<Map<String, Object>> auditsAfter = auditRows();
      assertThat(auditsAfter).hasSize(auditsBefore.size() + 1);
      assertThat(auditsAfter).containsAll(auditsBefore);
      Map<String, Object> recorded =
          jdbc().queryForMap("SELECT * FROM market_transactions WHERE id = ?", confirmed.getId());
      assertThat(recorded)
          .containsEntry("buyer_id", buyer.getId())
          .containsEntry("seller_id", superuser.getId())
          .containsEntry("player_id", player.getId())
          .containsEntry("quantity", 1)
          .containsEntry("unit_price", PRICE)
          .containsEntry("total_amount", PRICE)
          .containsEntry("buyer_credits_before", buyerCreditsBefore)
          .containsEntry("buyer_credits_after", buyerCreditsBefore - PRICE)
          .containsEntry("seller_credits_before", sellerCreditsBefore)
          .containsEntry("seller_credits_after", sellerCreditsBefore + PRICE)
          .containsEntry("buyer_tokens_before", buyerTokensBefore)
          .containsEntry("buyer_tokens_after", buyerTokensBefore + 1)
          .containsEntry("seller_tokens_before", sellerTokensBefore)
          .containsEntry("seller_tokens_after", sellerTokensBefore - 1)
          .containsEntry("quote_strategy_version", acceptedVersion);
      assertThat(recorded.get("quote_calculated_at")).isNotNull();
      assertThat(recorded.get("executed_at")).isNotNull();
    }

    private List<Map<String, Object>> creditRows() {
      return jdbc()
          .queryForList(
              "SELECT * FROM user_credits WHERE user_id IN (?, ?) ORDER BY user_id",
              buyer.getId(),
              superuser.getId());
    }

    private List<Map<String, Object>> holdingRows() {
      return jdbc()
          .queryForList(
              "SELECT * FROM player_tokens WHERE player_id = ? ORDER BY id", player.getId());
    }

    private List<Map<String, Object>> auditRows() {
      return jdbc()
          .queryForList(
              "SELECT * FROM market_transactions WHERE player_id = ? ORDER BY id", player.getId());
    }
  }

  @Nested
  @DisplayName("Límite del saldo receptor y rollback durable")
  class SellerCreditOverflow {
    @Test
    @DisplayName("El overflow del vendedor revierte cuentas, primera tenencia y auditoría")
    void revierteLaCompraCuandoDesbordaElVendedor() {
      // Arrange: no hay transacción de test. El aprovisionamiento y las lecturas JDBC se confirman
      // independientemente; la llamada al proxy del Service delimita la transacción bajo prueba.
      Map<String, Object> originalSeller = accountRow(superuser);
      try {
        jdbc()
            .update(
                "UPDATE user_credits SET available_credits = ? WHERE user_id = ?",
                Long.MAX_VALUE - 50L,
                superuser.getId());
        Map<String, Object> buyerBefore = accountRow(buyer);
        Map<String, Object> sellerBefore = accountRow(superuser);
        List<Map<String, Object>> tokensBefore = tokenRows();
        List<Map<String, Object>> auditBefore = auditRows();
        assertThat(countHoldingsOf(player)).isEqualTo(1L);
        assertThat(holdings.findByPlayerIdAndOwnerId(player.getId(), buyer.getId())).isEmpty();
        assertThat(creditOf(buyer)).isGreaterThanOrEqualTo(PRICE);
        assertThat(holdingOf(player, superuser)).isEqualTo(EMITTED);

        // Act / Assert: al retornar la excepción al llamador, el interceptor transaccional ya
        // concluyó el rollback. No se captura la excepción dentro de una transacción envolvente.
        assertThatThrownBy(() -> buy(player, 1))
            .isInstanceOf(footballmarket.services.exceptions.ArithmeticOverflowException.class);

        // Verify: nuevas consultas JDBC, sin entidades del EntityManager de la compra.
        // Las filas completas incluyen available_credits y updated_at de ambas cuentas.
        assertThat(accountRow(buyer)).isEqualTo(buyerBefore);
        assertThat(accountRow(superuser)).isEqualTo(sellerBefore);
        assertThat(tokenRows()).isEqualTo(tokensBefore);
        assertThat(countHoldingsOf(player)).isEqualTo(1L);
        assertThat(
                jdbc()
                    .queryForObject(
                        "SELECT count(*) FROM player_tokens WHERE player_id = ?"
                            + " AND owner_user_id = ?",
                        Long.class,
                        player.getId(),
                        buyer.getId()))
            .isZero();
        assertThat(auditRows()).isEqualTo(auditBefore);
      } finally {
        restoreSeller(originalSeller);
      }
    }

    @Test
    @DisplayName(
        "La compra confirma cuando el saldo del vendedor alcanza exactamente Long.MAX_VALUE")
    void confirmaLaCompraEnElLimiteDelVendedor() {
      // Arrange
      Map<String, Object> originalSeller = accountRow(superuser);
      try {
        jdbc()
            .update(
                "UPDATE user_credits SET available_credits = ? WHERE user_id = ?",
                Long.MAX_VALUE - PRICE,
                superuser.getId());
        long buyerBefore = creditOf(buyer);
        assertThat(auditRows()).isEmpty();
        assertThat(holdings.findByPlayerIdAndOwnerId(player.getId(), buyer.getId())).isEmpty();

        // Act: el proxy confirma la transacción antes de devolver el resultado.
        MarketTransaction result =
            buyOrderService.buy(buyer.getEmail(), new BuyOrderRequest(player.getId(), 1));

        // Verify: los importes, las tenencias y la auditoría se leen de PostgreSQL por JDBC.
        assertThat(accountRow(buyer).get("available_credits")).isEqualTo(buyerBefore - PRICE);
        assertThat(accountRow(superuser).get("available_credits")).isEqualTo(Long.MAX_VALUE);
        assertThat(
                jdbc()
                    .queryForObject(
                        "SELECT quantity FROM player_tokens"
                            + " WHERE player_id = ? AND owner_user_id = ?",
                        Integer.class,
                        player.getId(),
                        buyer.getId()))
            .isEqualTo(1);
        assertThat(
                jdbc()
                    .queryForObject(
                        "SELECT quantity FROM player_tokens"
                            + " WHERE player_id = ? AND owner_user_id = ?",
                        Integer.class,
                        player.getId(),
                        superuser.getId()))
            .isEqualTo(EMITTED - 1);
        List<Map<String, Object>> recorded = auditRows();
        assertThat(recorded).hasSize(1);
        Map<String, Object> audit = recorded.get(0);
        assertThat(audit)
            .containsEntry("id", result.getId())
            .containsEntry("buyer_id", buyer.getId())
            .containsEntry("seller_id", superuser.getId())
            .containsEntry("player_id", player.getId())
            .containsEntry("quantity", 1)
            .containsEntry("unit_price", PRICE)
            .containsEntry("total_amount", PRICE)
            .containsEntry("buyer_credits_before", buyerBefore)
            .containsEntry("buyer_credits_after", buyerBefore - PRICE)
            .containsEntry("seller_credits_before", Long.MAX_VALUE - PRICE)
            .containsEntry("seller_credits_after", Long.MAX_VALUE)
            .containsEntry("buyer_tokens_before", 0)
            .containsEntry("buyer_tokens_after", 1)
            .containsEntry("seller_tokens_before", EMITTED)
            .containsEntry("seller_tokens_after", EMITTED - 1);
        assertThat(audit.get("quote_calculated_at")).isNotNull();
        assertThat(audit.get("executed_at")).isNotNull();
        assertThat(audit.get("quote_strategy_version")).isEqualTo("strategy-v1");
      } finally {
        restoreSeller(originalSeller);
      }
    }

    private Map<String, Object> accountRow(User user) {
      return jdbc().queryForMap("SELECT * FROM user_credits WHERE user_id = ?", user.getId());
    }

    private List<Map<String, Object>> tokenRows() {
      return jdbc()
          .queryForList(
              "SELECT * FROM player_tokens WHERE player_id = ? ORDER BY id", player.getId());
    }

    private List<Map<String, Object>> auditRows() {
      return jdbc()
          .queryForList(
              "SELECT * FROM market_transactions WHERE player_id = ? ORDER BY id", player.getId());
    }

    /** Restaura la cuenta compartida incluso si falla una aserción, sin ocultar el resultado. */
    private void restoreSeller(Map<String, Object> original) {
      jdbc()
          .update(
              "UPDATE user_credits SET available_credits = ?, updated_at = ? WHERE user_id = ?",
              original.get("available_credits"),
              original.get("updated_at"),
              superuser.getId());
    }
  }

  @Nested
  @DisplayName("Auditoría persistida de la cotización")
  class PersistedQuote {
    @Test
    @DisplayName("El precio y el instante aplicados quedan registrados en la auditoría")
    void registraElSnapshotAplicado() {
      // Arrange
      OffsetDateTime calculatedAt = OffsetDateTime.parse("2026-10-14T10:30:00Z");
      org.mockito.Mockito.when(quotes.currentQuote(org.mockito.ArgumentMatchers.any()))
          .thenAnswer(
              invocation ->
                  Optional.of(
                      new CurrentQuote(
                          invocation.getArgument(0), 250L, calculatedAt, "estrategia-real")));

      // Act
      buy(player, QUANTITY);

      // Assert
      MarketTransaction recorded = auditOf(player).get(0);
      assertThat(recorded.getUnitPrice()).isEqualTo(250L);
      assertThat(recorded.getTotalAmount()).isEqualTo(2_500L);
      assertThat(recorded.getQuoteCalculatedAt()).isEqualTo(calculatedAt);
      assertThat(recorded.getQuoteStrategyVersion()).isEqualTo("estrategia-real");
    }

    @Test
    @DisplayName("La estrategia ausente de la fuente se conserva en null")
    void conservaEstrategiaAusenteEnNull() {
      // Arrange
      org.mockito.Mockito.when(quotes.currentQuote(org.mockito.ArgumentMatchers.any()))
          .thenAnswer(
              invocation ->
                  Optional.of(
                      new CurrentQuote(
                          invocation.getArgument(0),
                          PRICE,
                          OffsetDateTime.now(ZoneOffset.UTC),
                          null)));

      // Act
      buy(player, QUANTITY);

      // Assert
      assertThat(auditOf(player).get(0).getQuoteStrategyVersion()).isNull();
    }
  }

  @Nested
  @DisplayName("Concurrencia con PostgreSQL real")
  class Concurrency {
    @Test
    @DisplayName(
        "Las compras simultáneas del último token conservan el total y no generan sobreventa")
    void comprasSimultaneasDelUltimoToken() throws Exception {
      // Arrange
      buy(player, EMITTED - 1);
      User first = market.registerBuyer();
      User second = market.registerBuyer();
      List<MarketTransaction> auditBefore = auditOf(player);
      long creditsBefore = totalCreditsOf(first, second, superuser);
      long sellerCreditsBefore = creditOf(superuser);

      // Act: AC08 exige un único token disponible y una unidad por solicitante, de modo que la
      // carrera se dispara pidiendo un token y no el lote habitual.
      List<Outcome> outcomes = race(first, player, 1, second, player, 1);

      // Assert: una sola confirmación y un solo rechazo tipado.
      assertThat(outcomes).containsExactlyInAnyOrder(Outcome.REJECTED_STOCK, Outcome.CONFIRMED);

      // SC-008: ni el total emitido ni la suma de créditos de las partes cambian por la carrera.
      assertThat(totalHeld(player)).isEqualTo(EMITTED);
      assertThat(totalCreditsOf(first, second, superuser)).isEqualTo(creditsBefore);
      assertThat(creditOf(superuser)).isEqualTo(sellerCreditsBefore + PRICE);

      // SC-010: la operación confirmada deja exactamente un registro y la rechazada ninguno.
      MarketTransaction confirmed = onlyNewAudit(auditBefore, auditOf(player));
      User winner = confirmed.getBuyer();
      User loser = winner.getId().equals(first.getId()) ? second : first;
      assertThat(winner.getId()).isIn(first.getId(), second.getId());
      assertThat(confirmed.getSeller().getId()).isEqualTo(superuser.getId());
      assertThat(confirmed.getPlayer().getId()).isEqualTo(player.getId());
      assertThat(confirmed.getQuantity()).isEqualTo(1);
      assertThat(confirmed.getUnitPrice()).isEqualTo(PRICE);
      assertThat(confirmed.getTotalAmount()).isEqualTo(PRICE);

      // FR-013 y FR-014: la transferencia mueve exactamente el importe y conserva los estados
      // anterior y posterior de ambas partes.
      assertThat(confirmed.getBuyerCreditsBefore() - confirmed.getBuyerCreditsAfter())
          .isEqualTo(confirmed.getTotalAmount());
      assertThat(confirmed.getSellerCreditsAfter() - confirmed.getSellerCreditsBefore())
          .isEqualTo(confirmed.getTotalAmount());
      assertThat(confirmed.getBuyerTokensBefore()).isZero();
      assertThat(confirmed.getBuyerTokensAfter()).isEqualTo(1);
      assertThat(confirmed.getSellerTokensBefore()).isEqualTo(1);
      assertThat(confirmed.getSellerTokensAfter()).isZero();

      // Solo la operación ganadora movió recursos: el perdedor conserva su saldo inicial y no
      // obtiene tenencia alguna de este jugador.
      assertThat(holdingOf(player, winner)).isEqualTo(1);
      assertThat(holdingOf(player, superuser)).isZero();
      assertThat(holdings.findByPlayerIdAndOwnerId(player.getId(), loser.getId())).isEmpty();
      assertThat(creditOf(winner)).isEqualTo(UserCredit.INITIAL_USER_CREDITS - PRICE);
      assertThat(creditOf(loser)).isEqualTo(UserCredit.INITIAL_USER_CREDITS);
    }

    @Test
    @DisplayName(
        "Las compras simultáneas del mismo saldo sobre un jugador no permiten el doble gasto")
    void comprasSimultaneasSinDobleGasto() throws Exception {
      // Arrange: la cuenta tiene saldo justo para una sola compra.
      credits.saveAndFlush(UserCredit.restore(buyer.getId(), PRICE * QUANTITY));
      List<MarketTransaction> auditBefore = auditOf(player);
      long sellerCreditsBefore = creditOf(superuser);

      // Act
      List<Outcome> outcomes = race(buyer, player, QUANTITY, buyer, player, QUANTITY);

      // Assert
      assertThat(outcomes).containsExactlyInAnyOrder(Outcome.REJECTED_CREDITS, Outcome.CONFIRMED);
      assertThat(totalHeld(player)).isEqualTo(EMITTED);
      assertThat(creditOf(buyer)).isZero();
      assertThat(creditOf(superuser)).isEqualTo(sellerCreditsBefore + PRICE * QUANTITY);
      assertThat(holdingOf(player, buyer)).isEqualTo(QUANTITY);
      assertThat(holdingOf(player, superuser)).isEqualTo(EMITTED - QUANTITY);

      // SC-010: un único registro, del comprador que efectivamente confirmó.
      MarketTransaction confirmed = onlyNewAudit(auditBefore, auditOf(player));
      assertThat(confirmed.getBuyer().getId()).isEqualTo(buyer.getId());
      assertThat(confirmed.getSeller().getId()).isEqualTo(superuser.getId());
      assertThat(confirmed.getPlayer().getId()).isEqualTo(player.getId());
      assertThat(confirmed.getQuantity()).isEqualTo(QUANTITY);
      assertThat(confirmed.getUnitPrice()).isEqualTo(PRICE);
      assertThat(confirmed.getTotalAmount()).isEqualTo(PRICE * QUANTITY);
      assertThat(confirmed.getBuyerCreditsBefore()).isEqualTo(PRICE * QUANTITY);
      assertThat(confirmed.getBuyerCreditsAfter()).isZero();
      assertThat(confirmed.getSellerCreditsBefore()).isEqualTo(sellerCreditsBefore);
      assertThat(confirmed.getSellerCreditsAfter())
          .isEqualTo(sellerCreditsBefore + PRICE * QUANTITY);
      assertThat(confirmed.getBuyerTokensBefore()).isZero();
      assertThat(confirmed.getBuyerTokensAfter()).isEqualTo(QUANTITY);
      assertThat(confirmed.getSellerTokensBefore()).isEqualTo(EMITTED);
      assertThat(confirmed.getSellerTokensAfter()).isEqualTo(EMITTED - QUANTITY);
    }

    @Test
    @DisplayName(
        "Las compras simultáneas del mismo saldo sobre jugadores distintos tampoco permiten el doble gasto")
    void comprasSimultaneasSobreJugadoresDistintosSinDobleGasto() throws Exception {
      // Arrange: AC09 exige la garantía incluso sobre jugadores diferentes, con saldo justo para
      // una.
      Player other = market.createVisiblePlayer();
      market.emitInitialTokens(other, superuser);
      credits.saveAndFlush(UserCredit.restore(buyer.getId(), PRICE));
      List<MarketTransaction> auditBefore = auditOfBoth(player, other);
      long sellerCreditsBefore = creditOf(superuser);
      long creditsBefore = creditOf(buyer) + creditOf(superuser);
      long heldBefore = totalHeld(player) + totalHeld(other);

      // Act: cada operación compra un token de un jugador distinto, con el mismo comprador.
      List<Outcome> outcomes = race(buyer, player, 1, buyer, other, 1);

      // Assert
      assertThat(outcomes).containsExactlyInAnyOrder(Outcome.REJECTED_CREDITS, Outcome.CONFIRMED);

      // SC-008: los dos jugadores conservan su emisión y la suma de créditos no cambia.
      assertThat(totalHeld(player)).isEqualTo(EMITTED);
      assertThat(totalHeld(other)).isEqualTo(EMITTED);
      assertThat(totalHeld(player) + totalHeld(other)).isEqualTo(heldBefore);
      assertThat(creditOf(buyer)).isZero();
      assertThat(creditOf(superuser)).isEqualTo(sellerCreditsBefore + PRICE);
      // SC-008: la suma de créditos de las dos partes se conserva. La referencia es la suma previa
      // de ambas, no el saldo individual del vendedor, que por sí solo no representa las partes.
      assertThat(creditOf(buyer) + creditOf(superuser)).isEqualTo(creditsBefore);

      // SC-010: una sola operación confirmada genera un único registro sobre los dos jugadores, y
      // la rechazada no genera ninguno.
      MarketTransaction confirmed = onlyNewAudit(auditBefore, auditOfBoth(player, other));
      Player confirmedPlayer = confirmed.getPlayer();
      Player rejectedPlayer = confirmedPlayer.getId().equals(player.getId()) ? other : player;
      assertThat(confirmed.getBuyer().getId()).isEqualTo(buyer.getId());
      assertThat(confirmed.getSeller().getId()).isEqualTo(superuser.getId());
      assertThat(confirmed.getQuantity()).isEqualTo(1);
      assertThat(confirmed.getUnitPrice()).isEqualTo(PRICE);
      assertThat(confirmed.getTotalAmount()).isEqualTo(PRICE);
      assertThat(confirmed.getBuyerCreditsBefore()).isEqualTo(PRICE);
      assertThat(confirmed.getBuyerCreditsAfter()).isZero();
      assertThat(confirmed.getSellerCreditsBefore()).isEqualTo(sellerCreditsBefore);
      assertThat(confirmed.getSellerCreditsAfter()).isEqualTo(sellerCreditsBefore + PRICE);
      assertThat(confirmed.getBuyerTokensBefore()).isZero();
      assertThat(confirmed.getBuyerTokensAfter()).isEqualTo(1);
      assertThat(confirmed.getSellerTokensBefore()).isEqualTo(EMITTED);
      assertThat(confirmed.getSellerTokensAfter()).isEqualTo(EMITTED - 1);

      // El jugador rechazado conserva su inventario íntegro y no registra tenencia del comprador.
      assertThat(holdingOf(confirmedPlayer, buyer)).isEqualTo(1);
      assertThat(holdingOf(confirmedPlayer, superuser)).isEqualTo(EMITTED - 1);
      assertThat(holdingOf(rejectedPlayer, superuser)).isEqualTo(EMITTED);
      assertThat(holdings.findByPlayerIdAndOwnerId(rejectedPlayer.getId(), buyer.getId()))
          .isEmpty();
    }

    @Test
    @DisplayName("La creación concurrente de la primera tenencia no duplica filas")
    void creacionConcurrenteDeLaPrimeraTenencia() throws Exception {
      // Arrange: ninguno de los dos compradores tiene tenencia previa de este jugador.
      User first = market.registerBuyer();
      User second = market.registerBuyer();
      List<MarketTransaction> auditBefore = auditOf(player);
      long creditsBefore = totalCreditsOf(first, second, superuser);
      long sellerCreditsBefore = creditOf(superuser);

      // Act
      List<Outcome> outcomes = race(first, player, QUANTITY, second, player, QUANTITY);

      // Assert: ambas operaciones son válidas y ambas deben confirmar.
      assertThat(outcomes).containsExactlyInAnyOrder(Outcome.CONFIRMED, Outcome.CONFIRMED);

      // Una fila por parte, el vendedor y los dos compradores, sin perder la emisión inicial.
      assertThat(
              holdings.findAll().stream()
                  .filter(token -> token.getPlayer().getId().equals(player.getId()))
                  .count())
          .isEqualTo(3);
      assertThat(holdingOf(player, first)).isEqualTo(QUANTITY);
      assertThat(holdingOf(player, second)).isEqualTo(QUANTITY);
      assertThat(holdingOf(player, superuser)).isEqualTo(EMITTED - 2 * QUANTITY);
      assertThat(totalHeld(player))
          .isEqualTo(
              holdingOf(player, first) + holdingOf(player, second) + holdingOf(player, superuser));
      assertThat(totalHeld(player)).isEqualTo(EMITTED);

      // SC-008 y SC-010: un registro por operación confirmada y suma de créditos intacta.
      assertThat(totalCreditsOf(first, second, superuser)).isEqualTo(creditsBefore);
      assertThat(creditOf(superuser)).isEqualTo(sellerCreditsBefore + 2 * PRICE * QUANTITY);
      assertThat(auditOf(player)).hasSize(auditBefore.size() + 2);
      assertThat(
              auditOf(player).stream().map(MarketTransaction::getBuyer).map(User::getId).toList())
          .contains(first.getId(), second.getId());
    }

    /**
     * Ejecuta dos compras en paralelo, cada una con su propia conexión y transacción.
     *
     * <p>Cada operación usa el usuario, el jugador y la cantidad que recibe. Ambas transacciones se
     * abren y esperan en una barrera antes de invocar el caso de uso, de modo que quedan liberadas
     * a la vez: eso las distingue de una ejecución secuencial, que también satisfaría las mismas
     * aserciones. La barrera no demuestra por sí sola que ambas compitan por una misma fila
     * bloqueada de PostgreSQL; observar esa contención exigiría comprobar el bloqueo desde fuera y
     * queda fuera del alcance de estas pruebas.
     *
     * @return el resultado observado de cada operación
     */
    private List<Outcome> race(
        User firstUser,
        Player firstTarget,
        int firstQuantity,
        User secondUser,
        Player secondTarget,
        int secondQuantity)
        throws Exception {
      CyclicBarrier barrier = new CyclicBarrier(2);
      ExecutorService executor = Executors.newFixedThreadPool(2);
      try {
        Future<Outcome> one =
            executor.submit(() -> attempt(firstUser, firstTarget, firstQuantity, barrier));
        Future<Outcome> other =
            executor.submit(() -> attempt(secondUser, secondTarget, secondQuantity, barrier));
        return List.of(
            one.get(OUTCOME_TIMEOUT_SECONDS, TimeUnit.SECONDS),
            other.get(OUTCOME_TIMEOUT_SECONDS, TimeUnit.SECONDS));
      } finally {
        executor.shutdownNow();
      }
    }

    /**
     * Ejecuta una compra dentro de su propia transacción y clasifica el resultado por tipo de
     * excepción.
     *
     * <p>Solo las excepciones tipadas del contrato se convierten en un resultado; cualquier otra se
     * propaga, de modo que un fallo inesperado no pueda disfrazarse de rechazo previsto.
     *
     * @param user comprador de la operación
     * @param target jugador comprado
     * @param quantity cantidad solicitada
     * @param barrier barrera que coordina la liberación simultánea
     * @return resultado observado de la operación
     */
    private Outcome attempt(User user, Player target, int quantity, CyclicBarrier barrier) {
      TransactionTemplate template = new TransactionTemplate(transactionManager);
      try {
        return template.execute(
            status -> {
              releaseTogether(barrier);
              buyOrderService.buy(user.getEmail(), new BuyOrderRequest(target.getId(), quantity));
              return Outcome.CONFIRMED;
            });
      } catch (InsufficientStockException rejected) {
        return Outcome.REJECTED_STOCK;
      } catch (InsufficientCreditsException rejected) {
        return Outcome.REJECTED_CREDITS;
      }
    }

    /**
     * Espera en la barrera hasta que las dos transacciones estén abiertas.
     *
     * <p>La espera es acotada y no usa {@code Thread.sleep}: si la otra operación no llega, la
     * carrera termina con un error explícito en lugar de quedar bloqueada indefinidamente.
     *
     * @param barrier barrera compartida por las dos operaciones
     */
    private void releaseTogether(CyclicBarrier barrier) {
      try {
        barrier.await(BARRIER_TIMEOUT_SECONDS, TimeUnit.SECONDS);
      } catch (InterruptedException interrupted) {
        Thread.currentThread().interrupt();
        throw new IllegalStateException(
            "Interrupción al coordinar la liberación simultánea de las compras.", interrupted);
      } catch (BrokenBarrierException | TimeoutException uncoordinated) {
        throw new IllegalStateException(
            "Las dos compras no pudieron liberarse a la vez; la coordinación simultánea es la"
                + " precondición de esta prueba.",
            uncoordinated);
      }
    }

    /**
     * Devuelve el único registro financiero que la carrera añadió.
     *
     * <p>Se exige exactamente uno: la operación confirmada lo escribe y la rechazada no debe dejar
     * ninguno (SC-010).
     *
     * @param before registros existentes antes de la carrera
     * @param after registros existentes después de la carrera
     * @return registro financiero de la operación confirmada
     */
    private MarketTransaction onlyNewAudit(
        List<MarketTransaction> before, List<MarketTransaction> after) {
      assertThat(after)
          .as("una compra confirmada debe añadir exactamente un registro financiero")
          .hasSize(before.size() + 1);
      List<Long> known = before.stream().map(MarketTransaction::getId).toList();
      List<MarketTransaction> created =
          after.stream().filter(row -> !known.contains(row.getId())).toList();
      assertThat(created).as("el registro financiero creado debe ser único").hasSize(1);
      return created.get(0);
    }

    /**
     * Suma los saldos de todas las partes que participan en una carrera.
     *
     * <p>SC-008 exige que las transferencias no cambien la suma de créditos de las partes.
     *
     * @param participants partes cuya suma de saldos se afirma
     * @return suma de los saldos de crédito
     */
    private long totalCreditsOf(User... participants) {
      long total = 0L;
      for (User participant : participants) {
        total += creditOf(participant);
      }
      return total;
    }

    /**
     * Reúne los registros financieros de dos jugadores.
     *
     * <p>Una carrera sobre jugadores distintos reparte sus registros entre ambos, de modo que la
     * afirmación sobre la única operación confirmada debe observar los dos conjuntos.
     *
     * @param first primer jugador
     * @param second segundo jugador
     * @return registros financieros de ambos jugadores
     */
    private List<MarketTransaction> auditOfBoth(Player first, Player second) {
      List<MarketTransaction> rows = new ArrayList<>(auditOf(first));
      rows.addAll(auditOf(second));
      return rows;
    }
  }
}
