package footballmarket.features.quote.infraestructure.config;

import footballmarket.features.valuation.domain.enums.ValuationStrategy;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Parámetros del control de variabilidad y de la programación de cotizaciones.
 *
 * <p>Los valores por defecto están disponibles sin configuración explícita: el factor de ajuste y
 * el tope de variación en 0,10, el piso mínimo en 1 centésima y el ancla inicial en 100 centésimas.
 * Los parámetros de variabilidad son ajustables por el operador en cada ejecución manual. La
 * estrategia automática y la cadencia UTC se configuran al arrancar la aplicación.
 */
@Validated
@ConfigurationProperties(prefix = "market.quote")
public record QuoteProperties(
    @NotNull @DecimalMin("0.10") @DecimalMax("1.00") BigDecimal adjustmentFactor,
    @NotNull @DecimalMin(value = "0.0", inclusive = false) @DecimalMax("1.0")
        BigDecimal variationCap,
    @NotNull @Positive Long minPriceFloor,
    @NotNull @Positive Long initialAnchor,
    @NotNull ValuationStrategy strategy,
    @NotNull @Valid Schedule schedule) {

  /** Factor de ajuste por defecto. */
  public static final BigDecimal DEFAULT_ADJUSTMENT_FACTOR = new BigDecimal("0.10");

  /** Tope de variación por defecto, expresado como fracción del precio vigente. */
  public static final BigDecimal DEFAULT_VARIATION_CAP = new BigDecimal("0.10");

  /** Piso mínimo de precio por defecto, en centésimas de crédito. */
  public static final long DEFAULT_MIN_PRICE_FLOOR = 1L;

  /** Ancla inicial de 1 crédito por token, en centésimas de crédito. */
  public static final long DEFAULT_INITIAL_ANCHOR = 100L;

  public QuoteProperties {
    adjustmentFactor = adjustmentFactor == null ? DEFAULT_ADJUSTMENT_FACTOR : adjustmentFactor;
    variationCap = variationCap == null ? DEFAULT_VARIATION_CAP : variationCap;
    minPriceFloor = minPriceFloor == null ? DEFAULT_MIN_PRICE_FLOOR : minPriceFloor;
    initialAnchor = initialAnchor == null ? DEFAULT_INITIAL_ANCHOR : initialAnchor;
    strategy = strategy == null ? ValuationStrategy.POSITIONAL_PERFORMANCE : strategy;
    schedule = schedule == null ? new Schedule(null, null) : schedule;
  }

  /** Cadencia UTC y habilitación del disparador; Spring valida la sintaxis del cron al arrancar. */
  public record Schedule(@NotBlank String cron, @NotNull Boolean enabled) {
    public Schedule {
      cron = cron == null ? "0 0 12 * * *" : cron;
      enabled = enabled == null ? true : enabled;
    }
  }
}
