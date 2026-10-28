package footballmarket.repositories;

import static org.assertj.core.api.Assertions.assertThat;

import footballmarket.features.auth.domain.User;
import footballmarket.features.catalog.domain.models.player.Player;
import footballmarket.features.market.domain.models.MarketTransaction;
import footballmarket.features.market.infraestructure.MarketTransactionRepository;
import footballmarket.support.OperationHistoryTestData;
import footballmarket.support.TestcontainersConfiguration;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
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
  @DisplayName("Consulta del historial personal")
  class Consulta {

    @Test
    @DisplayName("Incluye compras y ventas propias una vez y excluye operaciones ajenas")
    void incluyeSoloOperacionesPropias() {
      // Arrange
      User owner = data.registerUser();
      User counterparty = data.registerSuperuser();
      User other = data.registerUser();
      Player player = data.createPlayer();
      MarketTransaction buy = data.record(owner, counterparty, player, 4, 125L, FIRST);
      MarketTransaction sell = data.record(other, owner, player, 2, 150L, LATEST);
      data.record(other, counterparty, player, 1, 175L, LATEST);
      // Act
      List<MarketTransaction> result = transactions.findOperationHistory(owner.getId());
      // Assert
      assertThat(result)
          .extracting(MarketTransaction::getId)
          .containsExactly(sell.getId(), buy.getId());
    }

    @Test
    @DisplayName("Desempata por identificador descendente cuando coincide el instante")
    void desempataPorIdentificador() {
      // Arrange
      User owner = data.registerUser();
      User counterparty = data.registerSuperuser();
      Player player = data.createPlayer();
      MarketTransaction first = data.record(owner, counterparty, player, 1, 100L, LATEST);
      MarketTransaction second = data.record(owner, counterparty, player, 2, 100L, LATEST);
      // Act
      List<MarketTransaction> result = transactions.findOperationHistory(owner.getId());
      // Assert
      assertThat(result)
          .extracting(MarketTransaction::getId)
          .containsExactly(second.getId(), first.getId());
    }

    @Test
    @DisplayName("Repite la consulta sin alterar el orden ni duplicar transacciones")
    void repiteConsultaSinCambios() {
      // Arrange
      User owner = data.registerUser();
      User counterparty = data.registerSuperuser();
      Player player = data.createPlayer();
      MarketTransaction first = data.record(owner, counterparty, player, 1, 100L, FIRST);
      MarketTransaction latest = data.record(counterparty, owner, player, 2, 125L, LATEST);

      // Act
      List<MarketTransaction> initialResult = transactions.findOperationHistory(owner.getId());
      List<MarketTransaction> repeatedResult = transactions.findOperationHistory(owner.getId());

      // Assert
      assertThat(initialResult)
          .extracting(MarketTransaction::getId)
          .containsExactly(latest.getId(), first.getId());
      assertThat(repeatedResult)
          .extracting(MarketTransaction::getId)
          .containsExactly(latest.getId(), first.getId());
    }
  }
}
