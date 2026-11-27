package footballmarket.features.dashboard.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import footballmarket.features.dashboard.infrastructure.DashboardRepository;
import footballmarket.features.dashboard.infrastructure.projections.FmiBenchmarkProjection;
import footballmarket.features.dashboard.presentation.dtos.response.DashboardPerformanceResponseDTO;
import footballmarket.features.portfolio.application.PortfolioService;
import footballmarket.features.portfolio.domain.records.PortfolioView;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@Tag("unit")
class DashboardPerformanceServiceTest {

  private DashboardRepository dashboardRepository;
  private PortfolioService portfolioService;
  private DashboardPerformanceService service;

  @BeforeEach
  void setUp() {
    dashboardRepository = mock(DashboardRepository.class);
    portfolioService = mock(PortfolioService.class);
    service = new DashboardPerformanceService(dashboardRepository, portfolioService);
  }

  @Nested
  @DisplayName("Validación de fechas base y respuesta vacía")
  class EmptyAndBaseValidations {

    @Test
    @DisplayName("Devuelve listas vacías si no existen fechas de cotizaciones")
    void returnsEmptyWhenNoDatesAvailable() {
      when(dashboardRepository.findDistinctQuoteDates()).thenReturn(null);

      DashboardPerformanceResponseDTO result = service.getPerformance("user@test.com", "1M");

      assertThat(result.dates()).isEmpty();
      assertThat(result.userPortfolioSeries()).isEmpty();
      assertThat(result.marketBenchmarkSeries()).isEmpty();

      when(dashboardRepository.findDistinctQuoteDates()).thenReturn(List.of());

      result = service.getPerformance("user@test.com", "1M");

      assertThat(result.dates()).isEmpty();
      assertThat(result.userPortfolioSeries()).isEmpty();
      assertThat(result.marketBenchmarkSeries()).isEmpty();
    }
  }

  @Nested
  @DisplayName("Filtrado por rango temporal y normalización")
  class RangeAndCalculations {

    @Test
    @DisplayName("Filtra fechas para rango 1M y calcula series normalizadas")
    void filtersOneMonthRangeAndCalculatesSeries() {
      LocalDate now = LocalDate.now();
      LocalDate d1 = now.minusDays(45); // fuera de 1M
      LocalDate d2 = now.minusDays(20); // dentro de 1M
      LocalDate d3 = now.minusDays(5); // dentro de 1M

      when(dashboardRepository.findDistinctQuoteDates()).thenReturn(List.of(d1, d2, d3));

      FmiBenchmarkProjection fmi1 = mock(FmiBenchmarkProjection.class);
      when(fmi1.getEffectiveDate()).thenReturn(d2);
      when(fmi1.getAveragePrice()).thenReturn(BigDecimal.valueOf(100));

      FmiBenchmarkProjection fmi2 = mock(FmiBenchmarkProjection.class);
      when(fmi2.getEffectiveDate()).thenReturn(d3);
      when(fmi2.getAveragePrice()).thenReturn(BigDecimal.valueOf(120));

      when(dashboardRepository.findHistoricalFmiBenchmark()).thenReturn(List.of(fmi1, fmi2));

      PortfolioView.PortfolioSummary summary =
          new PortfolioView.PortfolioSummary(
              1, 10, 0, 10, 1000L, 2000L, 0L, 1000L, 1000L, BigDecimal.TEN, 1L, 0L, true);
      PortfolioView view = new PortfolioView(summary, List.of(), null);
      when(portfolioService.portfolio(anyString(), anyInt(), anyInt(), anyString()))
          .thenReturn(view);

      DashboardPerformanceResponseDTO response = service.getPerformance("user@test.com", "1M");

      assertThat(response.dates()).containsExactly(d2, d3);
      assertThat(response.marketBenchmarkSeries()).hasSize(2);
      assertThat(response.marketBenchmarkSeries().get(0))
          .isEqualByComparingTo(BigDecimal.valueOf(100.00));
      assertThat(response.marketBenchmarkSeries().get(1))
          .isEqualByComparingTo(BigDecimal.valueOf(120.00));

      assertThat(response.userPortfolioSeries()).hasSize(2);
      assertThat(response.userPortfolioSeries().get(1)).isEqualTo(2000L);
    }

    @ParameterizedTest
    @ValueSource(strings = {"ALL", "INVALID_RANGE"})
    @DisplayName("Incluye todas las fechas ante rangos completos o no reconocidos")
    void includesAllDatesForFullOrUnknownRange(String range) {
      LocalDate now = LocalDate.now();
      LocalDate d1 = now.minusDays(120);
      LocalDate d2 = now.minusDays(10);

      when(dashboardRepository.findDistinctQuoteDates()).thenReturn(List.of(d1, d2));
      when(dashboardRepository.findHistoricalFmiBenchmark()).thenReturn(List.of());

      DashboardPerformanceResponseDTO response = service.getPerformance("user@test.com", range);

      assertThat(response.dates()).containsExactly(d1, d2);
    }

    @Test
    @DisplayName("Devuelve ceros en la serie de usuario cuando el portfolio no tiene valor actual")
    void userSeriesIsZeroWhenPortfolioValueIsZeroOrNull() {
      LocalDate now = LocalDate.now();
      when(dashboardRepository.findDistinctQuoteDates()).thenReturn(List.of(now));
      when(dashboardRepository.findHistoricalFmiBenchmark()).thenReturn(List.of());
      when(portfolioService.portfolio(anyString(), anyInt(), anyInt(), anyString()))
          .thenReturn(null);

      DashboardPerformanceResponseDTO response = service.getPerformance("user@test.com", "1M");

      assertThat(response.userPortfolioSeries()).containsExactly(0L);
    }

    @Test
    @DisplayName("Usa allDates si todas las fechas son anteriores al umbral del rango")
    void fallsBackToAllDatesWhenFilteredDatesAreEmpty() {
      LocalDate oldDate1 = LocalDate.now().minusDays(100);
      LocalDate oldDate2 = LocalDate.now().minusDays(90);

      when(dashboardRepository.findDistinctQuoteDates()).thenReturn(List.of(oldDate1, oldDate2));
      when(dashboardRepository.findHistoricalFmiBenchmark()).thenReturn(List.of());

      DashboardPerformanceResponseDTO response = service.getPerformance("user@test.com", "1M");

      assertThat(response.dates()).containsExactly(oldDate1, oldDate2);
    }
  }
}
