package footballmarket.models;

import static org.assertj.core.api.Assertions.*;

import footballmarket.models.enums.DerivationRule;
import footballmarket.models.enums.MetricSource;
import footballmarket.models.exceptions.InvalidPlayerException;
import java.util.List;
import org.junit.jupiter.api.*;

@org.junit.jupiter.api.Tag("unit")
class MetricCatalogTest {
  @Nested
  @DisplayName("Catálogo cerrado y cobertura")
  class Catalog {
    @Test
    @DisplayName("Las veintinueve tasas de detalle son directas en scraping y el resumen se deriva")
    void preservesNativeScrapingRates() {
      // Act
      List<MetricDefinition> nativeRates =
          MetricCatalog.definitions().stream()
              .filter(d -> d.sources().contains(MetricSource.SCRAPING))
              .filter(d -> d.derivation() == DerivationRule.DIVIDE_BY_APPEARANCES)
              .filter(d -> d.derivationFor(MetricSource.SCRAPING) == DerivationRule.NONE)
              .toList();
      // Assert
      assertThat(nativeRates).hasSize(29);
      assertThat(nativeRates)
          .extracting(MetricDefinition::key)
          .contains("total_aerial_duels_per_match", "fouls_committed_per_match", "saves_per_match");
      assertThat(MetricCatalog.require("goals_per_match").derivationFor(MetricSource.SCRAPING))
          .isEqualTo(DerivationRule.DIVIDE_BY_APPEARANCES);
      assertThat(MetricCatalog.require("assists_per_match").derivationFor(MetricSource.SCRAPING))
          .isEqualTo(DerivationRule.DIVIDE_BY_APPEARANCES);
    }

    @Test
    @DisplayName("La regla por canal rechaza una métrica no disponible en ese origen")
    void rejectsUnsupportedDerivationSource() {
      // Arrange
      MetricDefinition definition = MetricCatalog.require("shots_on_target_total");
      // Act / Assert
      assertThatThrownBy(() -> definition.derivationFor(MetricSource.JSON))
          .isInstanceOf(InvalidPlayerException.class);
      assertThatThrownBy(() -> definition.derivationFor(null))
          .isInstanceOf(InvalidPlayerException.class);
    }

    @Test
    @DisplayName("Conserva sesenta claves con cobertura 45, 57 y 42 compartidas")
    void validatesCoverage() {
      // Act
      List<MetricDefinition> definitions = MetricCatalog.definitions();
      // Assert
      assertThat(definitions).hasSize(60);
      assertThat(definitions).extracting(MetricDefinition::key).doesNotHaveDuplicates();
      assertThat(definitions.stream().filter(d -> d.sources().contains(MetricSource.SCRAPING)))
          .hasSize(45);
      assertThat(definitions.stream().filter(d -> d.sources().contains(MetricSource.JSON)))
          .hasSize(57);
      assertThat(definitions.stream().filter(d -> d.sources().size() == 2)).hasSize(42);
      assertThat(
              definitions.stream()
                  .filter(d -> d.sources().equals(java.util.Set.of(MetricSource.SCRAPING))))
          .extracting(MetricDefinition::key)
          .containsExactlyInAnyOrder(
              "shots_on_target_total", "second_yellow_total", "starts_total");
    }

    @Test
    @DisplayName(
        "Resuelve ambos aliases de faltas sin ampliar el catálogo y rechaza columnas ajenas")
    void normalizesAliases() {
      // Act / Assert
      assertThat(MetricCatalog.canonicalKey("foulsPerGame")).isEqualTo("fouls_committed_per_match");
      assertThat(MetricCatalog.canonicalKey("foulCommitted"))
          .isEqualTo("fouls_committed_per_match");
      assertThat(MetricCatalog.canonicalKey("SUBON")).isEqualTo("substitute_appearances_total");
      assertThatThrownBy(() -> MetricCatalog.canonicalKey("unknownMetric"))
          .isInstanceOf(InvalidPlayerException.class);
      assertThatThrownBy(() -> MetricCatalog.definitions().clear())
          .isInstanceOf(UnsupportedOperationException.class);
    }
  }
}
