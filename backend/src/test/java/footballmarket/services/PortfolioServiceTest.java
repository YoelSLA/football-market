package footballmarket.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import footballmarket.features.auth.domain.User;
import footballmarket.features.catalog.domain.models.player.Player;
import footballmarket.features.portfolio.domain.exceptions.PortfolioIntegrityException;
import footballmarket.features.portfolio.domain.records.PortfolioView;
import footballmarket.features.portfolio.presentation.exceptions.InvalidPortfolioPageException;
import footballmarket.features.portfolio.presentation.exceptions.InvalidPortfolioStatusException;
import footballmarket.features.quote.domain.records.CurrentQuote;
import footballmarket.integrations.QuoteSource;
import footballmarket.support.FinancialTestData;
import footballmarket.support.PortfolioTestData;
import footballmarket.support.TestcontainersConfiguration;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.convention.TestBean;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * API pública real de la consulta del portfolio; el estado de mercado se prepara solo en Arrange
 * según {@code docs/backend/testing.md} §5 y §6.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
@org.junit.jupiter.api.Tag("integration")
class PortfolioServiceTest {

  private static final OffsetDateTime FIRST = OffsetDateTime.parse("2026-10-18T10:00:00Z");
  private static final OffsetDateTime SECOND = OffsetDateTime.parse("2026-10-19T10:00:00Z");

  @Autowired private PortfolioService portfolioService;
  @Autowired private PortfolioTestData data;
  @Autowired private FinancialTestData financial;

  @MockitoBean private QuoteSource quotes;

  @TestBean(name = "marketClock", methodName = "fixedMarketClock")
  private Clock marketClock;

  private Long playerWithoutQuote;

  /**
   * Reloj del mercado fijado: la vigencia de las publicaciones se decide contra este instante y no
   * contra la hora real de la máquina, de modo que el escenario no dependa del día en que se
   * ejecuta.
   *
   * @return reloj fijo posterior a las operaciones del Arrange
   */
  static Clock fixedMarketClock() {
    return Clock.fixed(Instant.parse("2026-10-19T12:00:00Z"), ZoneOffset.UTC);
  }

  @BeforeEach
  void arrange() {
    this.playerWithoutQuote = null;
    when(this.quotes.currentQuotes(any()))
        .thenAnswer(
            invocation -> {
              Set<Long> ids = invocation.getArgument(0);
              Map<Long, CurrentQuote> result = new HashMap<>();
              for (Long id : ids) {
                if (!id.equals(this.playerWithoutQuote)) {
                  result.put(id, new CurrentQuote(id, 100L, SECOND, "fixture"));
                }
              }
              return result;
            });
  }

  @Nested
  @DisplayName("Composición del portfolio")
  class Composicion {

    @Test
    @DisplayName(
        "Informa la posición abierta con cantidad, costo, valor actual y resultado realizado")
    void informaPosicionAbierta() {
      // Arrange
      User buyer = data.registerBuyer();
      User seller = data.registerSuperuser();
      Player player = data.createVisiblePlayer();
      data.buy(buyer, seller, player, 10, 100L, FIRST);
      // Act
      PortfolioView view = portfolioService.portfolio(buyer.getEmail(), 0, 20, null);
      // Assert
      assertThat(view.positions()).hasSize(1);
      PortfolioView.PortfolioPosition position = view.positions().get(0);
      assertThat(position.playerId()).isEqualTo(player.getId());
      assertThat(position.playerName()).isEqualTo(player.getName());
      assertThat(position.teamName()).isEqualTo(player.getTeam().getName());
      assertThat(position.active()).isTrue();
      assertThat(position.status()).isEqualTo("OPEN");
      assertThat(position.valuationStatus()).isEqualTo("VALUED");
      assertThat(position.quantity()).isEqualTo(10);
      assertThat(position.reservedQuantity()).isZero();
      assertThat(position.availableQuantity()).isEqualTo(10);
      assertThat(position.acquisitionCost()).isEqualTo(1000L);
      assertThat(position.averageUnitPrice()).isEqualTo(100L);
      assertThat(position.currentUnitPrice()).isEqualTo(100L);
      assertThat(position.priceUpdatedAt()).isEqualTo(SECOND);
      assertThat(position.currentValue()).isEqualTo(1000L);
      assertThat(position.realizedProfitLoss()).isZero();
      assertThat(position.openedAt()).isEqualTo(FIRST);
      assertThat(position.closedAt()).isNull();
      assertThat(position.lastTransactionAt()).isEqualTo(FIRST);
      assertThat(position.totalBuyTransactions()).isEqualTo(1);
      assertThat(position.totalSellTransactions()).isZero();
    }

    @Test
    @DisplayName("Reserva los tokens de las publicaciones vigentes y deja el resto disponible")
    void reservaTokensVigentes() {
      // Arrange
      User buyer = data.registerBuyer();
      User seller = data.registerSuperuser();
      Player player = data.createVisiblePlayer();
      data.buy(buyer, seller, player, 10, 100L, FIRST);
      data.publish(buyer, player, 4, SECOND);
      // Act
      PortfolioView view = portfolioService.portfolio(buyer.getEmail(), 0, 20, null);
      // Assert
      assertThat(view.positions().get(0).reservedQuantity()).isEqualTo(4);
      assertThat(view.positions().get(0).availableQuantity()).isEqualTo(6);
      assertThat(view.summary().totalReservedTokens()).isEqualTo(4);
      assertThat(view.summary().totalAvailableTokens()).isEqualTo(6);
      assertThat(view.summary().totalTokens()).isEqualTo(10);
    }

    @Test
    @DisplayName("Un usuario sin operaciones recibe un portfolio vacío con los indicadores en cero")
    void portfolioVacio() {
      // Arrange
      User buyer = data.registerBuyer();
      // Act
      PortfolioView view = portfolioService.portfolio(buyer.getEmail(), 0, 20, null);
      // Assert
      assertThat(view.positions()).isEmpty();
      assertThat(view.summary().totalPositions()).isZero();
      assertThat(view.summary().totalTokens()).isZero();
      assertThat(view.summary().totalInvested()).isZero();
      assertThat(view.summary().valuationComplete()).isTrue();
      assertThat(view.pagination().totalElements()).isZero();
    }

    @Test
    @DisplayName(
        "La emisión inicial del superusuario aparece con costo cero y sin compras contadas")
    void posicionEmitidaDelSuperusuario() {
      // Arrange
      User superuser = data.registerSuperuser();
      User buyer = data.registerBuyer();
      Player player = data.createVisiblePlayer();
      data.buy(buyer, superuser, player, 10, 100L, FIRST);
      // Act
      PortfolioView view = portfolioService.portfolio(superuser.getEmail(), 0, 100, null);
      // Assert: la posición se localiza por jugador porque el superusuario es un dato compartido
      PortfolioView.PortfolioPosition position =
          view.positions().stream()
              .filter(candidate -> candidate.playerId().equals(player.getId()))
              .findFirst()
              .orElseThrow();
      assertThat(position.quantity()).isEqualTo(90);
      assertThat(position.acquisitionCost()).isZero();
      assertThat(position.averageUnitPrice()).isZero();
      assertThat(position.totalBuyTransactions()).isZero();
    }

    @Test
    @DisplayName("Una posición sin cotización conserva cantidades y resultado realizado")
    void posicionSinCotizacion() {
      // Arrange
      User buyer = data.registerBuyer();
      User seller = data.registerSuperuser();
      Player player = data.createVisiblePlayer();
      data.buy(buyer, seller, player, 10, 100L, FIRST);
      playerWithoutQuote = player.getId();
      // Act
      PortfolioView view = portfolioService.portfolio(buyer.getEmail(), 0, 20, null);
      // Assert
      PortfolioView.PortfolioPosition position = view.positions().get(0);
      assertThat(position.quantity()).isEqualTo(10);
      assertThat(position.valuationStatus()).isEqualTo("UNVALUED");
      assertThat(position.currentUnitPrice()).isNull();
      assertThat(position.priceUpdatedAt()).isNull();
      assertThat(position.currentValue()).isNull();
      assertThat(position.unrealizedProfitLoss()).isNull();
      assertThat(position.totalProfitLoss()).isNull();
      assertThat(view.summary().valuationComplete()).isFalse();
      assertThat(view.summary().totalCurrentValue()).isNull();
      assertThat(view.summary().totalProfitLossPercentage()).isNull();
    }

    @Test
    @DisplayName(
        "Una posición cerrada conserva el resultado realizado y anula sus campos de cotización")
    void posicionCerrada() {
      // Arrange
      User buyer = data.registerBuyer();
      User seller = data.registerSuperuser();
      User other = data.registerBuyer();
      Player player = data.createVisiblePlayer();
      data.buy(buyer, seller, player, 10, 100L, FIRST);
      data.sell(buyer, other, player, 10, 150L, SECOND);
      // Act
      PortfolioView view = portfolioService.portfolio(buyer.getEmail(), 0, 20, null);
      // Assert
      PortfolioView.PortfolioPosition position = view.positions().get(0);
      assertThat(position.status()).isEqualTo("CLOSED");
      assertThat(position.valuationStatus()).isEqualTo("NOT_APPLICABLE");
      assertThat(position.quantity()).isZero();
      assertThat(position.realizedProfitLoss()).isEqualTo(500L);
      assertThat(position.totalProfitLoss()).isEqualTo(500L);
      assertThat(position.currentUnitPrice()).isNull();
      assertThat(position.priceUpdatedAt()).isNull();
      assertThat(position.currentValue()).isNull();
      assertThat(position.unrealizedProfitLoss()).isNull();
      assertThat(position.averageUnitPrice()).isNull();
      assertThat(position.closedAt()).isEqualTo(SECOND);
      assertThat(position.totalSellTransactions()).isEqualTo(1);
    }

    @Test
    @DisplayName("El jugador inactivo permanece en el portfolio con sus datos descriptivos")
    void jugadorInactivo() {
      // Arrange
      User buyer = data.registerBuyer();
      User seller = data.registerSuperuser();
      Player player = data.createVisiblePlayer();
      data.buy(buyer, seller, player, 10, 100L, FIRST);
      financial.deactivate(player);
      // Act
      PortfolioView view = portfolioService.portfolio(buyer.getEmail(), 0, 20, null);
      // Assert
      assertThat(view.positions()).hasSize(1);
      assertThat(view.positions().get(0).active()).isFalse();
      assertThat(view.positions().get(0).playerName()).isEqualTo(player.getName());
    }

    @Test
    @DisplayName(
        "Una tenencia que no coincide con las operaciones se informa como error de integridad")
    void divergenciaEntreTenenciaYReconstruccion() {
      // Arrange
      User buyer = data.registerBuyer();
      User seller = data.registerSuperuser();
      Player player = data.createVisiblePlayer();
      data.buy(buyer, seller, player, 10, 100L, FIRST);
      data.breakHolding(buyer, player, 7);
      // Act / Assert
      assertThatThrownBy(() -> portfolioService.portfolio(buyer.getEmail(), 0, 20, null))
          .isInstanceOf(PortfolioIntegrityException.class);
    }

    @Test
    @DisplayName("La consulta no altera el estado del mercado")
    void consultaNoEscribe() {
      // Arrange
      User buyer = data.registerBuyer();
      User seller = data.registerSuperuser();
      Player player = data.createVisiblePlayer();
      data.buy(buyer, seller, player, 10, 100L, FIRST);
      PortfolioView first = portfolioService.portfolio(buyer.getEmail(), 0, 20, null);
      // Act
      PortfolioView second = portfolioService.portfolio(buyer.getEmail(), 0, 20, null);
      // Assert
      assertThat(second.summary()).isEqualTo(first.summary());
      assertThat(second.positions()).isEqualTo(first.positions());
    }
  }

  @Nested
  @DisplayName("Filtro, orden y paginación")
  class FiltroOrdenPaginacion {

    @Test
    @DisplayName("El filtro por estado devuelve solo las posiciones de ese estado")
    void filtraPorEstado() {
      // Arrange
      User buyer = data.registerBuyer();
      User seller = data.registerSuperuser();
      User other = data.registerBuyer();
      Player open = data.createVisiblePlayer();
      Player closed = data.createVisiblePlayer();
      data.buy(buyer, seller, open, 10, 100L, FIRST);
      data.buy(buyer, seller, closed, 10, 100L, FIRST);
      data.sell(buyer, other, closed, 10, 150L, SECOND);
      // Act
      PortfolioView opened = portfolioService.portfolio(buyer.getEmail(), 0, 20, "OPEN");
      PortfolioView closedView = portfolioService.portfolio(buyer.getEmail(), 0, 20, "CLOSED");
      // Assert
      assertThat(opened.positions()).hasSize(1);
      assertThat(opened.positions().get(0).playerId()).isEqualTo(open.getId());
      assertThat(closedView.positions()).hasSize(1);
      assertThat(closedView.positions().get(0).playerId()).isEqualTo(closed.getId());
      assertThat(opened.summary().totalPositions()).isEqualTo(1);
    }

    @Test
    @DisplayName("La paginación recorta la página y conserva los metadatos del conjunto filtrado")
    void paginaElConjuntoFiltrado() {
      // Arrange
      User buyer = data.registerBuyer();
      User seller = data.registerSuperuser();
      Player first = data.createVisiblePlayer();
      Player second = data.createVisiblePlayer();
      data.buy(buyer, seller, first, 10, 100L, FIRST);
      data.buy(buyer, seller, second, 20, 100L, FIRST);
      // Act
      PortfolioView view = portfolioService.portfolio(buyer.getEmail(), 0, 1, null);
      // Assert
      assertThat(view.positions()).hasSize(1);
      assertThat(view.pagination().page()).isZero();
      assertThat(view.pagination().size()).isEqualTo(1);
      assertThat(view.pagination().totalElements()).isEqualTo(2);
      assertThat(view.pagination().totalPages()).isEqualTo(2);
      assertThat(view.summary().totalTokens()).isEqualTo(30);
    }

    @Test
    @DisplayName("Una página superior a la última devuelve una página vacía y el resumen completo")
    void paginaFueraDeRango() {
      // Arrange
      User buyer = data.registerBuyer();
      User seller = data.registerSuperuser();
      Player player = data.createVisiblePlayer();
      data.buy(buyer, seller, player, 10, 100L, FIRST);
      // Act
      PortfolioView view = portfolioService.portfolio(buyer.getEmail(), 9, 20, null);
      // Assert
      assertThat(view.positions()).isEmpty();
      assertThat(view.pagination().totalElements()).isEqualTo(1);
      assertThat(view.summary().totalTokens()).isEqualTo(10);
    }

    @Test
    @DisplayName("El resumen no depende del filtro ni de la página")
    void resumenIndependienteDelFiltroYDeLaPagina() {
      // Arrange
      User buyer = data.registerBuyer();
      User seller = data.registerSuperuser();
      Player player = data.createVisiblePlayer();
      data.buy(buyer, seller, player, 10, 100L, FIRST);
      // Act
      PortfolioView all = portfolioService.portfolio(buyer.getEmail(), 0, 20, null);
      PortfolioView filtered = portfolioService.portfolio(buyer.getEmail(), 0, 20, "OPEN");
      // Assert
      assertThat(filtered.summary()).isEqualTo(all.summary());
    }

    @Test
    @DisplayName("Las posiciones abiertas sin cotización van después de las valoradas")
    void ordenaSinCotizacionDespuesDeValoradas() {
      // Arrange
      User buyer = data.registerBuyer();
      User seller = data.registerSuperuser();
      Player valued = data.createVisiblePlayer();
      Player unvalued = data.createVisiblePlayer();
      data.buy(buyer, seller, valued, 30, 100L, FIRST);
      data.buy(buyer, seller, unvalued, 10, 100L, FIRST);
      playerWithoutQuote = unvalued.getId();
      // Act
      PortfolioView view = portfolioService.portfolio(buyer.getEmail(), 0, 20, null);
      // Assert
      assertThat(view.positions()).hasSize(2);
      assertThat(view.positions().get(0).playerId()).isEqualTo(valued.getId());
      assertThat(view.positions().get(0).valuationStatus()).isEqualTo("VALUED");
      assertThat(view.positions().get(1).playerId()).isEqualTo(unvalued.getId());
      assertThat(view.positions().get(1).valuationStatus()).isEqualTo("UNVALUED");
    }

    @Test
    @DisplayName("Las posiciones cerradas se ordenan por nombre de jugador")
    void ordenaCerradasPorNombre() {
      // Arrange
      User buyer = data.registerBuyer();
      User seller = data.registerSuperuser();
      User other = data.registerBuyer();
      Player first = data.createVisiblePlayer();
      Player second = data.createVisiblePlayer();
      data.buy(buyer, seller, first, 10, 100L, FIRST);
      data.buy(buyer, seller, second, 10, 100L, FIRST);
      data.sell(buyer, other, first, 10, 150L, SECOND);
      data.sell(buyer, other, second, 10, 150L, SECOND);
      List<Long> expected =
          List.of(first, second).stream()
              .sorted(java.util.Comparator.comparing(Player::getName))
              .map(Player::getId)
              .toList();
      // Act
      PortfolioView view = portfolioService.portfolio(buyer.getEmail(), 0, 20, "CLOSED");
      // Assert
      assertThat(view.positions()).hasSize(2);
      assertThat(view.positions().get(0).playerId()).isEqualTo(expected.get(0));
      assertThat(view.positions().get(1).playerId()).isEqualTo(expected.get(1));
    }

    @Test
    @DisplayName("Ordena las posiciones abiertas valoradas por valor actual descendente")
    void ordenaPorValorActual() {
      // Arrange
      User buyer = data.registerBuyer();
      User seller = data.registerSuperuser();
      Player small = data.createVisiblePlayer();
      Player large = data.createVisiblePlayer();
      data.buy(buyer, seller, small, 10, 100L, FIRST);
      data.buy(buyer, seller, large, 30, 100L, FIRST);
      // Act
      PortfolioView view = portfolioService.portfolio(buyer.getEmail(), 0, 20, null);
      // Assert
      assertThat(view.positions().get(0).playerId()).isEqualTo(large.getId());
      assertThat(view.positions().get(1).playerId()).isEqualTo(small.getId());
    }
  }

  @Nested
  @DisplayName("Porcentajes propios")
  class Porcentajes {

    @Test
    @DisplayName("El rendimiento individual y el peso se calculan sobre el costo y el valor total")
    void calculaPorcentajes() {
      // Arrange
      User buyer = data.registerBuyer();
      User seller = data.registerSuperuser();
      Player player = data.createVisiblePlayer();
      data.buy(buyer, seller, player, 10, 50L, FIRST);
      // Act
      PortfolioView view = portfolioService.portfolio(buyer.getEmail(), 0, 20, null);
      // Assert
      PortfolioView.PortfolioPosition position = view.positions().get(0);
      assertThat(position.acquisitionCost()).isEqualTo(500L);
      assertThat(position.currentValue()).isEqualTo(1000L);
      assertThat(position.unrealizedProfitLoss()).isEqualTo(500L);
      assertThat(position.unrealizedProfitLossPercentage()).isEqualByComparingTo("100.00");
      assertThat(position.portfolioWeightPercentage()).isEqualByComparingTo("100.00");
    }

    @Test
    @DisplayName("Un denominador cero deja el porcentaje indefinido")
    void porcentajeIndefinido() {
      // Arrange
      User superuser = data.registerSuperuser();
      User buyer = data.registerBuyer();
      Player player = data.createVisiblePlayer();
      data.buy(buyer, superuser, player, 10, 100L, FIRST);
      // Act
      PortfolioView view = portfolioService.portfolio(superuser.getEmail(), 0, 100, null);
      // Assert: la posición se localiza por jugador porque el superusuario es un dato compartido
      PortfolioView.PortfolioPosition position =
          view.positions().stream()
              .filter(candidate -> candidate.playerId().equals(player.getId()))
              .findFirst()
              .orElseThrow();
      assertThat(position.acquisitionCost()).isZero();
      assertThat(position.unrealizedProfitLossPercentage()).isNull();
    }
  }

  @Nested
  @DisplayName("Validación de los parámetros de consulta")
  class Parametros {

    @Test
    @DisplayName("Una página o un tamaño inválidos se rechazan")
    void rechazaParametrosInvalidos() {
      // Arrange
      User buyer = data.registerBuyer();
      // Act / Assert
      assertThatThrownBy(() -> portfolioService.portfolio(buyer.getEmail(), -1, 20, null))
          .isInstanceOf(InvalidPortfolioPageException.class);
      assertThatThrownBy(() -> portfolioService.portfolio(buyer.getEmail(), 0, 0, null))
          .isInstanceOf(InvalidPortfolioPageException.class);
      assertThatThrownBy(() -> portfolioService.portfolio(buyer.getEmail(), 0, 101, null))
          .isInstanceOf(InvalidPortfolioPageException.class);
    }

    @Test
    @DisplayName("Un estado distinto de OPEN o CLOSED se rechaza")
    void rechazaEstadoDesconocido() {
      // Arrange
      User buyer = data.registerBuyer();
      // Act / Assert
      assertThatThrownBy(() -> portfolioService.portfolio(buyer.getEmail(), 0, 20, "open"))
          .isInstanceOf(InvalidPortfolioStatusException.class);
    }
  }
}
