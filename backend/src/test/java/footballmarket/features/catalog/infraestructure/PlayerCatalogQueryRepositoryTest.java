package footballmarket.features.catalog.infraestructure;

import static org.assertj.core.api.Assertions.assertThat;

import footballmarket.features.catalog.domain.models.player.Player;
import footballmarket.features.statistics.domain.enums.MetricSource;
import footballmarket.features.statistics.domain.enums.PlayerExtractionResult;
import footballmarket.support.PlayerCatalogQueryData;
import footballmarket.support.PlayerStatisticsQueryData;
import footballmarket.support.ResetPlayerCatalogListener;
import footballmarket.support.TestcontainersConfiguration;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.*;
import org.springframework.test.context.*;

@SpringBootTest
@ActiveProfiles("test")
@Import({
  TestcontainersConfiguration.class,
  PlayerCatalogQueryData.class,
  PlayerStatisticsQueryData.class
})
@TestExecutionListeners(
    listeners = ResetPlayerCatalogListener.class,
    mergeMode = TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS)
@Tag("integration")
class PlayerCatalogQueryRepositoryTest {
  @Autowired private PlayerRepository repository;
  @Autowired private PlayerCatalogQueryData data;
  @Autowired private PlayerStatisticsQueryData statistics;
  @Autowired private TeamRepository teams;

  private PlayerCatalogQueryData.Fixture eligiblePlayer(
      String name, String position, String country, Long teamId) {
    PlayerCatalogQueryData.Fixture player = data.player(name, position, country, teamId);
    statistics.row(
        player.playerId(),
        LocalDate.of(2026, 10, 1),
        MetricSource.JSON,
        "2026",
        "1",
        PlayerExtractionResult.COMPLETED,
        Map.of("goals_total", BigDecimal.ONE));
    return player;
  }

  @Nested
  @DisplayName("Filtros globales del catálogo")
  class Query {
    @Test
    @DisplayName("Excluye jugadores sin registros de métricas del listado y sus opciones globales")
    void excludesPlayersWithoutMetrics() {
      // Arrange
      PlayerCatalogQueryData.Fixture player =
          data.player("Sin métricas", "Goalkeeper", "Spain", null);

      // Act
      Page<Player> result =
          repository.findCatalog(null, null, null, null, null, PageRequest.of(0, 12));

      // Assert
      assertThat(result.getContent()).isEmpty();
      assertThat(result.getTotalElements()).isZero();
      assertThat(result.getTotalPages()).isZero();
      assertThat(repository.findByActiveTrue(PageRequest.of(0, 12)).getContent()).isEmpty();
      assertThat(repository.findCatalogLeagues()).isEmpty();
      assertThat(repository.findCatalogTeams()).isEmpty();
      assertThat(repository.findCatalogPositions()).isEmpty();
      assertThat(repository.findCatalogNationalities()).isEmpty();
      assertThat(repository.findVisibleById(player.playerId())).isEmpty();
    }

    @Test
    @DisplayName(
        "Usa la última fecha completa y admite cero informado en cualquiera de sus fuentes")
    void considersLatestDateAcrossSources() {
      // Arrange
      LocalDate previous = LocalDate.of(2026, 10, 1);
      LocalDate latest = previous.plusDays(1);
      PlayerCatalogQueryData.Fixture empty = data.player("Última vacía", "Forward", "France", null);
      PlayerCatalogQueryData.Fixture eligible =
          data.player("Cero informado", "Midfield", "Spain", null);
      statistics.row(
          empty.playerId(),
          previous,
          MetricSource.JSON,
          "2026",
          "1",
          PlayerExtractionResult.COMPLETED,
          Map.of("goals_total", BigDecimal.ONE));
      statistics.row(
          empty.playerId(),
          latest,
          MetricSource.JSON,
          "2026",
          "1",
          PlayerExtractionResult.COMPLETED,
          Map.of());
      statistics.row(
          eligible.playerId(),
          latest,
          MetricSource.JSON,
          "2026",
          "1",
          PlayerExtractionResult.COMPLETED,
          Map.of());
      statistics.row(
          eligible.playerId(),
          latest,
          MetricSource.SCRAPING,
          "2026",
          "1",
          PlayerExtractionResult.COMPLETED,
          Map.of("goals_total", BigDecimal.ZERO));

      // Act
      Page<Player> result =
          repository.findCatalog(null, null, null, null, null, PageRequest.of(0, 1));

      // Assert
      assertThat(result.getTotalElements()).isEqualTo(1);
      assertThat(result.getContent())
          .extracting(Player::getId)
          .containsExactly(eligible.playerId());
      assertThat(repository.findVisibleById(empty.playerId())).isEmpty();
      assertThat(repository.findVisibleById(eligible.playerId())).isPresent();
      assertThat(repository.findCatalogLeagues())
          .extracting(league -> league.getId())
          .containsExactly(eligible.leagueId());
      assertThat(repository.findCatalogTeams())
          .extracting(team -> team.getId())
          .containsExactly(eligible.teamId());
      assertThat(repository.findCatalogPositions()).containsExactly("Midfield");
      assertThat(repository.findCatalogNationalities()).containsExactly("Spain");
      assertThat(repository.findByActiveTrue(PageRequest.of(0, 1)).getTotalElements()).isEqualTo(1);
    }

    @Test
    @DisplayName("Las métricas no hacen visibles a jugadores inactivos o con equipo retirado")
    void retainsExistingVisibilityRules() {
      // Arrange
      PlayerCatalogQueryData.Fixture inactive =
          eligiblePlayer("Inactivo", "Forward", "France", null);
      PlayerCatalogQueryData.Fixture retired =
          eligiblePlayer("Retirado", "Midfield", "Spain", null);
      Player player = repository.findById(inactive.playerId()).orElseThrow();
      player.deactivate();
      repository.saveAndFlush(player);
      var team = teams.findById(retired.teamId()).orElseThrow();
      team.retire();
      teams.saveAndFlush(team);

      // Act / Assert
      assertThat(repository.findCatalog(null, null, null, null, null, PageRequest.of(0, 12)))
          .isEmpty();
      assertThat(repository.findCatalogLeagues()).isEmpty();
      assertThat(repository.findCatalogTeams()).isEmpty();
      assertThat(repository.findCatalogPositions()).isEmpty();
      assertThat(repository.findCatalogNationalities()).isEmpty();
    }

    @Test
    @DisplayName("Dos fuentes con métricas no duplican el jugador ni el conteo")
    void doesNotDuplicatePlayersWithTwoPopulatedSources() {
      // Arrange
      PlayerCatalogQueryData.Fixture player =
          eligiblePlayer("Dos fuentes", "Forward", "Spain", null);
      statistics.row(
          player.playerId(),
          LocalDate.of(2026, 10, 1),
          MetricSource.SCRAPING,
          "2026",
          "1",
          PlayerExtractionResult.COMPLETED,
          Map.of("goals_total", BigDecimal.ZERO));

      // Act
      Page<Player> page =
          repository.findCatalog(
              "fuentes",
              player.leagueId(),
              player.teamId(),
              "Forward",
              "Spain",
              PageRequest.of(0, 1));

      // Assert
      assertThat(page.getTotalElements()).isEqualTo(1);
      assertThat(page.getTotalPages()).isEqualTo(1);
      assertThat(page.getContent()).extracting(Player::getId).containsExactly(player.playerId());
    }

    @Test
    @DisplayName("El conteo global y el desempate por ID se conservan entre más de dos páginas")
    void pagesGlobalResults() {
      // Arrange
      PlayerCatalogQueryData.Fixture first = eligiblePlayer("Ana", "Forward", "Spain", null);
      for (int index = 0; index < 26; index++) {
        eligiblePlayer("Ana", "Forward", "Spain", first.teamId());
      }
      Sort sort = Sort.by(Sort.Order.asc("name").ignoreCase(), Sort.Order.asc("id"));
      // Act
      Page<Player> page =
          repository.findCatalog(
              "ANA", first.leagueId(), null, null, null, PageRequest.of(2, 12, sort));
      Page<Player> outside =
          repository.findCatalog(
              "ana", first.leagueId(), null, null, null, PageRequest.of(3, 12, sort));
      // Assert
      assertThat(page.getTotalElements()).isEqualTo(27);
      assertThat(page.getTotalPages()).isEqualTo(3);
      assertThat(page.getContent()).hasSize(3);
      assertThat(page.getContent()).extracting(Player::getId).isSorted();
      assertThat(outside.getContent()).isEmpty();
      assertThat(outside.getTotalElements()).isEqualTo(27);
    }

    @Test
    @DisplayName("Busca sin acentos y aplica todos los criterios antes del conteo y la página")
    void filtersBeforePagination() {
      // Arrange
      PlayerCatalogQueryData.Fixture fixture =
          eligiblePlayer("José Álvarez", "Midfield", "Spain", null);
      eligiblePlayer("Otro", "Forward", "France", null);
      eligiblePlayer("JOSÉ Segundo", "Midfield", "Spain", fixture.teamId());
      // Act
      Page<Player> result =
          repository.findCatalog(
              "jose",
              fixture.leagueId(),
              fixture.teamId(),
              "Midfield",
              "Spain",
              PageRequest.of(0, 1, Sort.by("id")));
      // Assert
      assertThat(result.getTotalElements()).isEqualTo(2);
      assertThat(result.getTotalPages()).isEqualTo(2);
      assertThat(result.getContent())
          .singleElement()
          .satisfies(player -> assertThat(player.getName()).isEqualTo("José Álvarez"));
      assertThat(
              repository
                  .findCatalog("alvarez", null, null, null, null, PageRequest.of(0, 12))
                  .getTotalElements())
          .isEqualTo(1);
    }

    @Test
    @DisplayName("Trata comodines de entrada como caracteres literales")
    void escapesLiteralWildcards() {
      // Arrange
      eligiblePlayer("Literal_%!", "Forward", null, null);
      eligiblePlayer("Literal ABC", "Forward", null, null);
      // Act
      Page<Player> result =
          repository.findCatalog("_%!", null, null, null, null, PageRequest.of(0, 12));
      // Assert
      assertThat(result.getTotalElements()).isEqualTo(1);
      assertThat(result.getContent())
          .singleElement()
          .satisfies(player -> assertThat(player.getName()).isEqualTo("Literal_%!"));
    }

    @Test
    @DisplayName("Un filtro incompatible devuelve vacío sin cambiar opciones globales")
    void preservesGlobalOptions() {
      // Arrange
      PlayerCatalogQueryData.Fixture first = eligiblePlayer("Primero", "Goalkeeper", "Spain", null);
      PlayerCatalogQueryData.Fixture second = eligiblePlayer("Segundo", "Forward", "France", null);
      // Act
      Page<Player> result =
          repository.findCatalog(
              null, first.leagueId(), second.teamId(), null, null, PageRequest.of(0, 12));
      // Assert
      assertThat(result).isEmpty();
      assertThat(repository.findCatalogTeams()).hasSize(2);
      assertThat(repository.findCatalogNationalities())
          .containsExactlyInAnyOrder("Spain", "France");
    }
  }
}
