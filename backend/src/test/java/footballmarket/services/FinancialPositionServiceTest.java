package footballmarket.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.when;

import footballmarket.features.auth.domain.User;
import footballmarket.features.catalog.domain.models.player.Player;
import footballmarket.features.market.application.BuyOrderService;
import footballmarket.features.market.application.TokenListingService;
import footballmarket.features.market.domain.records.BuyOrderRequest;
import footballmarket.features.market.domain.records.SellOrderRequest;
import footballmarket.features.portfolio.application.FinancialPositionService;
import footballmarket.features.portfolio.domain.exceptions.FinancialPositionIntegrityException;
import footballmarket.features.portfolio.domain.models.PlayerFinancialPosition;
import footballmarket.features.quote.application.QuoteSource;
import footballmarket.features.quote.application.exceptions.QuoteBatchUnavailableException;
import footballmarket.features.quote.application.exceptions.QuoteContractViolationException;
import footballmarket.features.quote.domain.records.CurrentQuote;
import footballmarket.support.FinancialTestData;
import footballmarket.support.MarketTestData;
import footballmarket.support.TestcontainersConfiguration;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/** API del caso de uso real con PostgreSQL y proveedor sustituido; fixtures solo en Arrange. */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
@Tag("integration")
class FinancialPositionServiceTest {
  private static final OffsetDateTime NOW = OffsetDateTime.parse("2026-10-20T12:00:00Z");
  @Autowired private FinancialPositionService financial;
  @Autowired private MarketTestData market;
  @Autowired private FinancialTestData fixtures;
  @Autowired private BuyOrderService purchases;
  @Autowired private TokenListingService listings;
  @MockitoBean private QuoteSource quotes;

  @MockitoBean
  @Qualifier("marketClock")
  private Clock marketClock;

  private User owner;
  private User seller;
  private Player player;

  @BeforeEach
  void arrange() {
    owner = market.registerBuyer();
    seller = market.registerSuperuser();
    player = market.createVisiblePlayer();
    when(marketClock.getZone()).thenReturn(ZoneOffset.UTC);
    when(marketClock.instant()).thenReturn(Instant.parse("2026-10-20T12:00:00Z"));
    when(quotes.currentQuotes(anySet()))
        .thenAnswer(
            invocation -> {
              Set<Long> ids = invocation.getArgument(0);
              Map<Long, CurrentQuote> batch = new HashMap<>();
              for (Long id : ids) {
                batch.put(id, new CurrentQuote(id, 150L, NOW, "fixture"));
              }
              return Map.copyOf(batch);
            });
  }

  @Nested
  @DisplayName("Posiciones propias e historial")
  class OwnPositions {
    @Test
    @DisplayName("Reconstruye solo las operaciones propias y conserva jugadores inactivos")
    void reconstruyeHistorialPropio() {
      // Arrange
      User receiver = market.registerBuyer();
      fixtures.record(owner, seller, player, 10, 100, NOW);
      fixtures.record(receiver, owner, player, 5, 200, NOW.plusSeconds(1));
      Player otherPlayer = market.createVisiblePlayer();
      fixtures.record(receiver, seller, otherPlayer, 2, 100, NOW);
      fixtures.deactivate(player);
      // Act
      List<PlayerFinancialPosition> result = financial.positionsOf(owner.getId());
      // Assert
      assertThat(result).hasSize(1);
      PlayerFinancialPosition position = result.getFirst();
      assertThat(position.playerId()).isEqualTo(player.getId());
      assertThat(position.acquisitionCost()).isEqualTo(500);
      assertThat(position.realizedProfitLoss()).isEqualTo(500);
      assertThat(position.totalProfitLoss()).isEqualTo(750L);
      assertThat(position.quoteCalculatedAt()).isEqualTo(NOW);
      assertThat(financial.positionsOf(owner.getId())).isEqualTo(result);
    }

    @Test
    @DisplayName("La publicación vigente no reduce cantidad, costo ni resultados")
    void publicarNoEsVender() {
      // Arrange
      market.emitInitialTokens(player, seller);
      CurrentQuote quote = new CurrentQuote(player.getId(), 100L, NOW, "fixture");
      when(quotes.currentQuote(player.getId())).thenReturn(Optional.of(quote));
      purchases.buy(owner.getEmail(), new BuyOrderRequest(player.getId(), 10));
      listings.publish(owner.getEmail(), new SellOrderRequest(player.getId(), 5));
      // Act
      PlayerFinancialPosition result = financial.positionsOf(owner.getId()).getFirst();
      // Assert
      assertThat(result.openQuantity()).isEqualTo(10);
      assertThat(result.acquisitionCost()).isEqualTo(1000);
      assertThat(result.sellOperationCount()).isZero();
      assertThat(result.realizedProfitLoss()).isZero();
    }

    @Test
    @DisplayName("La emisión se valora a costo cero sin compras ficticias")
    void valoraEmision() {
      // Arrange
      market.emitInitialTokens(player, seller);
      // Act
      List<PlayerFinancialPosition> result = financial.positionsOf(seller.getId());
      // Assert
      PlayerFinancialPosition position =
          result.stream()
              .filter(value -> value.playerId().equals(player.getId()))
              .findFirst()
              .orElseThrow();
      assertThat(position.acquisitionCost()).isZero();
      assertThat(position.openQuantity()).isEqualTo(100);
      assertThat(position.buyOperationCount()).isZero();
      assertThat(position.lastTransactionAt()).isNull();
      assertThat(position.openedAt()).isNotNull();
      assertThat(position.currentValue()).isEqualTo(15000L);
    }
  }

  @Nested
  @DisplayName("Adquisición masiva y fallos")
  class Quotes {
    @Test
    @DisplayName("El lote recibe todos los jugadores abiertos sin usar la lectura individual")
    void adquiereLoteUnico() {
      // Arrange
      Player second = market.createVisiblePlayer();
      fixtures.record(owner, seller, player, 1, 100, NOW);
      fixtures.record(owner, seller, second, 1, 100, NOW);
      AtomicReference<Set<Long>> requested = new AtomicReference<>();
      when(quotes.currentQuote(org.mockito.ArgumentMatchers.any()))
          .thenThrow(new AssertionError("No debe consultar por jugador"));
      when(quotes.currentQuotes(anySet()))
          .thenAnswer(
              invocation -> {
                Set<Long> ids = invocation.getArgument(0);
                if (!requested.compareAndSet(null, ids)) {
                  throw new AssertionError("No debe solicitar más de un lote");
                }
                return Map.of();
              });
      // Act
      List<PlayerFinancialPosition> result = financial.positionsOf(owner.getId());
      // Assert
      assertThat(result)
          .hasSize(2)
          .allSatisfy(position -> assertThat(position.currentValue()).isNull());
      assertThat(requested.get()).containsExactlyInAnyOrder(player.getId(), second.getId());
    }

    @Test
    @DisplayName("El portfolio vacío no solicita cotizaciones")
    void omiteLoteVacio() {
      // Arrange
      when(quotes.currentQuotes(anySet()))
          .thenThrow(new AssertionError("No hay jugadores abiertos"));
      // Act / Assert
      assertThat(financial.positionsOf(owner.getId())).isEmpty();
    }

    @Test
    @DisplayName("La ausencia conserva realizado y cantidades, pero no inventa valoración")
    void ausenciaNoEsFallo() {
      // Arrange
      fixtures.record(owner, seller, player, 1, 100, NOW);
      when(quotes.currentQuotes(anySet())).thenReturn(Map.of());
      // Act
      PlayerFinancialPosition result = financial.positionsOf(owner.getId()).getFirst();
      // Assert
      assertThat(result.openQuantity()).isEqualTo(1);
      assertThat(result.currentUnitPrice()).isNull();
      assertThat(result.quoteCalculatedAt()).isNull();
      assertThat(result.totalProfitLoss()).isNull();
      assertThat(result.realizedProfitLoss()).isZero();
    }

    @Test
    @DisplayName("El fallo técnico no se convierte en un mapa de cotizaciones ausentes")
    void propagaFallo() {
      // Arrange
      fixtures.record(owner, seller, player, 1, 100, NOW);
      when(quotes.currentQuotes(anySet())).thenThrow(new QuoteBatchUnavailableException());
      // Act / Assert
      assertThatThrownBy(() -> financial.positionsOf(owner.getId()))
          .isInstanceOf(QuoteBatchUnavailableException.class);
    }

    @Test
    @DisplayName("Una clave ajena es incumplimiento del contrato, no ausencia")
    void rechazaClaveAjena() {
      // Arrange
      fixtures.record(owner, seller, player, 1, 100, NOW);
      CurrentQuote foreign = new CurrentQuote(-1L, 150, NOW, null);
      when(quotes.currentQuotes(anySet())).thenReturn(Map.of(-1L, foreign));
      // Act / Assert
      assertThatThrownBy(() -> financial.positionsOf(owner.getId()))
          .isInstanceOf(QuoteContractViolationException.class);
    }

    @Test
    @DisplayName("Una venta sin adquisición previa no genera resultados falsos")
    void rechazaSobreventaHistorica() {
      // Arrange
      User receiver = market.registerBuyer();
      fixtures.record(receiver, owner, player, 1, 100, NOW);
      // Act / Assert
      assertThatThrownBy(() -> financial.positionsOf(owner.getId()))
          .isInstanceOf(FinancialPositionIntegrityException.class);
    }
  }

  @Nested
  @DisplayName("Instantánea ante una compra concurrente")
  class Consistency {
    @Test
    @DisplayName("Una compra confirmada mientras se valora no altera la instantánea ya leída")
    void conservaInstantaneaDuranteCompra() throws Exception {
      // Arrange: escritor de precondiciones mediante la API pública del mercado, sin repositorios.
      market.emitInitialTokens(player, seller);
      CurrentQuote quote = new CurrentQuote(player.getId(), 100, NOW, null);
      when(quotes.currentQuote(player.getId())).thenReturn(Optional.of(quote));
      purchases.buy(owner.getEmail(), new BuyOrderRequest(player.getId(), 1));
      CountDownLatch valuation = new CountDownLatch(1);
      CompletableFuture<Void> writer =
          CompletableFuture.runAsync(
              () -> {
                try {
                  if (!valuation.await(10, TimeUnit.SECONDS)) {
                    throw new AssertionError("No se alcanzó la valoración");
                  }
                  purchases.buy(owner.getEmail(), new BuyOrderRequest(player.getId(), 1));
                } catch (InterruptedException interrupted) {
                  Thread.currentThread().interrupt();
                  throw new AssertionError(interrupted);
                }
              });
      when(quotes.currentQuotes(anySet()))
          .thenAnswer(
              invocation -> {
                valuation.countDown();
                writer.get(10, TimeUnit.SECONDS);
                return Map.of(player.getId(), quote);
              });
      // Act
      PlayerFinancialPosition snapshot = financial.positionsOf(owner.getId()).getFirst();
      // Assert
      assertThat(snapshot.openQuantity()).isEqualTo(1);
      assertThat(snapshot.acquisitionCost()).isEqualTo(100);
      assertThat(financial.positionsOf(owner.getId()).getFirst().openQuantity()).isEqualTo(2);
    }
  }
}
