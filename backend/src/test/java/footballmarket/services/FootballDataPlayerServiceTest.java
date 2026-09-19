package footballmarket.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.when;

import footballmarket.integrations.FootballDataIntegration;
import footballmarket.integrations.exceptions.FootballDataUnavailableException;
import footballmarket.models.Player;
import footballmarket.models.records.PlayerSnapshot;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
@ActiveProfiles("test")
class FootballDataPlayerServiceTest {

  @Autowired private FootballDataPlayerService footballDataPlayerService;
  @MockitoBean private FootballDataIntegration footballDataIntegration;

  @Test
  void consolidatesByIdKeepingFirstCompetitionAndRawCounts() {
    when(this.footballDataIntegration.fetchCompetition("PL"))
        .thenReturn(new PlayerSnapshot(List.of(new Player(1L, "N", "T", "First", "P")), 2, 1));
    when(this.footballDataIntegration.fetchCompetition("PD"))
        .thenReturn(
            new PlayerSnapshot(
                List.of(
                    new Player(1L, "Other", "T", "Second", "P"),
                    new Player(2L, "N", "T", "Second", "P")),
                2,
                0));

    var result = this.footballDataPlayerService.fetchSnapshot();

    assertThat(result.obtained()).isEqualTo(4);
    assertThat(result.discardedInvalid()).isEqualTo(1);
    assertThat(result.players()).extracting(Player::getId).containsExactly(1L, 2L);
    assertThat(result.players().getFirst().getLeague()).isEqualTo("First");
    var order = inOrder(this.footballDataIntegration);
    order.verify(this.footballDataIntegration).fetchCompetition("PL");
    order.verify(this.footballDataIntegration).fetchCompetition("PD");
  }

  @Test
  void propagatesFailureOfLaterCompetition() {
    when(this.footballDataIntegration.fetchCompetition("PL"))
        .thenReturn(new PlayerSnapshot(List.of(), 0, 0));
    when(this.footballDataIntegration.fetchCompetition("PD"))
        .thenThrow(new FootballDataUnavailableException());

    assertThatThrownBy(this.footballDataPlayerService::fetchSnapshot)
        .isInstanceOf(FootballDataUnavailableException.class);
  }
}
