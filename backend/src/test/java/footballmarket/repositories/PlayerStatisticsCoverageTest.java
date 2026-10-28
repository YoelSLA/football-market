package footballmarket.repositories;

import static org.assertj.core.api.Assertions.assertThat;

import footballmarket.features.catalog.domain.enums.ExternalProvider;
import footballmarket.features.catalog.domain.models.league.League;
import footballmarket.features.catalog.domain.models.player.Player;
import footballmarket.features.catalog.domain.models.team.Team;
import footballmarket.features.catalog.infraestructure.LeagueRepository;
import footballmarket.features.catalog.infraestructure.PlayerRepository;
import footballmarket.features.catalog.infraestructure.TeamRepository;
import footballmarket.features.statistics.application.internal.PlayerStatisticsCoverageQuery;
import footballmarket.features.statistics.application.internal.PlayerStatisticsEligibilitySelector;
import footballmarket.features.statistics.domain.enums.MetricSource;
import footballmarket.features.statistics.domain.enums.PlayerExtractionResult;
import footballmarket.features.statistics.domain.models.PlayerStatisticsExtractionRun;
import footballmarket.features.statistics.domain.models.PlayerStatisticsRunItem;
import footballmarket.features.statistics.infraestructure.PlayerStatisticsExtractionRunRepository;
import footballmarket.features.statistics.infraestructure.PlayerStatisticsRunItemRepository;
import footballmarket.support.ResetPlayerCatalogListener;
import footballmarket.support.TestcontainersConfiguration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/** Elegibilidad, exclusión por falta de referencia y pendientes ascendentes de la campaña. */
@SpringBootTest
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@TestExecutionListeners(
    listeners = ResetPlayerCatalogListener.class,
    mergeMode = TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS)
@org.junit.jupiter.api.Tag("integration")
class PlayerStatisticsCoverageTest {
  private static final LocalDate DATE = LocalDate.of(2026, 10, 15);
  private static final Instant NOW = Instant.parse("2026-10-15T12:00:00Z");
  @Autowired private LeagueRepository leagues;
  @Autowired private TeamRepository teams;
  @Autowired private PlayerRepository players;
  @Autowired private PlayerStatisticsExtractionRunRepository runs;
  @Autowired private PlayerStatisticsRunItemRepository items;
  @Autowired private PlayerStatisticsEligibilitySelector selector;
  @Autowired private PlayerStatisticsCoverageQuery coverage;
  @Autowired private PlatformTransactionManager manager;

  private Long playerWithReference(String externalId) {
    return new TransactionTemplate(manager)
        .execute(
            status -> {
              League league = leagues.save(new League("Coverage League"));
              Team team = teams.save(new Team("Coverage Team", league, true));
              Player player = new Player("Coverage Player", team, "Forward");
              player.addExternalReference(ExternalProvider.WHOSCORED, externalId);
              return players.saveAndFlush(player).getId();
            });
  }

  private Long playerWithoutReference() {
    return new TransactionTemplate(manager)
        .execute(
            status -> {
              League league = leagues.save(new League("Coverage League"));
              Team team = teams.save(new Team("Coverage Team", league, true));
              return players.saveAndFlush(new Player("Coverage Player", team, "Forward")).getId();
            });
  }

  /**
   * Registra los resultados indicados dentro de una sola ejecución del canal: la restricción de
   * esquema admite un único run activo por fuente.
   */
  private void confirmAll(
      List<Long> playerIds, MetricSource source, List<PlayerExtractionResult> results) {
    new TransactionTemplate(manager)
        .executeWithoutResult(
            status -> {
              PlayerStatisticsExtractionRun run =
                  runs.saveAndFlush(new PlayerStatisticsExtractionRun(DATE, source, NOW, null));
              for (int index = 0; index < playerIds.size(); index++) {
                PlayerStatisticsRunItem item =
                    new PlayerStatisticsRunItem(
                        run, players.findById(playerIds.get(index)).orElseThrow(), "100", NOW);
                item.finish(results.get(index), NOW.plusSeconds(1), 0, 0, 0, null);
                items.saveAndFlush(item);
              }
            });
  }

  private void confirm(Long playerId, MetricSource source, PlayerExtractionResult result) {
    confirmAll(List.of(playerId), source, List.of(result));
  }

  @Nested
  @DisplayName("Elegibilidad de la población")
  class Eligibility {
    @Test
    @DisplayName(
        "Solo los jugadores con referencia confirmada son elegibles y el resto se cuenta como exclusión")
    void cuentaElegiblesYExcluidos() {
      // Arrange
      playerWithReference("100");
      playerWithReference("101");
      playerWithoutReference();

      // Act
      PlayerStatisticsEligibilitySelector.Population population = selector.population();

      // Assert
      assertThat(population.catalogPlayers()).isEqualTo(3);
      assertThat(population.eligiblePlayers()).isEqualTo(2);
      assertThat(population.excludedWithoutReference()).isEqualTo(1);
    }
  }

  @Nested
  @DisplayName("Selección de pendientes")
  class Selection {
    @Test
    @DisplayName("Los pendientes se seleccionan por identificador local ascendente")
    void ordenAscendente() {
      // Arrange
      Long primero = playerWithReference("100");
      Long segundo = playerWithReference("101");
      Long tercero = playerWithReference("102");

      // Act
      List<Long> selected = selector.pendingPlayerIds(DATE, MetricSource.SCRAPING, 10);

      // Assert
      assertThat(selected).containsExactly(primero, segundo, tercero);
    }

    @Test
    @DisplayName("Un jugador sin referencia confirmada nunca se selecciona")
    void excluyeSinReferencia() {
      // Arrange
      playerWithReference("100");
      playerWithoutReference();

      // Act
      List<Long> selected = selector.pendingPlayerIds(DATE, MetricSource.SCRAPING, 10);

      // Assert
      assertThat(selected).hasSize(1);
    }

    @Test
    @DisplayName("Los confirmados de la campaña no se repiten y los fallidos siguen pendientes")
    void excluyeConfirmadosYConservaFallidos() {
      // Arrange
      Long confirmado = playerWithReference("100");
      Long fallido = playerWithReference("101");
      Long noIntentado = playerWithReference("102");
      confirmAll(
          List.of(confirmado, fallido),
          MetricSource.SCRAPING,
          List.of(PlayerExtractionResult.COMPLETED, PlayerExtractionResult.PROVIDER_ERROR));

      // Act
      List<Long> selected = selector.pendingPlayerIds(DATE, MetricSource.SCRAPING, 10);

      // Assert
      assertThat(selected).containsExactly(fallido, noIntentado);
    }

    @Test
    @DisplayName("Confirmar otro canal o la fecha siguiente no completa la campaña de scraping")
    void campañasIndependientes() {
      // Arrange
      Long playerId = playerWithReference("100");
      confirm(playerId, MetricSource.JSON, PlayerExtractionResult.COMPLETED);

      // Act
      List<Long> selected = selector.pendingPlayerIds(DATE, MetricSource.SCRAPING, 10);

      // Assert
      assertThat(selected).containsExactly(playerId);
    }

    @Test
    @DisplayName("El máximo limita la cantidad total seleccionada sin multiplicarla por worker")
    void limitaPorMaxPlayers() {
      // Arrange
      playerWithReference("100");
      playerWithReference("101");
      playerWithReference("102");

      // Act
      List<Long> selected = selector.pendingPlayerIds(DATE, MetricSource.SCRAPING, 1);

      // Assert
      assertThat(selected).hasSize(1);
    }
  }

  @Nested
  @DisplayName("Cobertura de la campaña")
  class Coverage {
    @Test
    @DisplayName("Una fecha sin ejecuciones devuelve el catálogo vigente con cero completados")
    void fechaSinEjecuciones() {
      // Arrange
      playerWithReference("100");
      playerWithoutReference();

      // Act
      var result = coverage.coverage(DATE, MetricSource.SCRAPING);

      // Assert
      assertThat(result.catalogPlayers()).isEqualTo(2);
      assertThat(result.eligiblePlayers()).isEqualTo(1);
      assertThat(result.completedPlayers()).isZero();
      assertThat(result.pendingPlayers()).isEqualTo(1);
      assertThat(result.failedPlayers()).isZero();
      assertThat(result.coverageComplete()).isFalse();
      assertThat(result.lastRunId()).isNull();
      assertThat(result.lastRunFinishedAt()).isNull();
    }

    @Test
    @DisplayName("Los fallidos permanecen incluidos en los pendientes hasta su confirmación")
    void fallidosIncluidosEnPendientes() {
      // Arrange
      Long confirmado = playerWithReference("100");
      Long fallido = playerWithReference("101");
      confirmAll(
          List.of(confirmado, fallido),
          MetricSource.SCRAPING,
          List.of(PlayerExtractionResult.COMPLETED, PlayerExtractionResult.PROVIDER_ERROR));

      // Act
      var result = coverage.coverage(DATE, MetricSource.SCRAPING);

      // Assert
      assertThat(result.completedPlayers()).isEqualTo(1);
      assertThat(result.failedPlayers()).isEqualTo(1);
      assertThat(result.pendingPlayers()).isEqualTo(1);
      assertThat(result.coverageComplete()).isFalse();
    }

    @Test
    @DisplayName("La cobertura es completa solo cuando no quedan elegibles pendientes")
    void coberturaCompleta() {
      // Arrange
      Long playerId = playerWithReference("100");
      confirm(playerId, MetricSource.SCRAPING, PlayerExtractionResult.COMPLETED);

      // Act
      var result = coverage.coverage(DATE, MetricSource.SCRAPING);

      // Assert
      assertThat(result.coverageComplete()).isTrue();
      assertThat(result.pendingPlayers()).isZero();
    }
  }
}
