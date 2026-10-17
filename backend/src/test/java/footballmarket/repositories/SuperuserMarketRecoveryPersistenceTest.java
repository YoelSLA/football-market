package footballmarket.repositories;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import footballmarket.config.SuperuserProperties;
import footballmarket.integrations.QuoteSource;
import footballmarket.models.User;
import footballmarket.models.records.BuyOrderRequest;
import footballmarket.models.records.CurrentQuote;
import footballmarket.models.records.LeagueCandidate;
import footballmarket.models.records.PlayerCandidate;
import footballmarket.models.records.PlayerSnapshot;
import footballmarket.models.records.TeamCandidate;
import footballmarket.services.BuyOrderService;
import footballmarket.services.PlayerCatalogService;
import footballmarket.services.SuperuserProvisioningService;
import footballmarket.services.exceptions.SuperuserProvisioningException;
import footballmarket.support.MarketTestData;
import footballmarket.support.ResetPlayerCatalogListener;
import footballmarket.support.TestcontainersConfiguration;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * Recuperación durable por aprovisionamiento real, en PostgreSQL desechable y sin transacción de
 * test.
 */
@SpringBootTest
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
@TestExecutionListeners(
    listeners = ResetPlayerCatalogListener.class,
    mergeMode = TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS)
class SuperuserMarketRecoveryPersistenceTest {
  @Autowired private PlayerCatalogService catalog;
  @Autowired private SuperuserProvisioningService provisioning;
  @Autowired private BuyOrderService orders;
  @Autowired private MarketTestData market;
  @Autowired private DataSource dataSource;
  @MockitoBean private SuperuserProperties properties;
  @MockitoBean private QuoteSource quotes;

  private JdbcTemplate jdbc;

  @BeforeEach
  void configureProvisioning() {
    jdbc = new JdbcTemplate(dataSource);
    // El mock vale false durante startup: la acción bajo prueba es la llamada explícita al Service.
    when(properties.enabled()).thenReturn(true);
    when(properties.username()).thenReturn("h06.owner");
    when(properties.email()).thenReturn("h06-owner@example.com");
    when(properties.password()).thenReturn("test-only-password");
  }

  private PlayerSnapshot snapshot(boolean includeSecond) {
    LeagueCandidate league = new LeagueCandidate("h06-league", "Liga H06");
    TeamCandidate team = new TeamCandidate("h06-team", "Equipo H06", "h06-league", true);
    PlayerCandidate first =
        new PlayerCandidate(
            "h06-active",
            "Jugador Activo",
            "Equipo H06",
            "Liga H06",
            "Forward",
            null,
            null,
            "h06-team");
    PlayerCandidate second =
        new PlayerCandidate(
            "h06-inactive",
            "Jugador Inactivo",
            "Equipo H06",
            "Liga H06",
            "Forward",
            null,
            null,
            "h06-team");
    List<PlayerCandidate> players = includeSecond ? List.of(first, second) : List.of(first);
    return new PlayerSnapshot(
        players, players.size(), 0, List.of(league), List.of(team), List.of());
  }

  /** Crea dos jugadores por el catálogo y desactiva uno antes de que exista SUPERUSER. */
  private long createPendingPlayers() {
    catalog.applySynchronization(snapshot(true));
    catalog.applySynchronization(snapshot(false));
    return jdbc.queryForObject(
        "SELECT player_id FROM player_external_references"
            + " WHERE provider = 'FOOTBALL_DATA' AND external_id = 'h06-active'",
        Long.class);
  }

  /** Captura filas completas de mercado y usuarios para detectar cambios, incluidos timestamps. */
  private List<List<Map<String, Object>>> marketRows() {
    return List.of(
        jdbc.queryForList("SELECT * FROM users ORDER BY id"),
        jdbc.queryForList("SELECT * FROM user_credits ORDER BY user_id"),
        jdbc.queryForList("SELECT * FROM player_token_emissions ORDER BY player_id"),
        jdbc.queryForList("SELECT * FROM player_tokens ORDER BY id"),
        jdbc.queryForList("SELECT * FROM market_transactions ORDER BY id"));
  }

  private int quantity(long playerId, long ownerId) {
    return jdbc.queryForObject(
        "SELECT quantity FROM player_tokens WHERE player_id = ? AND owner_user_id = ?",
        Integer.class,
        playerId,
        ownerId);
  }

  @Nested
  @DisplayName("Recuperación al aprovisionar el propietario inicial")
  class Recovery {
    @Test
    @DirtiesContext(methodMode = DirtiesContext.MethodMode.AFTER_METHOD)
    @DisplayName(
        "Aprovisionar recupera jugadores activos e inactivos y repetir conserva todas las filas")
    void recuperaPendientesUnaSolaVez() {
      // Arrange: catálogo confirmado antes del SUPERUSER, sin emitir mediante fixtures.
      createPendingPlayers();
      assertThat(jdbc.queryForObject("SELECT count(*) FROM users", Long.class)).isZero();
      assertThat(jdbc.queryForObject("SELECT count(*) FROM player_token_emissions", Long.class))
          .isZero();
      assertThat(jdbc.queryForObject("SELECT count(*) FROM player_tokens", Long.class)).isZero();
      assertThat(jdbc.queryForObject("SELECT count(*) FROM players WHERE NOT active", Long.class))
          .isEqualTo(1L);

      // Act: la transacción del Service incluye usuario, cuenta, emisiones y tenencias.
      User owner = provisioning.provisionSuperuser();

      // Verify: lecturas posteriores al commit real.
      List<Map<String, Object>> issued =
          jdbc.queryForList("SELECT * FROM player_token_emissions ORDER BY player_id");
      assertThat(issued).hasSize(2);
      for (Map<String, Object> emission : issued) {
        assertThat(emission)
            .containsEntry("owner_user_id", owner.getId())
            .containsEntry("initial_quantity", 100);
        assertThat(emission.get("emitted_at")).isNotNull();
        assertThat(quantity(((Number) emission.get("player_id")).longValue(), owner.getId()))
            .isEqualTo(100);
      }
      assertThat(jdbc.queryForObject("SELECT count(*) FROM player_tokens", Long.class))
          .isEqualTo(2L);
      assertThat(jdbc.queryForObject("SELECT available_credits FROM user_credits", Long.class))
          .isZero();
      assertThat(jdbc.queryForObject("SELECT count(*) FROM market_transactions", Long.class))
          .isZero();
      List<List<Map<String, Object>>> beforeRepeat = marketRows();

      // Act
      User repeated = provisioning.provisionSuperuser();

      // Verify: no duplicación ni cambios de saldo, identidad, hash o timestamps.
      assertThat(repeated.getId()).isEqualTo(owner.getId());
      assertThat(marketRows()).isEqualTo(beforeRepeat);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    @DirtiesContext(methodMode = DirtiesContext.MethodMode.AFTER_METHOD)
    @DisplayName(
        "El SUPERUSER existente recupera pendientes con configuración habilitada o deshabilitada")
    void recuperaConPropietarioExistente(boolean enabled) {
      // Arrange: estado omitido anterior a H06, aprovisionado solo como dato del caso Repository.
      long playerId = createPendingPlayers();
      User owner = market.registerSuperuser();
      List<Map<String, Object>> accounts = jdbc.queryForList("SELECT * FROM user_credits");
      when(properties.enabled()).thenReturn(enabled);
      when(properties.email()).thenReturn(owner.getEmail());

      // Act
      User returned = provisioning.provisionSuperuser();

      // Verify
      assertThat(returned.getId()).isEqualTo(owner.getId());
      assertThat(quantity(playerId, owner.getId())).isEqualTo(100);
      assertThat(jdbc.queryForObject("SELECT count(*) FROM player_token_emissions", Long.class))
          .isEqualTo(2L);
      assertThat(jdbc.queryForList("SELECT * FROM user_credits")).isEqualTo(accounts);
      List<List<Map<String, Object>>> recovered = marketRows();
      provisioning.provisionSuperuser();
      assertThat(marketRows()).isEqualTo(recovered);
    }

    @ParameterizedTest
    @ValueSource(ints = {10, 100})
    @DirtiesContext(methodMode = DirtiesContext.MethodMode.AFTER_METHOD)
    @DisplayName("Reaprovisionar no repone inventario parcialmente vendido ni agotado")
    void conservaInventarioYFinanzasTrasComprar(int bought) {
      // Arrange
      long playerId = createPendingPlayers();
      User owner = provisioning.provisionSuperuser();
      User buyer = market.registerBuyer();
      CurrentQuote quote =
          new CurrentQuote(
              playerId, 100L, OffsetDateTime.parse("2026-10-14T10:30:00Z"), "h06-strategy");
      when(quotes.currentQuote(playerId)).thenReturn(Optional.of(quote));
      orders.buy(buyer.getEmail(), new BuyOrderRequest(playerId, bought));
      assertThat(quantity(playerId, owner.getId())).isEqualTo(100 - bought);
      assertThat(quantity(playerId, buyer.getId())).isEqualTo(bought);
      assertThat(jdbc.queryForObject("SELECT count(*) FROM market_transactions", Long.class))
          .isEqualTo(1L);
      List<List<Map<String, Object>>> afterSale = marketRows();

      // Act
      provisioning.provisionSuperuser();

      // Verify: compara todas las filas, no solo totales de inventario.
      assertThat(marketRows()).isEqualTo(afterSale);
      assertThat(quantity(playerId, owner.getId())).isEqualTo(100 - bought);
      assertThat(quantity(playerId, buyer.getId())).isEqualTo(bought);
    }
  }

  @Nested
  @DisplayName("Rechazo de inconsistencias y fallos PostgreSQL")
  class Rejection {
    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    @DirtiesContext(methodMode = DirtiesContext.MethodMode.AFTER_METHOD)
    @DisplayName("Rechaza tenencias sin emisión y emisiones sin tenencia inicial, sin repararlas")
    void rechazaInventarioInconsistente(boolean emissionWithoutHolding) {
      // Arrange: el fixture solo establece un propietario; JDBC introduce el estado inconsistente.
      long playerId = createPendingPlayers();
      User owner = market.registerSuperuser();
      when(properties.email()).thenReturn(owner.getEmail());
      if (emissionWithoutHolding) {
        jdbc.update(
            "INSERT INTO player_token_emissions (player_id, owner_user_id, initial_quantity)"
                + " VALUES (?, ?, 100)",
            playerId,
            owner.getId());
      } else {
        jdbc.update(
            "INSERT INTO player_tokens (player_id, owner_user_id, quantity) VALUES (?, ?, 0)",
            playerId,
            owner.getId());
      }
      // Cuenta ausente: si se intenta asegurarla antes del rechazo, también debe revertirse.
      jdbc.update("DELETE FROM user_credits WHERE user_id = ?", owner.getId());
      List<List<Map<String, Object>>> inconsistent = marketRows();

      // Act / Assert
      assertThatThrownBy(() -> provisioning.provisionSuperuser())
          .isInstanceOf(SuperuserProvisioningException.class)
          .hasMessage(
              "El inventario de tokens es inconsistente. No se realiza reparación automática.");

      // Verify: transacción ya terminada; no reparación, nueva cuenta ni emisión del otro jugador.
      assertThat(marketRows()).isEqualTo(inconsistent);
    }

    @Test
    @DirtiesContext(methodMode = DirtiesContext.MethodMode.AFTER_METHOD)
    @DisplayName("Un rechazo SQL de tenencia revierte también emisiones, SUPERUSER y cuenta nuevos")
    void revierteTodoSiPostgresRechazaLaTenencia() {
      // Arrange: el contexto contiene exclusivamente el PostgreSQL del contenedor de esta clase.
      long playerId = createPendingPlayers();
      List<List<Map<String, Object>>> before = marketRows();
      try {
        // El id es un long generado por nuestra DB, no entrada externa. La constraint solo impide
        // insertar la tenencia del jugador del caso; no modifica ninguna migración.
        jdbc.execute(
            "ALTER TABLE player_tokens ADD CONSTRAINT test_h06_reject_holding"
                + " CHECK (player_id <> "
                + playerId
                + ")");

        // Act: llamada externa al proxy; la excepción se inspecciona tras concluir el rollback.
        Throwable failure =
            org.assertj.core.api.Assertions.catchThrowable(() -> provisioning.provisionSuperuser());

        // Assert: exige rechazo real CHECK de PostgreSQL, no cualquier excepción.
        assertThat(failure).isNotNull();
        boolean checkViolation = false;
        for (Throwable cause = failure; cause != null; cause = cause.getCause()) {
          if (cause instanceof SQLException sqlFailure
              && "23514".equals(sqlFailure.getSQLState())) {
            checkViolation = true;
            break;
          }
        }
        assertThat(checkViolation).as("PostgreSQL debe rechazar la tenencia con 23514").isTrue();

        // Verify: nuevas consultas JDBC autocommit, fuera de la transacción fallida.
        assertThat(marketRows()).isEqualTo(before);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM users", Long.class)).isZero();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM user_credits", Long.class)).isZero();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM player_token_emissions", Long.class))
            .isZero();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM player_tokens", Long.class)).isZero();
      } finally {
        jdbc.execute("ALTER TABLE player_tokens DROP CONSTRAINT IF EXISTS test_h06_reject_holding");
      }
      assertThat(
              jdbc.queryForObject(
                  "SELECT count(*) FROM pg_constraint WHERE conrelid = 'player_tokens'::regclass"
                      + " AND conname = 'test_h06_reject_holding'",
                  Long.class))
          .isZero();
    }
  }
}
