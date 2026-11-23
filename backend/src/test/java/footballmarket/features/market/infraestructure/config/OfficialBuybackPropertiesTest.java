package footballmarket.features.market.infraestructure.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import footballmarket.features.market.application.exceptions.InvalidOfficialBuybackConfigurationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Verifica los límites admitidos por la configuración de comisión de venta oficial. */
@Tag("unit")
class OfficialBuybackPropertiesTest {

  @Nested
  @DisplayName("Límites de comisión")
  class CommissionLimits {

    @Test
    @DisplayName("Acepta la comisión mínima del 10%")
    void aceptaComisionMinima() {
      // Act
      OfficialBuybackProperties properties = new OfficialBuybackProperties(10);

      // Assert
      assertThat(properties.discountPercentage()).isEqualTo(10);
    }

    @Test
    @DisplayName("Acepta la comisión máxima del 99%")
    void aceptaComisionMaxima() {
      // Act
      OfficialBuybackProperties properties = new OfficialBuybackProperties(99);

      // Assert
      assertThat(properties.discountPercentage()).isEqualTo(99);
    }

    @ParameterizedTest
    @ValueSource(ints = {Integer.MIN_VALUE, 0, 9, 100, Integer.MAX_VALUE})
    @DisplayName("Rechaza una comisión fuera del rango 10–99")
    void rechazaComisionFueraDeRango(int commission) {
      // Act / Assert
      assertThatThrownBy(() -> new OfficialBuybackProperties(commission))
          .isInstanceOf(InvalidOfficialBuybackConfigurationException.class);
    }
  }
}
