package footballmarket.services;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import footballmarket.config.FootballDataProperties;
import footballmarket.integrations.footballdata.FootballDataIntegration;
import footballmarket.integrations.footballdata.FootballDataUnavailableException;
import footballmarket.models.Player;
import footballmarket.models.PlayerSnapshot;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
class FootballDataPlayerServiceTest {
  private final FootballDataIntegration integration = mock(FootballDataIntegration.class);
  private final FootballDataPlayerService service =
      new FootballDataPlayerService(
          integration,
          new FootballDataProperties("test-only", "https://provider.example", List.of("PL", "PD")));

  @Test
  void consolidatesByIdKeepingFirstCompetitionAndRawCounts() {
    when(integration.fetchCompetition("PL"))
        .thenReturn(new PlayerSnapshot(List.of(new Player(1L, "N", "T", "First", "P")), 2, 1));
    when(integration.fetchCompetition("PD"))
        .thenReturn(
            new PlayerSnapshot(
                List.of(
                    new Player(1L, "Other", "T", "Second", "P"),
                    new Player(2L, "N", "T", "Second", "P")),
                2,
                0));
    var result = service.fetchSnapshot();
    assertThat(result.obtained()).isEqualTo(4);
    assertThat(result.discardedInvalid()).isEqualTo(1);
    assertThat(result.players()).extracting(Player::getId).containsExactly(1L, 2L);
    assertThat(result.players().getFirst().getLeague()).isEqualTo("First");
    var order = inOrder(integration);
    order.verify(integration).fetchCompetition("PL");
    order.verify(integration).fetchCompetition("PD");
  }

  @Test
  void propagatesFailureOfLaterCompetition() {
    when(integration.fetchCompetition("PL")).thenReturn(new PlayerSnapshot(List.of(), 0, 0));
    when(integration.fetchCompetition("PD")).thenThrow(new FootballDataUnavailableException());
    assertThatThrownBy(service::fetchSnapshot).isInstanceOf(FootballDataUnavailableException.class);
  }
}
