package footballmarket.repositories;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.when;

import footballmarket.features.auth.domain.User;
import footballmarket.features.catalog.domain.models.player.Player;
import footballmarket.features.market.domain.models.MarketTransaction;
import footballmarket.features.market.domain.models.PlayerTokenEmission;
import footballmarket.features.market.infraestructure.MarketTransactionRepository;
import footballmarket.features.market.infraestructure.PlayerTokenEmissionRepository;
import footballmarket.features.portfolio.application.FinancialPositionService;
import footballmarket.features.portfolio.domain.models.PlayerFinancialPosition;
import footballmarket.features.quote.application.QuoteSource;
import footballmarket.features.quote.domain.records.CurrentQuote;
import footballmarket.support.FinancialTestData;
import footballmarket.support.MarketTestData;
import footballmarket.support.TestcontainersConfiguration;
import jakarta.persistence.EntityManagerFactory;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Consultas reales, instantánea PostgreSQL y ausencia de escrituras o N+1 del recorrido contable.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
@Tag("integration")
class FinancialPositionPersistenceTest {
  private static final OffsetDateTime NOW = OffsetDateTime.parse("2026-10-20T12:00:00Z");
  @Autowired private MarketTestData market;
  @Autowired private FinancialTestData fixtures;
  @Autowired private MarketTransactionRepository transactions;
  @Autowired private PlayerTokenEmissionRepository emissions;
  @Autowired private FinancialPositionService financial;
  @Autowired private EntityManagerFactory entityManagerFactory;
  @Autowired private PlatformTransactionManager manager;
  @Autowired private JdbcTemplate jdbc;
  @MockitoBean private QuoteSource quotes;
  private User owner;
  private User seller;
  private Player player;

  @BeforeEach
  void arrange() {
    owner = market.registerBuyer();
    seller = market.registerSuperuser();
    player = market.createVisiblePlayer();
    when(quotes.currentQuotes(anySet()))
        .thenAnswer(
            invocation -> {
              Set<Long> ids = invocation.getArgument(0);
              Map<Long, CurrentQuote> batch = new HashMap<>();
              for (Long id : ids) {
                batch.put(id, new CurrentQuote(id, 150L, NOW, null));
              }
              return batch;
            });
  }

  @Nested
  @DisplayName("Consultas contables de conjunto")
  class Queries {
    @Test
    @DisplayName("Incluye ambos roles, excluye terceros y desempata por identidad")
    void ordenaAmbosRoles() {
      // Arrange
      User receiver = market.registerBuyer();
      MarketTransaction first = fixtures.record(owner, seller, player, 10, 100, NOW);
      MarketTransaction second = fixtures.record(receiver, owner, player, 5, 200, NOW);
      fixtures.record(receiver, seller, player, 1, 100, NOW.minusDays(1));
      // Act
      List<MarketTransaction> result = transactions.findFinancialHistory(owner.getId());
      // Assert
      assertThat(result)
          .extracting(MarketTransaction::getId)
          .containsExactly(first.getId(), second.getId());
    }

    @Test
    @DisplayName("Filtra las emisiones por propietario y existe el índice correspondiente")
    void consultaEmisionesEIndice() {
      // Arrange
      market.emitInitialTokens(player, seller);
      // Act
      List<PlayerTokenEmission> result = emissions.findFinancialEmissions(seller.getId());
      // Assert
      assertThat(result)
          .anySatisfy(
              emission -> {
                assertThat(emission.getPlayerId()).isEqualTo(player.getId());
                assertThat(emission.getInitialQuantity()).isEqualTo(100);
                assertThat(emission.getOwner().getId()).isEqualTo(seller.getId());
              });
      assertThat(emissions.findFinancialEmissions(owner.getId())).isEmpty();
      assertThat(
              jdbc.queryForObject(
                  "select count(*) from pg_indexes where schemaname = current_schema()"
                      + " and indexname = 'idx_player_token_emissions_owner'",
                  Long.class))
          .isEqualTo(1L);
    }

    @Test
    @DisplayName("El número de consultas no crece con las posiciones y no escribe nada")
    void evitaNMasUnoYEscrituras() {
      // Arrange: un usuario con una posición y otro con tres, sobre la misma forma de datos.
      Player second = market.createVisiblePlayer();
      Player third = market.createVisiblePlayer();
      User several = market.registerBuyer();
      fixtures.record(owner, seller, player, 1, 100, NOW);
      fixtures.record(several, seller, player, 1, 100, NOW);
      fixtures.record(several, seller, second, 1, 100, NOW);
      fixtures.record(several, seller, third, 1, 100, NOW);
      Statistics statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
      boolean previous = statistics.isStatisticsEnabled();
      statistics.setStatisticsEnabled(true);
      try {
        // Act: se mide el mismo cálculo con una y con varias posiciones, en caliente.
        financial.positionsOf(owner.getId());
        statistics.clear();
        financial.positionsOf(owner.getId());
        long onePosition = statistics.getPrepareStatementCount();
        statistics.clear();
        financial.positionsOf(several.getId());
        long threePositions = statistics.getPrepareStatementCount();
        // Assert
        // Una lectura por posición incrementaría el contador; el cálculo es de conjunto y su
        // coste no depende de cuántas posiciones haya.
        assertThat(threePositions)
            .as("consultas ejecutadas con una y con tres posiciones")
            .isEqualTo(onePosition);
        assertThat(statistics.getEntityInsertCount()).isZero();
        assertThat(statistics.getEntityUpdateCount()).isZero();
        assertThat(statistics.getEntityDeleteCount()).isZero();
      } finally {
        statistics.setStatisticsEnabled(previous);
      }
    }
  }

  @Nested
  @DisplayName("Instantánea compartida entre operaciones y emisiones")
  class Snapshot {
    @Test
    @DisplayName("Una confirmación entre ambas lecturas queda fuera de la misma instantánea")
    void conservaSnapshotEntreConsultas() {
      // Arrange
      fixtures.record(owner, seller, player, 1, 100, NOW);
      Player concurrent = market.createVisiblePlayer();
      TransactionTemplate read = new TransactionTemplate(manager);
      read.setReadOnly(true);
      read.setIsolationLevel(TransactionDefinition.ISOLATION_REPEATABLE_READ);
      // Act / Assert: categoría Repository permite observar ambas lecturas y el estado durable.
      read.executeWithoutResult(
          status -> {
            List<MarketTransaction> before = transactions.findFinancialHistory(owner.getId());
            assertThat(before).hasSize(1);
            CompletableFuture.runAsync(
                    () -> {
                      market.emitInitialTokens(concurrent, seller);
                      fixtures.record(owner, seller, concurrent, 1, 100, NOW.plusSeconds(1));
                    })
                .orTimeout(10, TimeUnit.SECONDS)
                .join();
            assertThat(emissions.findFinancialEmissions(seller.getId()))
                .extracting(PlayerTokenEmission::getPlayerId)
                .doesNotContain(concurrent.getId());
            assertThat(transactions.findFinancialHistory(owner.getId())).hasSize(1);
            assertThat(financial.positionsOf(owner.getId()))
                .extracting(PlayerFinancialPosition::playerId)
                .containsExactly(player.getId());
          });
      // Verify
      assertThat(transactions.findFinancialHistory(owner.getId())).hasSize(2);
      assertThat(emissions.findFinancialEmissions(seller.getId()))
          .extracting(PlayerTokenEmission::getPlayerId)
          .contains(concurrent.getId());
    }
  }
}
