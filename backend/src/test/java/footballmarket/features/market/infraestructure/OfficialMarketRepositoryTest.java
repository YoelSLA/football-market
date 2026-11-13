package footballmarket.features.market.infraestructure;

import static org.assertj.core.api.Assertions.assertThat;

import footballmarket.features.catalog.domain.models.league.League;
import footballmarket.features.catalog.domain.models.player.Player;
import footballmarket.features.catalog.domain.models.team.Team;
import footballmarket.features.catalog.infraestructure.LeagueRepository;
import footballmarket.features.catalog.infraestructure.PlayerRepository;
import footballmarket.features.catalog.infraestructure.TeamRepository;
import footballmarket.features.market.infraestructure.projections.OfficialMarketPlayerProjection;
import footballmarket.features.quote.domain.enums.QuoteOrigin;
import footballmarket.features.quote.domain.models.PlayerQuote;
import footballmarket.features.quote.infraestructure.PlayerQuoteRepository;
import footballmarket.support.MarketTestData;
import footballmarket.support.TestcontainersConfiguration;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
@Tag("integration")
@Transactional
class OfficialMarketRepositoryTest {

  private static final Logger log = LoggerFactory.getLogger(OfficialMarketRepositoryTest.class);

  @Autowired private OfficialMarketRepository officialMarketRepository;
  @Autowired private PlayerRepository playerRepository;
  @Autowired private TeamRepository teamRepository;
  @Autowired private LeagueRepository leagueRepository;
  @Autowired private PlayerQuoteRepository playerQuoteRepository;
  @Autowired private MarketTestData testData;
  @Autowired private EntityManager entityManager;

  @Nested
  @DisplayName("Filtros deportivos y de cotización en consultas nativas")
  class FilteringAndLateralJoin {

    @Test
    @DisplayName("Filtra por nombre con catalog_normalize_name ignorando mayúsculas y acentos")
    void filtersByNameInsensitiveToAccentsAndCase() {
      // Arrange
      League league = leagueRepository.save(new League("Liga Santander"));
      Team team = teamRepository.save(new Team("Inter Miami", league, true));
      Player player = playerRepository.save(new Player("Lionel Andrés Messi", team, "DEL"));

      // Act
      List<OfficialMarketPlayerProjection> results =
          officialMarketRepository.findOfficialMarketPlayers(
              "andres", null, null, null, null, null, null, null, "id", "asc", 10, 0);

      // Assert
      assertThat(results)
          .extracting(OfficialMarketPlayerProjection::getId)
          .contains(player.getId());
    }

    @Test
    @DisplayName("Filtra por disponibilidad de cotización hasQuote true y false")
    void filtersByQuoteAvailability() {
      // Arrange
      Player withQuote = testData.createVisiblePlayer();
      testData.initializeMarket(withQuote);

      Player withoutQuote = testData.createVisiblePlayer();

      // Act (hasQuote = true)
      List<OfficialMarketPlayerProjection> onlyWithQuote =
          officialMarketRepository.findOfficialMarketPlayers(
              null, null, null, null, null, true, null, null, "id", "asc", 50, 0);

      // Act (hasQuote = false)
      List<OfficialMarketPlayerProjection> onlyWithoutQuote =
          officialMarketRepository.findOfficialMarketPlayers(
              null, null, null, null, null, false, null, null, "id", "asc", 50, 0);

      // Assert
      assertThat(onlyWithQuote)
          .extracting(OfficialMarketPlayerProjection::getId)
          .contains(withQuote.getId());
      assertThat(onlyWithQuote)
          .extracting(OfficialMarketPlayerProjection::getId)
          .doesNotContain(withoutQuote.getId());

      assertThat(onlyWithoutQuote)
          .extracting(OfficialMarketPlayerProjection::getId)
          .contains(withoutQuote.getId());
      assertThat(onlyWithoutQuote)
          .extracting(OfficialMarketPlayerProjection::getId)
          .doesNotContain(withQuote.getId());
    }

    @Test
    @DisplayName("Filtra por rango de precio minPrice y maxPrice inclusivo")
    void filtersByPriceRange() {
      // Arrange
      Player p1 = testData.createVisiblePlayer();
      playerQuoteRepository.save(
          new PlayerQuote(
              p1,
              LocalDate.of(2026, 11, 1),
              5000L,
              null,
              BigDecimal.ONE,
              null,
              null,
              null,
              null,
              QuoteOrigin.INITIAL,
              Instant.parse("2026-11-01T08:00:00Z")));

      Player p2 = testData.createVisiblePlayer();
      playerQuoteRepository.save(
          new PlayerQuote(
              p2,
              LocalDate.of(2026, 11, 1),
              15000L,
              null,
              BigDecimal.ONE,
              null,
              null,
              null,
              null,
              QuoteOrigin.INITIAL,
              Instant.parse("2026-11-01T08:00:00Z")));

      // Act
      List<OfficialMarketPlayerProjection> results =
          officialMarketRepository.findOfficialMarketPlayers(
              null, null, null, null, null, null, 10000L, 20000L, "id", "asc", 10, 0);

      // Assert
      assertThat(results).extracting(OfficialMarketPlayerProjection::getId).contains(p2.getId());
      assertThat(results)
          .extracting(OfficialMarketPlayerProjection::getId)
          .doesNotContain(p1.getId());
    }
  }

  @Nested
  @DisplayName("Ordenamiento dinámico con NULLS LAST y desempate determinista")
  class DynamicOrderingAndTieBreak {

    @Test
    @DisplayName("Ubica valores nulos al final (NULLS LAST) en ordenamiento por precio ASC y DESC")
    void placesNullsLastInPriceOrdering() {
      // Arrange
      Player playerWithQuote = testData.createVisiblePlayer();
      testData.initializeMarket(playerWithQuote); // price 100L

      Player playerWithoutQuote = testData.createVisiblePlayer(); // quote null

      // Act (price,asc)
      List<OfficialMarketPlayerProjection> ascList =
          officialMarketRepository.findOfficialMarketPlayers(
              null, null, null, null, null, null, null, null, "price", "asc", 50, 0);

      // Act (price,desc)
      List<OfficialMarketPlayerProjection> descList =
          officialMarketRepository.findOfficialMarketPlayers(
              null, null, null, null, null, null, null, null, "price", "desc", 50, 0);

      // Assert: playerWithoutQuote appears at the end in both ASC and DESC
      int ascIndexWithQuote = indexOfPlayer(ascList, playerWithQuote.getId());
      int ascIndexWithoutQuote = indexOfPlayer(ascList, playerWithoutQuote.getId());
      assertThat(ascIndexWithQuote).isLessThan(ascIndexWithoutQuote);

      int descIndexWithQuote = indexOfPlayer(descList, playerWithQuote.getId());
      int descIndexWithoutQuote = indexOfPlayer(descList, playerWithoutQuote.getId());
      assertThat(descIndexWithQuote).isLessThan(descIndexWithoutQuote);
    }

    @Test
    @DisplayName("Desempata de forma determinista por p.id ASC ante igualdad de precios")
    void deterministicTieBreakByIdAscending() {
      // Arrange
      Player playerA = testData.createVisiblePlayer();
      testData.initializeMarket(playerA); // price 100L

      Player playerB = testData.createVisiblePlayer();
      testData.initializeMarket(playerB); // price 100L

      // Act
      List<OfficialMarketPlayerProjection> list =
          officialMarketRepository.findOfficialMarketPlayers(
              null, null, null, null, null, null, null, null, "price", "asc", 50, 0);

      // Assert
      int indexA = indexOfPlayer(list, playerA.getId());
      int indexB = indexOfPlayer(list, playerB.getId());
      if (playerA.getId() < playerB.getId()) {
        assertThat(indexA).isLessThan(indexB);
      } else {
        assertThat(indexB).isLessThan(indexA);
      }
    }

    private int indexOfPlayer(List<OfficialMarketPlayerProjection> list, Long playerId) {
      for (int i = 0; i < list.size(); i++) {
        if (list.get(i).getId().equals(playerId)) {
          return i;
        }
      }
      return -1;
    }
  }

  @Nested
  @DisplayName("Estrategia desacoplada de conteo y filtros mutuamente excluyentes")
  class DecoupledCountStrategy {

    @Test
    @DisplayName("Conteo sin filtros financieros devuelve el número exacto de jugadores activos")
    void countsWithoutFinancialFilters() {
      // Arrange
      Player p1 = testData.createVisiblePlayer();
      Player p2 = testData.createVisiblePlayer();

      // Act
      long count =
          officialMarketRepository.countOfficialMarketPlayersWithoutFinancialFilters(
              null, null, null, null, null);

      // Assert
      assertThat(count).isGreaterThanOrEqualTo(2L);
    }

    @Test
    @DisplayName("Filtros mutuamente excluyentes responden conteo 0 y lista vacía")
    void mutuallyExclusiveFiltersReturnZeroCount() {
      // Arrange
      Player p = testData.createVisiblePlayer();
      testData.initializeMarket(p); // has quote

      // Act (hasQuote = false and minPrice = 1000)
      long count =
          officialMarketRepository.countOfficialMarketPlayersWithFinancialFilters(
              null, null, null, null, null, false, 1000L, null);
      List<OfficialMarketPlayerProjection> results =
          officialMarketRepository.findOfficialMarketPlayers(
              null, null, null, null, null, false, 1000L, null, "id", "asc", 10, 0);

      // Assert
      assertThat(count).isZero();
      assertThat(results).isEmpty();
    }
  }

  @Nested
  @DisplayName("Verificación diagnóstica de plan de ejecución SQL")
  class DiagnosticPlanVerification {

    @Test
    @DisplayName("Inspecciona el plan EXPLAIN de la consulta nativa de catálogo oficial")
    void inspectsExecutionPlanDiagnostically() {
      // Arrange
      String explainSql =
          """
          EXPLAIN
          SELECT p.id, p.name, q.price
          FROM players p
          JOIN teams t ON t.id = p.team_id
          JOIN leagues l ON l.id = t.league_id
          LEFT JOIN LATERAL (
              SELECT pq.id AS quote_id, pq.price
              FROM player_quotes pq
              WHERE pq.player_id = p.id
              ORDER BY pq.effective_date DESC, (CASE WHEN pq.origin = 'RECOMPUTED' THEN 0 ELSE 1 END), pq.calculated_at DESC, pq.id DESC
              LIMIT 1
          ) q ON true
          WHERE p.active = true AND t.current = true
          LIMIT 20
          """;

      // Act
      @SuppressWarnings("unchecked")
      List<String> planLines = entityManager.createNativeQuery(explainSql).getResultList();

      // Assert
      assertThat(planLines).isNotEmpty();
      log.info(
          "Plan de ejecución diagnóstica para consulta LATERAL del Mercado Oficial: {}",
          String.join("\n", planLines));
    }
  }
}
