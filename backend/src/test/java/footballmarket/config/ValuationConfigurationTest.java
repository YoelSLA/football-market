package footballmarket.config;

import static org.assertj.core.api.Assertions.assertThat;

import footballmarket.features.valuation.infraestructure.config.ValuationConfig;
import footballmarket.features.valuation.infraestructure.config.ValuationProperties;
import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

@org.junit.jupiter.api.Tag("lightweight")
class ValuationConfigurationTest {
  private final ApplicationContextRunner runner =
      new ApplicationContextRunner()
          .withUserConfiguration(ValuationConfig.class)
          .withPropertyValues(
              "valuation.minimum-minutes.field=270",
              "valuation.minimum-minutes.goalkeeper=540",
              "valuation.performance-maximum-impact=0.10",
              "valuation.discipline-maximum-impact=0.05",
              "valuation.attribute-penalty-maximum-impact=0.10");

  @Nested
  @DisplayName("Configuración operativa de valoración")
  class Binding {
    @Test
    @DisplayName("Vincula los valores iniciales de las estrategias")
    void bindsInitialValues() {
      runner.run(
          context -> {
            assertThat(context).hasNotFailed().hasSingleBean(ValuationProperties.class);
            ValuationProperties properties = context.getBean(ValuationProperties.class);
            assertThat(properties.minimumMinutes().field()).isEqualTo(270);
            assertThat(properties.minimumMinutes().goalkeeper()).isEqualTo(540);
            assertThat(properties.performanceMaximumImpact())
                .isEqualByComparingTo(BigDecimal.valueOf(.10));
          });
    }

    @ParameterizedTest
    @ValueSource(
        strings = {
          "valuation.minimum-minutes.field=0",
          "valuation.minimum-minutes.goalkeeper=0",
          "valuation.performance-maximum-impact=1.01",
          "valuation.discipline-maximum-impact=-0.01",
          "valuation.attribute-penalty-maximum-impact=2"
        })
    @DisplayName("Rechaza mínimos e impactos fuera de rango")
    void rejectsInvalidValues(String override) {
      runner.withPropertyValues(override).run(context -> assertThat(context).hasFailed());
    }
  }
}
