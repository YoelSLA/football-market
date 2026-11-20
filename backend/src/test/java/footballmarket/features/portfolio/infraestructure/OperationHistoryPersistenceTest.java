package footballmarket.features.portfolio.infraestructure;

import static org.assertj.core.api.Assertions.assertThat;

import footballmarket.features.auth.domain.User;
import footballmarket.features.catalog.domain.models.player.Player;
import footballmarket.features.market.domain.models.MarketTransaction;
import footballmarket.features.market.infraestructure.MarketTransactionRepository;
import footballmarket.features.portfolio.domain.records.OperationHistoryAggregate;
import footballmarket.support.OperationHistoryTestData;
import footballmarket.support.TestcontainersConfiguration;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;

/** Persistencia real de la consulta dedicada del historial personal. */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
@org.junit.jupiter.api.Tag("integration")
class OperationHistoryPersistenceTest {

  private static final OffsetDateTime FIRST = OffsetDateTime.parse("2026-10-20T10:00:00Z");
  private static final OffsetDateTime LATEST = OffsetDateTime.parse("2026-10-21T10:00:00Z");

  @Autowired private MarketTransactionRepository transactions;
  @Autowired private OperationHistoryTestData data;

  @Nested
  @DisplayName("Página y agregados")
  class PaginaYAgregados {

    @Test
    @DisplayName("Pagina solo operaciones propias con orden estable y relaciones cargadas")
    void paginaOperacionesPropias() {
      // Arrange
      User owner = data.registerUser();
      User counterparty = data.registerSuperuser();
      User other = data.registerUser();
      Player player = data.createPlayer();
      MarketTransaction buy = data.record(owner, counterparty, player, 4, 125L, FIRST);
      MarketTransaction sell = data.record(other, owner, player, 2, 150L, LATEST);
      data.record(other, counterparty, player, 1, 175L, LATEST);
      PageRequest pageable =
          PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "executedAt", "id"));

      // Act
      Page<MarketTransaction> result =
          transactions.findOperationHistory(
              owner.getId(), null, null, false, FIRST, false, FIRST, pageable);

      // Assert
      assertThat(result.getContent())
          .extracting(MarketTransaction::getId)
          .containsExactly(sell.getId(), buy.getId());
      assertThat(result.getContent().get(0).getPlayer().getTeam().getLeague().getName())
          .isNotBlank();
    }

    @Test
    @DisplayName("Aplica filtros idénticos a contenido y agregados")
    void filtraContenidoYAgregados() {
      // Arrange
      User owner = data.registerUser();
      User counterparty = data.registerSuperuser();
      User other = data.registerUser();
      Player player = data.createPlayer();
      data.record(owner, counterparty, player, 4, 125L, FIRST);
      data.record(other, owner, player, 2, 150L, LATEST);
      PageRequest pageable =
          PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "executedAt", "id"));

      // Act
      Page<MarketTransaction> page =
          transactions.findOperationHistory(
              owner.getId(),
              player.getName(),
              "BUY",
              true,
              FIRST.minusSeconds(1),
              true,
              LATEST,
              pageable);
      OperationHistoryAggregate aggregate =
          transactions.aggregateOperationHistory(
              owner.getId(), player.getName(), "BUY", true, FIRST.minusSeconds(1), true, LATEST);

      // Assert
      assertThat(page.getTotalElements()).isEqualTo(1);
      assertThat(aggregate).isEqualTo(new OperationHistoryAggregate(1, 1, 0, 4, 0, 500, 0));
    }

    @Test
    @DisplayName("Devuelve una página concreta sin duplicar filas")
    void paginaSinDuplicados() {
      // Arrange
      User owner = data.registerUser();
      User counterparty = data.registerSuperuser();
      Player player = data.createPlayer();
      MarketTransaction first = data.record(owner, counterparty, player, 1, 100L, LATEST);
      MarketTransaction second = data.record(owner, counterparty, player, 2, 100L, LATEST);
      PageRequest pageable = PageRequest.of(1, 1, Sort.by(Sort.Direction.DESC, "executedAt", "id"));

      // Act
      Page<MarketTransaction> result =
          transactions.findOperationHistory(
              owner.getId(), null, null, false, FIRST, false, FIRST, pageable);

      // Assert
      assertThat(result.getTotalElements()).isEqualTo(2);
      assertThat(result.getContent())
          .extracting(MarketTransaction::getId)
          .containsExactly(first.getId());
      assertThat(second.getId()).isGreaterThan(first.getId());
    }
  }
}
