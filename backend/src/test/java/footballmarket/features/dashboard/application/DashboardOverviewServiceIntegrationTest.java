package footballmarket.features.dashboard.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import footballmarket.features.auth.application.AuthenticationService;
import footballmarket.features.auth.domain.enums.UserRole;
import footballmarket.features.auth.domain.records.CurrentUserView;
import footballmarket.features.dashboard.infrastructure.DashboardRepository;
import footballmarket.features.dashboard.presentation.dtos.response.DashboardOverviewResponseDTO;
import footballmarket.features.portfolio.application.OperationHistoryService;
import footballmarket.features.portfolio.application.PortfolioService;
import footballmarket.features.portfolio.domain.records.OperationHistoryView;
import footballmarket.features.portfolio.domain.records.PortfolioView;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
class DashboardOverviewServiceIntegrationTest {

  @Test
  @DisplayName("Consolida correctamente las métricas patrimoniales y de mercado")
  void getOverviewCalculatesCorrectly() {
    DashboardRepository repository = mock(DashboardRepository.class);
    PortfolioService portfolioService = mock(PortfolioService.class);
    AuthenticationService authService = mock(AuthenticationService.class);
    OperationHistoryService historyService = mock(OperationHistoryService.class);

    CurrentUserView user =
        new CurrentUserView(1L, "yoel", "test@example.com", UserRole.USER, 500000L);
    when(authService.getCurrentUser("test@example.com")).thenReturn(user);

    PortfolioView.PortfolioSummary summary =
        new PortfolioView.PortfolioSummary(
            2,
            20,
            5,
            15,
            800000L,
            1000000L,
            50000L,
            200000L,
            250000L,
            new BigDecimal("31.25"),
            3,
            1,
            true);
    PortfolioView portfolioView =
        new PortfolioView(summary, List.of(), new PortfolioView.PortfolioPagination(0, 100, 2, 1));
    when(portfolioService.portfolio(anyString(), anyInt(), anyInt(), anyString()))
        .thenReturn(portfolioView);

    when(repository.calculateTotalMarketCap()).thenReturn(150000000L);
    when(repository.calculateVolumeSince(any(OffsetDateTime.class))).thenReturn(2000000L);
    when(repository.countTotalTransactions()).thenReturn(450L);
    when(repository.countActiveListings(any(OffsetDateTime.class))).thenReturn(12);

    OperationHistoryView historyView =
        new OperationHistoryView(null, List.of(), new OperationHistoryView.Pagination(0, 5, 0, 0));
    when(historyService.history(
            anyString(), anyInt(), anyInt(), any(), any(), any(), any(), anyString()))
        .thenReturn(historyView);

    DashboardOverviewService service =
        new DashboardOverviewService(repository, portfolioService, authService, historyService);

    DashboardOverviewResponseDTO result = service.getOverview("test@example.com");

    assertThat(result).isNotNull();
    assertThat(result.financialSummary().availableCredits()).isEqualTo(500000L);
    assertThat(result.financialSummary().netWorth()).isEqualTo(1500000L);
    assertThat(result.marketPulse().totalMarketCap()).isEqualTo(150000000L);
  }
}
