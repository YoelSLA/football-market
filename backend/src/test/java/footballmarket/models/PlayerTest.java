package footballmarket.models;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
class PlayerTest {
  @Test
  void preservesExternalIdAndTransitions() {
    Player player = new Player(7L, "Player", "Team", "League", "Forward");
    assertThat(player.getId()).isEqualTo(7L);
    assertThat(player.isActive()).isTrue();
    player.deactivate();
    assertThat(player.isActive()).isFalse();
    player.update("New name", "New team", "New league", "Goalkeeper");
    assertThat(player.getName()).isEqualTo("New name");
    assertThat(player.getTeam()).isEqualTo("New team");
    assertThat(player.getLeague()).isEqualTo("New league");
    assertThat(player.getPosition()).isEqualTo("Goalkeeper");
    player.activate();
    assertThat(player.isActive()).isTrue();
    assertThat(player.getId()).isEqualTo(7L);
  }

  @Test
  void requiresId() {
    assertThatIllegalArgumentException().isThrownBy(() -> new Player(null, "N", "T", "L", "P"));
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = {" ", "\t"})
  void requiresAllTextFieldsAndDoesNotPartiallyUpdate(String invalid) {
    assertThatIllegalArgumentException().isThrownBy(() -> new Player(1L, invalid, "T", "L", "P"));
    assertThatIllegalArgumentException().isThrownBy(() -> new Player(1L, "N", invalid, "L", "P"));
    assertThatIllegalArgumentException().isThrownBy(() -> new Player(1L, "N", "T", invalid, "P"));
    assertThatIllegalArgumentException().isThrownBy(() -> new Player(1L, "N", "T", "L", invalid));
    Player player = new Player(1L, "N", "T", "L", "P");
    assertThatIllegalArgumentException()
        .isThrownBy(() -> player.update("Changed", "T", "L", invalid));
    assertThat(player.getName()).isEqualTo("N");
  }
}
