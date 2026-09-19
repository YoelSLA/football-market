package footballmarket.models;

import static org.assertj.core.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
class PlayerSynchronizationResultTest {
  @ParameterizedTest
  @CsvSource({
    "-1,0,0,0,0",
    "1,-1,0,0,0",
    "1,0,-1,0,0",
    "1,0,0,-1,0",
    "1,0,0,0,-1",
    "1,1,1,0,0",
    "1,0,0,0,2"
  })
  void rejectsImpossibleCounters(
      int obtained, int created, int updated, int inactive, int discarded) {
    assertThatIllegalArgumentException()
        .isThrownBy(
            () -> new PlayerSynchronizationResult(obtained, created, updated, inactive, discarded));
  }

  @Test
  void permitsDuplicateObtainedRecordsAndIndependentInactivations() {
    assertThat(new PlayerSynchronizationResult(5, 1, 1, 10, 1).markedInactive()).isEqualTo(10);
  }

  @Test
  void snapshotRejectsInconsistentCountsAndProtectsMembership() {
    Player player = new Player(1L, "N", "T", "L", "P");
    assertThatIllegalArgumentException()
        .isThrownBy(() -> new PlayerSnapshot(List.of(player), 0, 0));
    assertThatIllegalArgumentException().isThrownBy(() -> new PlayerSnapshot(List.of(), 0, 1));
    assertThatIllegalArgumentException().isThrownBy(() -> new PlayerSnapshot(List.of(), -1, 0));
    assertThatIllegalArgumentException().isThrownBy(() -> new PlayerSnapshot(List.of(), 0, -1));
    var mutable = new java.util.ArrayList<>(List.of(player));
    var snapshot = new PlayerSnapshot(mutable, 1, 0);
    mutable.clear();
    assertThat(snapshot.players()).containsExactly(player);
  }
}
