package footballmarket.features.dashboard.application;

import footballmarket.features.auth.application.AuthenticationService;
import footballmarket.features.dashboard.infrastructure.DashboardRepository;
import footballmarket.features.dashboard.infrastructure.projections.P2PDealProjection;
import footballmarket.features.dashboard.infrastructure.projections.PlayerMoverProjection;
import footballmarket.features.dashboard.infrastructure.projections.ValuePickProjection;
import footballmarket.features.dashboard.presentation.dtos.response.DashboardAllocationDTO;
import footballmarket.features.dashboard.presentation.dtos.response.DashboardFinancialSummaryDTO;
import footballmarket.features.dashboard.presentation.dtos.response.DashboardMarketPulseDTO;
import footballmarket.features.dashboard.presentation.dtos.response.DashboardOverviewResponseDTO;
import footballmarket.features.dashboard.presentation.dtos.response.DashboardP2PDealDTO;
import footballmarket.features.dashboard.presentation.dtos.response.DashboardPlayerMoverDTO;
import footballmarket.features.dashboard.presentation.dtos.response.DashboardRecentActivityDTO;
import footballmarket.features.dashboard.presentation.dtos.response.DashboardValuePickDTO;
import footballmarket.features.dashboard.presentation.mappers.DashboardMapper;
import footballmarket.features.portfolio.application.OperationHistoryService;
import footballmarket.features.portfolio.application.PortfolioService;
import footballmarket.features.portfolio.domain.records.OperationHistoryView;
import footballmarket.features.portfolio.domain.records.PortfolioView;
import java.time.OffsetDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Orquestador de agregación del resumen ejecutivo del Dashboard. */
@Service
@RequiredArgsConstructor
public class DashboardOverviewService {

  private final DashboardRepository dashboardRepository;
  private final PortfolioService portfolioService;
  private final AuthenticationService authenticationService;
  private final OperationHistoryService operationHistoryService;

  @Transactional(readOnly = true)
  public DashboardOverviewResponseDTO getOverview(String userEmail) {
    OffsetDateTime now = OffsetDateTime.now();

    // 1. Datos personales del usuario
    long availableCredits = 0L;
    try {
      availableCredits = this.authenticationService.getCurrentUser(userEmail).availableCredits();
    } catch (Exception ignored) {
      // Si el usuario no tiene cuenta de créditos aún, 0
    }

    PortfolioView portfolioView = this.portfolioService.portfolio(userEmail, 0, 100, "OPEN");

    DashboardFinancialSummaryDTO financialSummary =
        DashboardMapper.toFinancialSummary(
            availableCredits, portfolioView != null ? portfolioView.summary() : null);

    DashboardAllocationDTO allocation =
        DashboardMapper.toAllocation(portfolioView != null ? portfolioView.positions() : List.of());

    // 2. Pulso macro del mercado
    long totalMarketCap = this.dashboardRepository.calculateTotalMarketCap();
    long volume24h = this.dashboardRepository.calculateVolumeSince(now.minusHours(24));
    long totalTransactions = this.dashboardRepository.countTotalTransactions();
    int activeListings = this.dashboardRepository.countActiveListings(now);

    DashboardMarketPulseDTO marketPulse =
        new DashboardMarketPulseDTO(totalMarketCap, volume24h, totalTransactions, activeListings);

    // 3. Top Gainers & Losers
    List<PlayerMoverProjection> gainersProj = this.dashboardRepository.findTopGainers(5);
    List<DashboardPlayerMoverDTO> topGainers =
        gainersProj.stream().map(DashboardMapper::toPlayerMover).toList();

    List<PlayerMoverProjection> losersProj = this.dashboardRepository.findTopLosers(5);
    List<DashboardPlayerMoverDTO> topLosers =
        losersProj.stream().map(DashboardMapper::toPlayerMover).toList();

    // 4. Value Picks
    List<ValuePickProjection> valuePicksProj = this.dashboardRepository.findValuePicks(4);
    List<DashboardValuePickDTO> valuePicks =
        valuePicksProj.stream().map(DashboardMapper::toValuePick).toList();

    // 5. Best P2P Deals
    List<P2PDealProjection> dealsProj = this.dashboardRepository.findBestP2PDeals(now, 3);
    List<DashboardP2PDealDTO> bestP2PDeals =
        dealsProj.stream().map(DashboardMapper::toP2PDeal).toList();

    // 6. Actividad Reciente del usuario
    OperationHistoryView historyView =
        this.operationHistoryService.history(userEmail, 0, 5, null, null, null, null, "NEWEST");
    List<DashboardRecentActivityDTO> recentActivity = DashboardMapper.toRecentActivity(historyView);

    return new DashboardOverviewResponseDTO(
        financialSummary,
        allocation,
        marketPulse,
        topGainers,
        topLosers,
        valuePicks,
        bestP2PDeals,
        recentActivity,
        now);
  }
}
