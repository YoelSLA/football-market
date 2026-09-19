package footballmarket.models;

import static org.assertj.core.api.Assertions.*;

import footballmarket.models.exceptions.InvalidPlayerException;
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
    assertThatThrownBy(() -> new Player(null, "N", "T", "L", "P"))
        .isInstanceOf(InvalidPlayerException.class);
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = {" ", "\t"})
  void requiresAllTextFieldsAndDoesNotPartiallyUpdate(String invalid) {
    assertThatThrownBy(() -> new Player(1L, invalid, "T", "L", "P"))
        .isInstanceOf(InvalidPlayerException.class);
    assertThatThrownBy(() -> new Player(1L, "N", invalid, "L", "P"))
        .isInstanceOf(InvalidPlayerException.class);
    assertThatThrownBy(() -> new Player(1L, "N", "T", invalid, "P"))
        .isInstanceOf(InvalidPlayerException.class);
    assertThatThrownBy(() -> new Player(1L, "N", "T", "L", invalid))
        .isInstanceOf(InvalidPlayerException.class);
    Player player = new Player(1L, "N", "T", "L", "P");
    assertThatThrownBy(() -> player.update("Changed", "T", "L", invalid))
        .isInstanceOf(InvalidPlayerException.class);
    assertThat(player.getName()).isEqualTo("N");
  }
}
