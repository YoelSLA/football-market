package footballmarket.features.dashboard.presentation.mappers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import footballmarket.features.dashboard.infrastructure.projections.P2PDealProjection;
import footballmarket.features.dashboard.infrastructure.projections.PlayerMoverProjection;
import footballmarket.features.dashboard.infrastructure.projections.ValuePickProjection;
import footballmarket.features.dashboard.presentation.dtos.response.AllocationItemDTO;
import footballmarket.features.dashboard.presentation.dtos.response.DashboardAllocationDTO;
import footballmarket.features.dashboard.presentation.dtos.response.DashboardFinancialSummaryDTO;
import footballmarket.features.dashboard.presentation.dtos.response.DashboardP2PDealDTO;
import footballmarket.features.dashboard.presentation.dtos.response.DashboardPlayerMoverDTO;
import footballmarket.features.dashboard.presentation.dtos.response.DashboardRecentActivityDTO;
import footballmarket.features.dashboard.presentation.dtos.response.DashboardValuePickDTO;
import footballmarket.features.portfolio.domain.records.OperationHistoryEntry;
import footballmarket.features.portfolio.domain.records.OperationHistoryView;
import footballmarket.features.portfolio.domain.records.PortfolioView;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@Tag("unit")
class DashboardMapperTest {

  @Nested
  @DisplayName("Mapeo de resumen financiero (toFinancialSummary)")
  class FinancialSummaryMapping {

    @Test
    @DisplayName("Mapea resumen nulo a DTO por defecto con créditos disponibles")
    void mapsNullSummaryToDefaultValues() {
      DashboardFinancialSummaryDTO result = DashboardMapper.toFinancialSummary(5000L, null);

      assertThat(result).isNotNull();
      assertThat(result.availableCredits()).isEqualTo(5000L);
      assertThat(result.totalInvested()).isZero();
      assertThat(result.netWorth()).isEqualTo(5000L);
      assertThat(result.totalPortfolioValue()).isZero();
      assertThat(result.totalProfitLossPercentage()).isEqualTo(BigDecimal.ZERO);
      assertThat(result.totalTokens()).isZero();
      assertThat(result.valuationComplete()).isTrue();
    }

    @Test
    @DisplayName("Calcula patrimonio neto sumando créditos y valor actual cuando existe cotización")
    void calculatesNetWorthWhenCurrentValueIsPresent() {
      PortfolioView.PortfolioSummary summary =
          new PortfolioView.PortfolioSummary(
              2, 10, 2, 8, 2000L, 2500L, 500L, 500L, 1000L, BigDecimal.valueOf(50.0), 3L, 1L, true);

      DashboardFinancialSummaryDTO result = DashboardMapper.toFinancialSummary(1000L, summary);

      assertThat(result.netWorth()).isEqualTo(3500L);
      assertThat(result.availableCredits()).isEqualTo(1000L);
      assertThat(result.totalPortfolioValue()).isEqualTo(2500L);
      assertThat(result.totalInvested()).isEqualTo(2000L);
      assertThat(result.totalUnrealizedProfitLoss()).isEqualTo(500L);
      assertThat(result.totalRealizedProfitLoss()).isEqualTo(500L);
      assertThat(result.totalProfitLoss()).isEqualTo(1000L);
      assertThat(result.totalTokens()).isEqualTo(10);
      assertThat(result.availableTokens()).isEqualTo(8);
      assertThat(result.reservedTokens()).isEqualTo(2);
      assertThat(result.valuationComplete()).isTrue();
    }

    @Test
    @DisplayName("Deja patrimonio neto como nulo si la valoración no está completa")
    void netWorthIsNullWhenTotalCurrentValueIsNull() {
      PortfolioView.PortfolioSummary summary =
          new PortfolioView.PortfolioSummary(
              1, 5, 0, 5, 1000L, null, 0L, null, null, null, 1L, 0L, false);

      DashboardFinancialSummaryDTO result = DashboardMapper.toFinancialSummary(1000L, summary);

      assertThat(result.netWorth()).isNull();
      assertThat(result.totalPortfolioValue()).isNull();
      assertThat(result.valuationComplete()).isFalse();
    }
  }

  @Nested
  @DisplayName("Mapeo de asignación de activos (toAllocation)")
  class AllocationMapping {

    @Test
    @DisplayName("Devuelve asignación vacía si la lista de posiciones es nula o vacía")
    void handlesNullOrEmptyPositions() {
      DashboardAllocationDTO nullResult = DashboardMapper.toAllocation(null);
      DashboardAllocationDTO emptyResult = DashboardMapper.toAllocation(List.of());

      assertThat(nullResult.byPosition()).isEmpty();
      assertThat(nullResult.byLeague()).isEmpty();
      assertThat(nullResult.topHoldingPlayerName()).isEqualTo("Sin posiciones");

      assertThat(emptyResult.byPosition()).isEmpty();
      assertThat(emptyResult.byLeague()).isEmpty();
      assertThat(emptyResult.topHoldingPlayerName()).isEqualTo("Sin posiciones");
    }

    @Test
    @DisplayName(
        "Agrupa y calcula porcentajes por posición y por liga ignorando posiciones cerradas")
    void groupsAndCalculatesPercentagesForOpenPositions() {
      PortfolioView.PortfolioPosition pos1 =
          new PortfolioView.PortfolioPosition(
              1L,
              "Lionel Messi",
              "Inter Miami",
              "LaLiga Santander",
              "Delantero",
              "img1",
              true,
              "OPEN",
              "VALUED",
              10,
              0,
              10,
              1000L,
              100L,
              150L,
              OffsetDateTime.now(),
              1500L,
              0L,
              500L,
              500L,
              BigDecimal.TEN,
              BigDecimal.TEN,
              OffsetDateTime.now(),
              null,
              OffsetDateTime.now(),
              1L,
              0L);

      PortfolioView.PortfolioPosition pos2 =
          new PortfolioView.PortfolioPosition(
              2L,
              "Luka Modric",
              "Real Madrid",
              "Premier League",
              "Midfielder",
              "img2",
              true,
              "OPEN",
              "VALUED",
              5,
              0,
              5,
              500L,
              100L,
              100L,
              OffsetDateTime.now(),
              500L,
              0L,
              0L,
              0L,
              BigDecimal.ZERO,
              BigDecimal.TEN,
              OffsetDateTime.now(),
              null,
              OffsetDateTime.now(),
              1L,
              0L);

      PortfolioView.PortfolioPosition closedPos =
          new PortfolioView.PortfolioPosition(
              3L,
              "Sergio Ramos",
              "PSG",
              "Ligue 1",
              "Defensa",
              "img3",
              true,
              "CLOSED",
              "VALUED",
              0,
              0,
              0,
              0L,
              null,
              null,
              null,
              null,
              100L,
              null,
              100L,
              null,
              null,
              OffsetDateTime.now(),
              OffsetDateTime.now(),
              OffsetDateTime.now(),
              1L,
              1L);

      DashboardAllocationDTO result = DashboardMapper.toAllocation(List.of(pos1, pos2, closedPos));

      assertThat(result.topHoldingPlayerName()).isEqualTo("Lionel Messi");
      assertThat(result.byPosition()).hasSize(2);
      AllocationItemDTO topPos = result.byPosition().getFirst();
      assertThat(topPos.categoryKey()).isEqualTo("DELANTERO");
      assertThat(topPos.label()).isEqualTo("Delanteros");
      assertThat(topPos.totalValue()).isEqualTo(1500L);
      assertThat(topPos.percentage()).isEqualByComparingTo(BigDecimal.valueOf(75.00));
      assertThat(topPos.tokenCount()).isEqualTo(10);

      assertThat(result.byLeague()).hasSize(2);
      AllocationItemDTO topLeague = result.byLeague().getFirst();
      assertThat(topLeague.categoryKey()).isEqualTo("LALIGA");
      assertThat(topLeague.label()).isEqualTo("LaLiga");
      assertThat(topLeague.totalValue()).isEqualTo(1500L);
    }

    @Test
    @DisplayName("Usa acquisitionCost si currentValue es nulo y Desconocido si playerName es nulo")
    void usesAcquisitionCostWhenCurrentValueIsNull() {
      PortfolioView.PortfolioPosition pos =
          new PortfolioView.PortfolioPosition(
              1L,
              null,
              "Team",
              "Unknown League",
              null,
              null,
              true,
              "OPEN",
              "UNVALUED",
              4,
              0,
              4,
              800L,
              200L,
              null,
              null,
              null,
              0L,
              null,
              0L,
              null,
              null,
              OffsetDateTime.now(),
              null,
              null,
              1L,
              0L);

      DashboardAllocationDTO result = DashboardMapper.toAllocation(List.of(pos));

      assertThat(result.topHoldingPlayerName()).isEqualTo("Desconocido");
      assertThat(result.byPosition()).hasSize(1);
      assertThat(result.byPosition().getFirst().categoryKey()).isEqualTo("DELANTERO");
      assertThat(result.byLeague()).hasSize(1);
      assertThat(result.byLeague().getFirst().categoryKey()).isEqualTo("OTRAS_LIGAS");
      assertThat(result.byLeague().getFirst().label()).isEqualTo("Otras Ligas");
      assertThat(result.byLeague().getFirst().totalValue()).isEqualTo(800L);
    }
  }

  @Nested
  @DisplayName("Mapeo de jugadores destacados (toPlayerMover, toValuePick, toP2PDeal)")
  class ProjectionsMapping {

    @Test
    @DisplayName("toPlayerMover divide nombre y apellido y normaliza nulos")
    void mapsPlayerMoverWithFallbacks() {
      PlayerMoverProjection proj = mock(PlayerMoverProjection.class);
      when(proj.getPlayerId()).thenReturn(10L);
      when(proj.getName()).thenReturn("Kylian Mbappé");
      when(proj.getTeamName()).thenReturn(null);
      when(proj.getLeagueName()).thenReturn(null);
      when(proj.getPosition()).thenReturn("keeper");
      when(proj.getImageUrl()).thenReturn("http://img");
      when(proj.getCurrentPrice()).thenReturn(null);
      when(proj.getPreviousPrice()).thenReturn(100L);
      when(proj.getDeltaCredits()).thenReturn(20L);
      when(proj.getDeltaPercentage()).thenReturn(BigDecimal.valueOf(20.0));
      when(proj.getScore()).thenReturn(BigDecimal.valueOf(7.8));

      DashboardPlayerMoverDTO dto = DashboardMapper.toPlayerMover(proj);

      assertThat(dto.playerId()).isEqualTo(10L);
      assertThat(dto.firstName()).isEqualTo("Kylian");
      assertThat(dto.lastName()).isEqualTo("Mbappé");
      assertThat(dto.teamName()).isEqualTo("Club");
      assertThat(dto.leagueName()).isEqualTo("Liga");
      assertThat(dto.position()).isEqualTo("ARQUERO");
      assertThat(dto.currentPrice()).isZero();
      assertThat(dto.previousPrice()).isEqualTo(100L);
      assertThat(dto.deltaCredits()).isEqualTo(20L);
      assertThat(dto.deltaPercentage()).isEqualTo(BigDecimal.valueOf(20.0));
      assertThat(dto.score()).isEqualTo(BigDecimal.valueOf(7.8));
    }

    @ParameterizedTest
    @CsvSource(
        value = {
          "'Neymar', 'Neymar', ''",
          "'', 'Jugador', ''",
          "'  ', 'Jugador', ''",
          "null, 'Jugador', ''"
        },
        nullValues = {"null"})
    @DisplayName("toPlayerMover maneja variantes de formato de nombre")
    void handlesNameFormattingVariations(
        String rawName, String expectedFirst, String expectedLast) {
      PlayerMoverProjection proj = mock(PlayerMoverProjection.class);
      when(proj.getName()).thenReturn(rawName);

      DashboardPlayerMoverDTO dto = DashboardMapper.toPlayerMover(proj);

      assertThat(dto.firstName()).isEqualTo(expectedFirst);
      assertThat(dto.lastName()).isEqualTo(expectedLast);
    }

    @Test
    @DisplayName("toValuePick mapea valores con fallbacks para ceros y nulos")
    void mapsValuePickWithFallbacks() {
      ValuePickProjection proj = mock(ValuePickProjection.class);
      when(proj.getPlayerId()).thenReturn(11L);
      when(proj.getName()).thenReturn("Vinicius Junior");
      when(proj.getTeamName()).thenReturn("Real Madrid");
      when(proj.getLeagueName()).thenReturn("LaLiga");
      when(proj.getPosition()).thenReturn("forward");
      when(proj.getImageUrl()).thenReturn("http://vini");
      when(proj.getWhoScoredScore()).thenReturn(null);
      when(proj.getCurrentPrice()).thenReturn(null);
      when(proj.getValueRatio()).thenReturn(null);

      DashboardValuePickDTO dto = DashboardMapper.toValuePick(proj);

      assertThat(dto.playerId()).isEqualTo(11L);
      assertThat(dto.firstName()).isEqualTo("Vinicius");
      assertThat(dto.lastName()).isEqualTo("Junior");
      assertThat(dto.whoScoredScore()).isEqualTo(BigDecimal.ZERO);
      assertThat(dto.currentPrice()).isZero();
      assertThat(dto.valueRatio()).isEqualTo(BigDecimal.ZERO);
      assertThat(dto.position()).isEqualTo("DELANTERO");
    }

    @Test
    @DisplayName("toP2PDeal mapea publicación con fallbacks ante campos nulos")
    void mapsP2PDealWithFallbacks() {
      P2PDealProjection proj = mock(P2PDealProjection.class);
      when(proj.getListingId()).thenReturn(99L);
      when(proj.getPlayerId()).thenReturn(12L);
      when(proj.getPlayerName()).thenReturn(null);
      when(proj.getTeamName()).thenReturn(null);
      when(proj.getLeagueName()).thenReturn(null);
      when(proj.getImageUrl()).thenReturn(null);
      when(proj.getAvailableTokens()).thenReturn(null);
      when(proj.getP2pUnitPrice()).thenReturn(null);
      when(proj.getOfficialQuotePrice()).thenReturn(null);
      when(proj.getDiscountPercentage()).thenReturn(null);

      DashboardP2PDealDTO dto = DashboardMapper.toP2PDeal(proj);

      assertThat(dto.listingId()).isEqualTo(99L);
      assertThat(dto.playerId()).isEqualTo(12L);
      assertThat(dto.playerName()).isEqualTo("Futbolista");
      assertThat(dto.teamName()).isEqualTo("Club");
      assertThat(dto.leagueName()).isEqualTo("Liga");
      assertThat(dto.availableTokens()).isZero();
      assertThat(dto.p2pUnitPrice()).isZero();
      assertThat(dto.officialQuotePrice()).isZero();
      assertThat(dto.discountPercentage()).isEqualTo(BigDecimal.ZERO);
    }
  }

  @Nested
  @DisplayName("Mapeo de actividad reciente (toRecentActivity)")
  class RecentActivityMapping {

    @Test
    @DisplayName("Devuelve lista vacía cuando el historial o las transacciones son nulas")
    void handlesNullHistoryView() {
      assertThat(DashboardMapper.toRecentActivity(null)).isEmpty();
      OperationHistoryView emptyView = new OperationHistoryView(null, null, null);
      assertThat(DashboardMapper.toRecentActivity(emptyView)).isEmpty();
    }

    @Test
    @DisplayName("Mapea transacciones completas y maneja datos nulos de jugador y tipo")
    void mapsTransactionsWithFallbacks() {
      OffsetDateTime now = OffsetDateTime.now();
      OperationHistoryEntry entryWithPlayer =
          new OperationHistoryEntry(
              100L,
              OperationHistoryEntry.OperationType.SELL,
              new OperationHistoryEntry.PlayerData(
                  5L, "Pedri", "img", "fallback", "Medio", null, null),
              2,
              500L,
              1000L,
              now);

      OperationHistoryEntry entryNullPlayer =
          new OperationHistoryEntry(101L, null, null, 1, 200L, 200L, now);

      OperationHistoryView view =
          new OperationHistoryView(null, List.of(entryWithPlayer, entryNullPlayer), null);

      List<DashboardRecentActivityDTO> result = DashboardMapper.toRecentActivity(view);

      assertThat(result).hasSize(2);
      DashboardRecentActivityDTO first = result.get(0);
      assertThat(first.transactionId()).isEqualTo(100L);
      assertThat(first.type()).isEqualTo("SELL");
      assertThat(first.playerId()).isEqualTo(5L);
      assertThat(first.playerName()).isEqualTo("Pedri");
      assertThat(first.quantity()).isEqualTo(2);
      assertThat(first.unitPrice()).isEqualTo(500L);
      assertThat(first.totalAmount()).isEqualTo(1000L);

      DashboardRecentActivityDTO second = result.get(1);
      assertThat(second.transactionId()).isEqualTo(101L);
      assertThat(second.type()).isEqualTo("BUY");
      assertThat(second.playerId()).isZero();
      assertThat(second.playerName()).isEqualTo("Futbolista");
    }
  }

  @Nested
  @DisplayName("Normalización de posiciones y ligas")
  class NormalizationTests {

    @ParameterizedTest
    @CsvSource({
      "'DELANTERO', 'DELANTERO'",
      "'Forward', 'DELANTERO'",
      "'Attacking Midfield', 'DELANTERO'",
      "'Striker', 'DELANTERO'",
      "'Medio', 'MEDIOCAMPISTA'",
      "'Midfielder', 'MEDIOCAMPISTA'",
      "'Defensor', 'DEFENSOR'",
      "'Defense', 'DEFENSOR'",
      "'Arquero', 'ARQUERO'",
      "'Goalkeeper', 'ARQUERO'",
      "'Portero', 'ARQUERO'",
      "'Desconocido', 'DELANTERO'"
    })
    @DisplayName("Normaliza variedad de strings de posición deportiva")
    void normalizesPositions(String raw, String expectedCategory) {
      PlayerMoverProjection proj = mock(PlayerMoverProjection.class);
      when(proj.getPosition()).thenReturn(raw);

      DashboardPlayerMoverDTO dto = DashboardMapper.toPlayerMover(proj);

      assertThat(dto.position()).isEqualTo(expectedCategory);
    }

    @ParameterizedTest
    @CsvSource({
      "'Premier League', 'PREMIER_LEAGUE'",
      "'LaLiga Santander', 'LALIGA'",
      "'Primera División', 'LALIGA'",
      "'Serie A TIM', 'SERIE_A'",
      "'Calcio', 'SERIE_A'",
      "'Bundesliga', 'BUNDESLIGA'",
      "'Ligue 1 Uber Eats', 'LIGUE_1'",
      "'Brasileirao', 'OTRAS_LIGAS'",
      "'', 'OTRAS_LIGAS'"
    })
    @DisplayName("Normaliza variedad de nombres de ligas de fútbol")
    void normalizesLeagues(String rawLeague, String expectedCategory) {
      PortfolioView.PortfolioPosition pos =
          new PortfolioView.PortfolioPosition(
              1L,
              "Player",
              "Team",
              rawLeague,
              "Medio",
              null,
              true,
              "OPEN",
              "VALUED",
              1,
              0,
              1,
              100L,
              100L,
              100L,
              null,
              100L,
              0L,
              0L,
              0L,
              BigDecimal.ZERO,
              BigDecimal.ZERO,
              null,
              null,
              null,
              1L,
              0L);

      DashboardAllocationDTO dto = DashboardMapper.toAllocation(List.of(pos));

      assertThat(dto.byLeague().getFirst().categoryKey()).isEqualTo(expectedCategory);
    }
  }
}
