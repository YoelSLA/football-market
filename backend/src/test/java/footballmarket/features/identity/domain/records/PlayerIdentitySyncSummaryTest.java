package footballmarket.features.identity.domain.records;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import footballmarket.features.catalog.domain.exceptions.InvalidPlayerException;
import footballmarket.features.identity.domain.enums.PlayerIdentityItemResult;
import footballmarket.features.identity.domain.enums.PlayerIdentityRunStatus;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@Tag("unit")
class PlayerIdentitySyncSummaryTest {
  @Nested
  @DisplayName("Agregación final de jugadores locales")
  class Totals {
    @Test
    @DisplayName("Agrupa referencias, ausencia de coincidencia y todos los casos sin resolver")
    void aggregatesFinalResults() {
      // Arrange
      List<PlayerIdentityItemResult> results =
          List.of(
              PlayerIdentityItemResult.RESOLVED,
              PlayerIdentityItemResult.ALREADY_REFERENCED,
              PlayerIdentityItemResult.NO_MATCH,
              PlayerIdentityItemResult.AMBIGUOUS,
              PlayerIdentityItemResult.CONFLICT,
              PlayerIdentityItemResult.LOCAL_DATA_ERROR,
              PlayerIdentityItemResult.DISCOVERY_ERROR,
              PlayerIdentityItemResult.PERSISTENCE_ERROR,
              PlayerIdentityItemResult.INTERRUPTED);
      // Act
      PlayerIdentitySyncSummary summary =
          PlayerIdentitySyncSummary.from(PlayerIdentityRunStatus.PARTIAL, results);
      // Assert
      assertThat(summary.selectedPlayers()).isEqualTo(9);
      assertThat(summary.referencedPlayers()).isEqualTo(2);
      assertThat(summary.noMatchPlayers()).isEqualTo(1);
      assertThat(summary.unresolvedPlayers()).isEqualTo(6);
    }

    @Test
    @DisplayName("Un universo vacío reconcilia con cuatro totales cero")
    void aggregatesEmptyResults() {
      // Act
      PlayerIdentitySyncSummary summary =
          PlayerIdentitySyncSummary.from(PlayerIdentityRunStatus.COMPLETED, List.of());
      // Assert
      assertThat(summary)
          .isEqualTo(new PlayerIdentitySyncSummary(PlayerIdentityRunStatus.COMPLETED, 0, 0, 0, 0));
    }

    @ParameterizedTest
    @CsvSource({
      "-1,0,0,0",
      "1,2,0,0",
      "1,0,2,0",
      "1,0,0,0",
      "1,0,0,-1",
      "9223372036854775807,9223372036854775807,1,0"
    })
    @DisplayName("Rechaza totales negativos o incompatibles sin aceptar overflow")
    void rejectsInvalidTotals(long selected, long referenced, long noMatch, long unresolved) {
      // Act / Assert
      assertThatThrownBy(
              () ->
                  new PlayerIdentitySyncSummary(
                      PlayerIdentityRunStatus.COMPLETED, selected, referenced, noMatch, unresolved))
          .isInstanceOf(InvalidPlayerException.class);
    }
  }

  @Nested
  @DisplayName("Estado global de ambos pases")
  class Status {
    @ParameterizedTest
    @CsvSource({
      "NO_MATCH,5,5,false,COMPLETED",
      "AMBIGUOUS,5,5,false,COMPLETED",
      "CONFLICT,0,0,false,COMPLETED",
      "LOCAL_DATA_ERROR,5,5,false,COMPLETED",
      "NO_MATCH,5,4,false,PARTIAL",
      "DISCOVERY_ERROR,5,0,false,FAILED",
      "PERSISTENCE_ERROR,5,5,false,PARTIAL",
      "INTERRUPTED,5,0,true,FAILED",
      "ALREADY_REFERENCED,0,0,true,PARTIAL"
    })
    @DisplayName("El estado del pase refleja completitud y errores, no cobertura de identidad")
    void classifiesPass(
        PlayerIdentityItemResult result,
        int expectedLeagues,
        int completedLeagues,
        boolean interrupted,
        PlayerIdentityRunStatus expected) {
      // Arrange
      List<PlayerIdentityItemResult> results = List.of(result);
      // Act / Assert
      assertThat(
              PlayerIdentityRunStatus.forPass(
                  results, expectedLeagues, completedLeagues, interrupted))
          .isEqualTo(expected);
    }

    @ParameterizedTest
    @CsvSource({
      "COMPLETED,COMPLETED,COMPLETED",
      "COMPLETED,PARTIAL,PARTIAL",
      "COMPLETED,FAILED,FAILED",
      "PARTIAL,COMPLETED,PARTIAL",
      "PARTIAL,PARTIAL,PARTIAL",
      "PARTIAL,FAILED,FAILED",
      "FAILED,COMPLETED,FAILED",
      "FAILED,PARTIAL,FAILED",
      "FAILED,FAILED,FAILED"
    })
    @DisplayName("Conserva prioridad de fallo sobre parcial y completado")
    void combinesStatuses(
        PlayerIdentityRunStatus current,
        PlayerIdentityRunStatus historical,
        PlayerIdentityRunStatus expected) {
      // Act / Assert
      assertThat(current.combine(historical)).isEqualTo(expected);
    }
  }
}
