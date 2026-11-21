package footballmarket.features.dashboard.infrastructure.projections;

import java.math.BigDecimal;
import java.time.LocalDate;

public interface FmiBenchmarkProjection {
  LocalDate getEffectiveDate();

  BigDecimal getAveragePrice();
}
