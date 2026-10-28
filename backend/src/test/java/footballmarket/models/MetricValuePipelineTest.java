package footballmarket.models;

import static org.assertj.core.api.Assertions.*;

import footballmarket.models.enums.MetricAvailability;
import footballmarket.models.exceptions.InvalidPlayerException;
import java.math.BigDecimal;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;

@org.junit.jupiter.api.Tag("unit")
class MetricValuePipelineTest {
  @Nested
  @DisplayName("Normalización decimal y ausencias")
  class Normalization {
    @ParameterizedTest
    @ValueSource(strings = {"6.9033333333333333", "\t 6,9033333333333333  "})
    @DisplayName("Conserva el rating publicado y sus dieciséis decimales con punto o coma")
    void preservesRating(String published) {
      // Act
      MetricValueNormalizer.NormalizedMetricValue value =
          MetricValueNormalizer.normalize(published);
      // Assert
      assertThat(value.value()).isEqualTo(new BigDecimal("6.9033333333333333"));
      assertThat(value.observedPrecision()).isEqualTo(16);
      assertThat(value.availability()).isEqualTo(MetricAvailability.PRESENT);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"-", " \t ", "N/A", "\t N/A  "})
    @DisplayName("Distingue una ausencia de un cero publicado")
    void distinguishesAbsence(String published) {
      // Act
      MetricValueNormalizer.NormalizedMetricValue absent =
          MetricValueNormalizer.normalize(published);
      MetricValueNormalizer.NormalizedMetricValue zero = MetricValueNormalizer.normalize("0");
      // Assert
      assertThat(absent.availability()).isEqualTo(MetricAvailability.ABSENT);
      assertThat(absent.value()).isNull();
      assertThat(absent.observedPrecision()).isNull();
      assertThat(zero.availability()).isEqualTo(MetricAvailability.PRESENT);
      assertThat(zero.value()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @ParameterizedTest
    @ValueSource(strings = {"NaN", "null", "abc", "N/A%", "n/a"})
    @DisplayName("Rechaza textos ajenos al marcador literal de ausencia autorizado")
    void rejectsInvalidText(String published) {
      // Act / Assert
      assertThatThrownBy(() -> MetricValueNormalizer.normalize(published))
          .isInstanceOf(InvalidPlayerException.class);
    }
  }

  @Nested
  @DisplayName("Derivación por apariciones del mismo contexto")
  class Derivation {
    @ParameterizedTest
    @CsvSource({"1,3,0.333333", "1,128,0.007813", "0,3,0.000000"})
    @DisplayName("Aplica exclusivamente al cociente seis decimales y HALF_UP")
    void roundsDerived(String accumulated, String appearances, String expected) {
      // Act
      MetricDerivationApplier.DerivedMetricValue value =
          MetricDerivationApplier.perAppearance(
              new BigDecimal(accumulated), new BigDecimal(appearances));
      // Assert
      assertThat(value.value()).isEqualTo(new BigDecimal(expected));
      assertThat(value.observedPrecision()).isEqualTo(6);
    }

    @Test
    @DisplayName("No inventa cocientes con apariciones nulas o cero ni acumulados ausentes")
    void doesNotInventValues() {
      // Act / Assert
      assertThat(MetricDerivationApplier.perAppearance(BigDecimal.ONE, null).availability())
          .isEqualTo(MetricAvailability.NOT_DERIVABLE);
      assertThat(
              MetricDerivationApplier.perAppearance(BigDecimal.ONE, BigDecimal.ZERO).availability())
          .isEqualTo(MetricAvailability.NOT_DERIVABLE);
      assertThat(MetricDerivationApplier.perAppearance(null, BigDecimal.ONE).availability())
          .isEqualTo(MetricAvailability.ABSENT);
    }
  }
}
