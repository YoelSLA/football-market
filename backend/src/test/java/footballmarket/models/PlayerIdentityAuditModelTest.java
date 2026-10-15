package footballmarket.models;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import footballmarket.models.enums.PlayerIdentityItemResult;
import footballmarket.models.enums.PlayerIdentityPassType;
import footballmarket.models.enums.PlayerIdentityRunStatus;
import footballmarket.models.exceptions.InvalidPlayerException;
import footballmarket.models.records.PlayerIdentitySyncCounters;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class PlayerIdentityAuditModelTest {
  private static final Instant START = Instant.parse("2026-10-14T12:00:00Z");

  @Nested
  @DisplayName("Finalización independiente de cobertura")
  class Completion {
    @ParameterizedTest
    @EnumSource(
        value = PlayerIdentityItemResult.class,
        names = {"NO_MATCH", "AMBIGUOUS", "CONFLICT", "LOCAL_DATA_ERROR"})
    @DisplayName("Una decisión local no técnica permite completar sin exigir cobertura")
    void completesWithFinalLocalDecisions(PlayerIdentityItemResult result) {
      // Arrange
      PlayerIdentitySyncRun run =
          new PlayerIdentitySyncRun(PlayerIdentityPassType.CURRENT, null, START);
      PlayerIdentitySyncCounters counters =
          PlayerIdentitySyncCounters.from(Map.of(result, 1L), 2, 0, 3);
      // Act
      run.finish(counters, 5, START.plusSeconds(1), null);
      // Assert
      assertThat(run.getStatus()).isEqualTo(PlayerIdentityRunStatus.COMPLETED);
      assertThat(run.getCounters().selectedPlayers()).isEqualTo(1);
      assertThat(run.getCounters().discardedRows()).isEqualTo(3);
    }

    @Test
    @DisplayName("Una liga fallida degrada el estado aunque todos los jugadores tengan resultado")
    void marksPartialDiscovery() {
      // Arrange
      PlayerIdentitySyncRun run =
          new PlayerIdentitySyncRun(PlayerIdentityPassType.CURRENT, null, START);
      PlayerIdentitySyncCounters counters =
          PlayerIdentitySyncCounters.from(Map.of(PlayerIdentityItemResult.NO_MATCH, 1L), 0, 1, 0);
      // Act
      run.finish(counters, 4, START.plusSeconds(1), null);
      // Assert
      assertThat(run.getStatus()).isEqualTo(PlayerIdentityRunStatus.PARTIAL);
    }

    @Test
    @DisplayName("Un vacío sin trabajo completa sin descubrir ámbitos")
    void completesEmptyRun() {
      // Arrange
      PlayerIdentitySyncRun run =
          new PlayerIdentitySyncRun(PlayerIdentityPassType.CURRENT, null, START);
      PlayerIdentitySyncCounters counters = PlayerIdentitySyncCounters.from(Map.of(), 0, 0, 0);
      // Act
      run.finish(counters, 0, START, null);
      // Assert
      assertThat(run.getStatus()).isEqualTo(PlayerIdentityRunStatus.COMPLETED);
      assertThat(run.getFinishedAt()).isEqualTo(START);
    }

    @Test
    @DisplayName("Sin ámbitos útiles ni decisiones no técnicas el trabajo fallido es FAILED")
    void failsWithoutUsefulWork() {
      // Arrange
      PlayerIdentitySyncRun run =
          new PlayerIdentitySyncRun(PlayerIdentityPassType.CURRENT, null, START);
      PlayerIdentitySyncCounters counters =
          PlayerIdentitySyncCounters.from(
              Map.of(PlayerIdentityItemResult.DISCOVERY_ERROR, 1L), 0, 5, 0);
      // Act
      run.finish(counters, 0, START, null);
      // Assert
      assertThat(run.getStatus()).isEqualTo(PlayerIdentityRunStatus.FAILED);
    }

    @Test
    @DisplayName("Rechaza cerrar con pendientes y no muta RUNNING")
    void rejectsPendingClosure() {
      // Arrange
      PlayerIdentitySyncRun run =
          new PlayerIdentitySyncRun(PlayerIdentityPassType.CURRENT, null, START);
      // Act / Assert
      assertThatThrownBy(
              () ->
                  PlayerIdentitySyncCounters.from(
                      Map.of(PlayerIdentityItemResult.PENDING, 1L), 0, 0, 0))
          .isInstanceOf(InvalidPlayerException.class);
      // Verify
      assertThat(run.getStatus()).isEqualTo(PlayerIdentityRunStatus.RUNNING);
      assertThat(run.getFinishedAt()).isNull();
    }
  }

  @Nested
  @DisplayName("Evidencia y origen")
  class Evidence {
    @Test
    @DisplayName(
        "Un ámbito no puede completarse antes de terminar sus páginas ni cambiar después de finalizar")
    void enforcesLeagueTransitions() {
      // Arrange
      PlayerIdentityLeagueDiscovery league = new PlayerIdentityLeagueDiscovery(1L, 1L, "League", 4);
      league.start(START);
      league.recordPage(1, 2, 4, 3, 1);
      // Act / Assert
      assertThatThrownBy(() -> league.complete(START)).isInstanceOf(InvalidPlayerException.class);
      // Act
      league.recordPage(2, 2, 4, 1, 0);
      league.complete(START.plusSeconds(1));
      // Assert
      assertThat(league.getDiscardedRows()).isEqualTo(1);
      assertThat(league.getRowsRead()).isEqualTo(4);
      assertThatThrownBy(() -> league.start(START)).isInstanceOf(InvalidPlayerException.class);
    }

    @Test
    @DisplayName("Rechaza contadores negativos y un fin anterior al inicio sin cierre parcial")
    void rejectsInvalidCountersAndTimes() {
      // Arrange
      PlayerIdentitySyncRun run =
          new PlayerIdentitySyncRun(PlayerIdentityPassType.CURRENT, null, START);
      PlayerIdentitySyncCounters empty = PlayerIdentitySyncCounters.from(Map.of(), 0, 0, 0);
      // Act / Assert
      assertThatThrownBy(
              () ->
                  PlayerIdentitySyncCounters.from(
                      Map.of(PlayerIdentityItemResult.NO_MATCH, -1L), 0, 0, 0))
          .isInstanceOf(InvalidPlayerException.class);
      assertThatThrownBy(() -> run.finish(empty, 0, START.minusSeconds(1), null))
          .isInstanceOf(InvalidPlayerException.class);
      // Verify
      assertThat(run.getStatus()).isEqualTo(PlayerIdentityRunStatus.RUNNING);
    }

    @Test
    @DisplayName("Rechaza candidatos de otra ejecución sin mutar la decisión")
    void rejectsForeignCandidate() {
      // Arrange
      PlayerIdentitySyncItem item =
          new PlayerIdentitySyncItem(1L, 2L, null, null, null, 1L, "League", List.of("10", "11"));
      WhoScoredIdentityCandidate candidate = new WhoScoredIdentityCandidate(2L, "123");
      // Act / Assert
      assertThatThrownBy(
              () ->
                  item.decide(
                      PlayerIdentityItemResult.CONFLICT,
                      "MULTIPLE_INITIAL_REFERENCES",
                      "Conflicto",
                      List.of(candidate),
                      null,
                      START))
          .isInstanceOf(InvalidPlayerException.class);
      // Verify
      assertThat(item.getResult()).isEqualTo(PlayerIdentityItemResult.PENDING);
      assertThat(item.getInitialExternalIds()).containsExactly("10", "11");
      assertThat(item.getInitialExternalId()).isNull();
    }

    @Test
    @DisplayName("Un pase actual no acepta origen y un histórico lo exige")
    void validatesPassOrigin() {
      // Act / Assert
      assertThatThrownBy(() -> new PlayerIdentitySyncRun(PlayerIdentityPassType.CURRENT, 1L, START))
          .isInstanceOf(InvalidPlayerException.class);
      assertThatThrownBy(
              () -> new PlayerIdentitySyncRun(PlayerIdentityPassType.HISTORICAL, null, START))
          .isInstanceOf(InvalidPlayerException.class);
    }
  }
}
