package footballmarket.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import footballmarket.models.MarketTransaction;
import footballmarket.models.Player;
import footballmarket.models.User;
import footballmarket.models.records.OperationHistoryEntry;
import footballmarket.models.records.OperationHistoryEntry.OperationType;
import footballmarket.services.exceptions.CurrentUserNotFoundException;
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

/** Caso de uso real del historial personal sobre PostgreSQL. */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
@org.junit.jupiter.api.Tag("integration")
class OperationHistoryServiceTest {

  private static final OffsetDateTime BUY_TIME = OffsetDateTime.parse("2026-10-20T10:00:00Z");
  private static final OffsetDateTime SELL_TIME = OffsetDateTime.parse("2026-10-21T10:00:00Z");

  @Autowired private OperationHistoryService historyService;
  @Autowired private OperationHistoryTestData data;

  @Nested
  @DisplayName("Interpretación del historial")
  class Interpretacion {

    @Test
    @DisplayName("Interpreta compra y venta y conserva todos los valores históricos")
    void interpretaRolesYValores() {
      // Arrange
      User owner = data.registerUser();
      User superuser = data.registerSuperuser();
      User buyer = data.registerUser();
      Player player = data.createPlayer();
      MarketTransaction buy = data.record(owner, superuser, player, 4, 125L, BUY_TIME);
      MarketTransaction sell = data.record(buyer, owner, player, 2, 150L, SELL_TIME);
      // Act
      List<OperationHistoryEntry> result = historyService.history(owner.getEmail().toUpperCase());
      // Assert
      assertThat(result).hasSize(2);
      assertThat(result.get(0))
          .isEqualTo(
              new OperationHistoryEntry(
                  sell.getId(),
                  OperationType.SELL,
                  player.getId(),
                  player.getName(),
                  2,
                  150L,
                  300L,
                  SELL_TIME));
      assertThat(result.get(1))
          .isEqualTo(
              new OperationHistoryEntry(
                  buy.getId(),
                  OperationType.BUY,
                  player.getId(),
                  player.getName(),
                  4,
                  125L,
                  500L,
                  BUY_TIME));
    }

    @Test
    @DisplayName("Excluye las transacciones de otros usuarios")
    void excluyeOperacionesAjenas() {
      // Arrange
      User owner = data.registerUser();
      User other = data.registerUser();
      User superuser = data.registerSuperuser();
      Player player = data.createPlayer();
      data.record(other, superuser, player, 1, 100L, BUY_TIME);
      // Act
      List<OperationHistoryEntry> result = historyService.history(owner.getEmail());
      // Assert
      assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Devuelve una colección vacía cuando el usuario no tiene operaciones")
    void devuelveHistorialVacio() {
      // Arrange
      User owner = data.registerUser();

      // Act
      List<OperationHistoryEntry> result = historyService.history(owner.getEmail());

      // Assert
      assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Rechaza un subject que no corresponde a un usuario persistido")
    void rechazaSubjectSinUsuario() {
      // Act / Assert
      assertThatThrownBy(() -> historyService.history("missing@example.com"))
          .isInstanceOf(CurrentUserNotFoundException.class)
          .hasMessage("La identidad de la sesión no es válida.");
    }
  }
}
