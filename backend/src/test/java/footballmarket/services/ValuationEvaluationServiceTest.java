package footballmarket.services;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import footballmarket.models.enums.ValuationExclusionReason;
import footballmarket.services.exceptions.ValuationSnapshotUnavailableException;
import footballmarket.support.IdentityCatalogueFixture;
import footballmarket.support.ResetPlayerCatalogListener;
import footballmarket.support.TestcontainersConfiguration;
import footballmarket.support.ValuationTestData;
import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestExecutionListeners;

@SpringBootTest
@ActiveProfiles("test")
@Import({
  TestcontainersConfiguration.class,
  IdentityCatalogueFixture.class,
  ValuationTestData.class
})
@TestExecutionListeners(
    listeners = ResetPlayerCatalogListener.class,
    mergeMode = TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS)
class ValuationEvaluationServiceTest {
  @Autowired private ValuationEvaluationService service;
  @Autowired private IdentityCatalogueFixture catalogue;
  @Autowired private ValuationTestData data;

  @Nested
  @DisplayName("Selección previa al recorrido poblacional")
  class SnapshotSelection {
    @Test
    @DisplayName("Rechaza antes de escribir cuando no existe campaña JSON confirmada")
    void rejectsMissingSnapshot() {
      assertThatThrownBy(service::evaluate)
          .isInstanceOf(ValuationSnapshotUnavailableException.class)
          .hasMessage("No existe un snapshot poblacional confirmado para evaluar");
    }

    @Test
    @DisplayName("Publica ambas estrategias para toda la campaña elegible")
    void publishesConfirmedPopulation() {
      // Arrange
      catalogue.prepare();
      data.prepareConfirmedJsonCampaign(LocalDate.of(2026, 10, 18));

      // Act
      footballmarket.models.records.ValuationEvaluationSummary result = service.evaluate();

      // Assert
      org.assertj.core.api.Assertions.assertThat(result.evaluatedPlayers()).isEqualTo(5);
      org.assertj.core.api.Assertions.assertThat(result.failedPlayers()).isZero();
    }

    @Test
    @DisplayName("Republica la misma fecha sin fallar ni duplicar resultados")
    void republishesSameValuationDate() {
      // Arrange
      catalogue.prepare();
      data.prepareConfirmedJsonCampaign(LocalDate.of(2026, 10, 18));
      service.evaluate();

      // Act
      footballmarket.models.records.ValuationEvaluationSummary result = service.evaluate();

      // Assert
      org.assertj.core.api.Assertions.assertThat(result.evaluatedPlayers()).isEqualTo(5);
      org.assertj.core.api.Assertions.assertThat(result.failedPlayers()).isZero();
    }
  }

  @Nested
  @DisplayName("Confirmación de la campaña vigente")
  class CampaignConfirmation {
    @Test
    @DisplayName("Rechaza cuando un item de la campaña quedó en fallo estructural")
    void rejectsStructurallyFailedCampaign() {
      // Arrange
      catalogue.prepare();
      data.prepareStructurallyFailedJsonCampaign(LocalDate.of(2026, 10, 18));

      // Act & Assert
      org.assertj.core.api.Assertions.assertThatThrownBy(service::evaluate)
          .isInstanceOf(ValuationSnapshotUnavailableException.class);
    }

    @Test
    @DisplayName(
        "Confirma una campaña terminada con items sin datos y excluye al elegible sin fila")
    void acceptsPartialCampaignAndExcludesMissingSnapshot() {
      // Arrange
      catalogue.prepare();
      data.preparePartialJsonCampaign(LocalDate.of(2026, 10, 18));

      // Act
      footballmarket.models.records.ValuationEvaluationSummary result = service.evaluate();

      // Assert
      org.assertj.core.api.Assertions.assertThat(result.evaluatedPlayers()).isEqualTo(3);
      org.assertj.core.api.Assertions.assertThat(result.failedPlayers()).isZero();
      org.assertj.core.api.Assertions.assertThat(
              result
                  .excludedPlayers()
                  .get(ValuationExclusionReason.MISSING_OR_UNCONFIRMED_SNAPSHOT))
          .isEqualTo(1L);
      org.assertj.core.api.Assertions.assertThat(
              result.excludedPlayers().get(ValuationExclusionReason.UNRESOLVED_LEAGUE_CONTEXT))
          .isEqualTo(1L);
    }
  }
}
