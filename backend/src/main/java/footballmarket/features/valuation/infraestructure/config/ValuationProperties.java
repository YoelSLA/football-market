package footballmarket.features.valuation.infraestructure.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/** Parámetros operativos de las estrategias de valoración v1. */
@Validated
@ConfigurationProperties(prefix = "valuation")
public record ValuationProperties(
    @Valid @NotNull MinimumMinutes minimumMinutes,
    @NotNull @DecimalMin("0.0") @DecimalMax("1.0") BigDecimal performanceMaximumImpact,
    @NotNull @DecimalMin("0.0") @DecimalMax("1.0") BigDecimal disciplineMaximumImpact,
    @NotNull @DecimalMin("0.0") @DecimalMax("1.0") BigDecimal attributePenaltyMaximumImpact) {

  /** Mínimos de minutos usados por el ajuste de confianza. */
  public record MinimumMinutes(@Positive int field, @Positive int goalkeeper) {}
}
