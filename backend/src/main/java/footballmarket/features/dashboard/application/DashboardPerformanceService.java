package footballmarket.features.dashboard.application;

import footballmarket.features.dashboard.infrastructure.DashboardRepository;
import footballmarket.features.dashboard.infrastructure.projections.FmiBenchmarkProjection;
import footballmarket.features.dashboard.presentation.dtos.response.DashboardPerformanceResponseDTO;
import footballmarket.features.portfolio.application.PortfolioService;
import footballmarket.features.portfolio.domain.records.PortfolioView;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Servicio de cálculo de series temporales de rendimiento y benchmark FMI. */
@Service
@RequiredArgsConstructor
public class DashboardPerformanceService {

  private final DashboardRepository dashboardRepository;
  private final PortfolioService portfolioService;

  @Transactional(readOnly = true)
  public DashboardPerformanceResponseDTO getPerformance(String userEmail, String range) {
    List<LocalDate> allDates = this.dashboardRepository.findDistinctQuoteDates();
    if (allDates == null || allDates.isEmpty()) {
      return new DashboardPerformanceResponseDTO(List.of(), List.of(), List.of());
    }

    LocalDate now = LocalDate.now();
    LocalDate threshold =
        switch (range != null ? range.toUpperCase() : "1M") {
          case "1M" -> now.minusDays(30);
          case "3M" -> now.minusDays(90);
          default -> LocalDate.MIN;
        };

    List<LocalDate> filteredDates = allDates.stream().filter(d -> !d.isBefore(threshold)).toList();

    if (filteredDates.isEmpty()) {
      filteredDates = allDates;
    }

    // 1. Obtener Benchmark FMI
    List<FmiBenchmarkProjection> fmiList = this.dashboardRepository.findHistoricalFmiBenchmark();
    Map<LocalDate, BigDecimal> fmiMap = new HashMap<>();
    for (FmiBenchmarkProjection f : fmiList) {
      if (f.getEffectiveDate() != null && f.getAveragePrice() != null) {
        fmiMap.put(f.getEffectiveDate(), f.getAveragePrice());
      }
    }

    // Normalizar a base 100
    BigDecimal basePrice = null;
    List<BigDecimal> benchmarkSeries = new ArrayList<>();
    for (LocalDate d : filteredDates) {
      BigDecimal avg = fmiMap.getOrDefault(d, BigDecimal.valueOf(100));
      if (basePrice == null && avg.compareTo(BigDecimal.ZERO) > 0) {
        basePrice = avg;
      }
      BigDecimal normalized =
          basePrice != null && basePrice.compareTo(BigDecimal.ZERO) > 0
              ? avg.multiply(BigDecimal.valueOf(100)).divide(basePrice, 2, RoundingMode.HALF_UP)
              : BigDecimal.valueOf(100);
      benchmarkSeries.add(normalized);
    }

    // 2. Reconstrucción de la serie del usuario
    PortfolioView portfolioView = this.portfolioService.portfolio(userEmail, 0, 100, "OPEN");
    Long currentVal =
        (portfolioView != null && portfolioView.summary() != null)
            ? portfolioView.summary().totalCurrentValue()
            : 0L;
    if (currentVal == null) currentVal = 0L;

    List<Long> userSeries = new ArrayList<>();
    int count = filteredDates.size();
    for (int i = 0; i < count; i++) {
      if (currentVal == 0L) {
        userSeries.add(0L);
      } else {
        // Interpola o proyecta la evolución según el índice FMI relativo a la fecha actual
        BigDecimal ratio =
            benchmarkSeries.get(count - 1).compareTo(BigDecimal.ZERO) > 0
                ? benchmarkSeries
                    .get(i)
                    .divide(benchmarkSeries.get(count - 1), 4, RoundingMode.HALF_UP)
                : BigDecimal.ONE;
        long val =
            BigDecimal.valueOf(currentVal)
                .multiply(ratio)
                .setScale(0, RoundingMode.HALF_UP)
                .longValue();
        userSeries.add(val);
      }
    }

    return new DashboardPerformanceResponseDTO(filteredDates, userSeries, benchmarkSeries);
  }
}
