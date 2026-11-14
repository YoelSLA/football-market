package footballmarket.features.portfolio.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import footballmarket.features.auth.application.exceptions.CurrentUserNotFoundException;
import footballmarket.features.auth.domain.User;
import footballmarket.features.catalog.domain.models.player.Player;
import footballmarket.features.market.domain.models.MarketTransaction;
import footballmarket.features.portfolio.domain.records.OperationHistoryEntry;
import footballmarket.features.portfolio.domain.records.OperationHistoryEntry.OperationType;
import footballmarket.features.portfolio.domain.records.OperationHistoryEntry.PlayerData;
import footballmarket.features.portfolio.domain.records.OperationHistoryEntry.RelatedData;
import footballmarket.features.portfolio.domain.records.OperationHistoryView;
import footballmarket.features.portfolio.presentation.exceptions.InvalidOperationHistoryCriteriaException;
import footballmarket.features.portfolio.presentation.exceptions.InvalidOperationHistoryDateRangeException;
import footballmarket.features.statistics.domain.enums.MetricSource;
import footballmarket.features.statistics.domain.enums.PlayerExtractionResult;
import footballmarket.support.OperationHistoryTestData;
import footballmarket.support.PlayerStatisticsQueryData;
import footballmarket.support.TestcontainersConfiguration;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Map;
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
  @Autowired private PlayerStatisticsQueryData statistics;

  @Nested
  @DisplayName("Consulta paginada")
  class ConsultaPaginada {

    @Test
    @DisplayName("Conserva las operaciones propias cuando el jugador deja de tener métricas")
    void conservaOperacionesSinMetricasRecientes() {
      // Arrange
      User owner = data.registerUser();
      User superuser = data.registerSuperuser();
      User other = data.registerUser();
      Player player = data.createPlayer();
      LocalDate date = LocalDate.of(2026, 10, 20);
      statistics.row(
          player.getId(),
          date,
          MetricSource.JSON,
          "2026",
          "1",
          PlayerExtractionResult.COMPLETED,
          Map.of("goals_total", BigDecimal.ONE));
      MarketTransaction own = data.record(owner, superuser, player, 4, 125L, BUY_TIME);
      data.record(other, superuser, player, 1, 100L, BUY_TIME);
      statistics.row(
          player.getId(),
          date.plusDays(1),
          MetricSource.JSON,
          "2026",
          "1",
          PlayerExtractionResult.COMPLETED,
          Map.of());

      // Act / Assert
      assertThat(
              historyService
                  .history(owner.getEmail(), 0, 20, null, null, null, null, "NEWEST")
                  .transactions())
          .containsExactly(
              new OperationHistoryEntry(
                  own.getId(),
                  OperationType.BUY,
                  new PlayerData(
                      player.getId(),
                      player.getName(),
                      player.getImageUrl(),
                      player.getFallbackImageUrl(),
                      new RelatedData(player.getTeam().getId(), player.getTeam().getName()),
                      new RelatedData(player.getLeague().getId(), player.getLeague().getName())),
                  4,
                  125L,
                  500L,
                  BUY_TIME));
    }

    @Test
    @DisplayName("Interpreta roles, conserva valores y devuelve el perfil actual")
    void interpretaRolesValoresYPerfil() {
      // Arrange
      User owner = data.registerUser();
      User superuser = data.registerSuperuser();
      User buyer = data.registerUser();
      Player player = data.createPlayer();
      data.record(owner, superuser, player, 4, 125L, BUY_TIME);
      MarketTransaction sell = data.record(buyer, owner, player, 2, 150L, SELL_TIME);

      // Act
      OperationHistoryView result = history(owner.getEmail(), 0, 20, null, null, "NEWEST");

      // Assert
      assertThat(result.summary())
          .isEqualTo(new OperationHistoryView.Summary(2, 1, 1, 4, 2, 500, 300));
      assertThat(result.pagination().totalElements()).isEqualTo(2);
      assertThat(result.transactions()).hasSize(2);
      OperationHistoryEntry first = result.transactions().get(0);
      assertThat(first.transactionId()).isEqualTo(sell.getId());
      assertThat(first.type()).isEqualTo(OperationType.SELL);
      assertThat(first.player().id()).isEqualTo(player.getId());
      assertThat(first.player().name()).isEqualTo(player.getName());
      assertThat(first.player().team().id()).isEqualTo(player.getTeam().getId());
      assertThat(first.player().league().id()).isEqualTo(player.getLeague().getId());
      assertThat(first.totalAmount()).isEqualTo(300L);
    }

    @Test
    @DisplayName("Aplica tipo, búsqueda, fechas, orden y paginación al mismo conjunto")
    void aplicaCriterios() {
      // Arrange
      User owner = data.registerUser();
      User superuser = data.registerSuperuser();
      Player player = data.createPlayer();
      data.record(owner, superuser, player, 1, 100L, BUY_TIME);
      data.record(owner, superuser, player, 2, 200L, SELL_TIME);

      // Act
      OperationHistoryView result =
          history(
              owner.getEmail(),
              0,
              1,
              player.getName(),
              "BUY",
              BUY_TIME.minusSeconds(1),
              SELL_TIME.plusSeconds(1),
              "AMOUNT_DESC");

      // Assert
      assertThat(result.summary().totalTransactions()).isEqualTo(2);
      assertThat(result.summary().buyTransactions()).isEqualTo(2);
      assertThat(result.transactions()).hasSize(1);
      assertThat(result.transactions().get(0).totalAmount()).isEqualTo(400L);
      assertThat(result.pagination().totalPages()).isEqualTo(2);
    }

    @Test
    @DisplayName("Devuelve resumen y página vacíos cuando no existen operaciones")
    void devuelveHistorialVacio() {
      // Arrange
      User owner = data.registerUser();

      // Act
      OperationHistoryView result = history(owner.getEmail(), 0, 20, null, null, "NEWEST");

      // Assert
      assertThat(result.summary().totalTransactions()).isZero();
      assertThat(result.transactions()).isEmpty();
      assertThat(result.pagination().totalElements()).isZero();
    }
  }

  @Nested
  @DisplayName("Validación de criterios")
  class Validacion {

    @Test
    @DisplayName("Rechaza paginación, tipo y orden inválidos")
    void rechazaCriteriosInvalidos() {
      // Arrange
      User owner = data.registerUser();

      // Act / Assert
      assertThatThrownBy(() -> history(owner.getEmail(), -1, 20, null, null, "NEWEST"))
          .isInstanceOf(InvalidOperationHistoryCriteriaException.class);
      assertThatThrownBy(() -> history(owner.getEmail(), 0, 20, null, "OTHER", "NEWEST"))
          .isInstanceOf(InvalidOperationHistoryCriteriaException.class);
      assertThatThrownBy(() -> history(owner.getEmail(), 0, 20, null, null, "OTHER"))
          .isInstanceOf(InvalidOperationHistoryCriteriaException.class);
    }

    @Test
    @DisplayName("Rechaza un rango temporal vacío o invertido")
    void rechazaRangoInvalido() {
      // Arrange
      User owner = data.registerUser();

      // Act / Assert
      assertThatThrownBy(
              () ->
                  historyService.history(
                      owner.getEmail(), 0, 20, null, null, SELL_TIME, BUY_TIME, "NEWEST"))
          .isInstanceOf(InvalidOperationHistoryDateRangeException.class);
    }

    @Test
    @DisplayName("Rechaza un subject que no corresponde a un usuario persistido")
    void rechazaSubjectSinUsuario() {
      // Act / Assert
      assertThatThrownBy(() -> history("missing@example.com", 0, 20, null, null, "NEWEST"))
          .isInstanceOf(CurrentUserNotFoundException.class);
    }
  }

  private OperationHistoryView history(
      String email, int page, int size, String search, String type, String sort) {
    return history(email, page, size, search, type, null, null, sort);
  }

  private OperationHistoryView history(
      String email,
      int page,
      int size,
      String search,
      String type,
      OffsetDateTime from,
      OffsetDateTime before,
      String sort) {
    return historyService.history(email, page, size, search, type, from, before, sort);
  }
}
