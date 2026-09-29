package footballmarket.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import footballmarket.integrations.FootballDataIntegration;
import footballmarket.integrations.exceptions.FootballDataUnavailableException;
import footballmarket.models.Player;
import footballmarket.models.records.PlayerSnapshot;
import footballmarket.support.TestcontainersConfiguration;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
class FootballDataPlayerServiceTest {

  @Autowired private FootballDataPlayerService footballDataPlayerService;
  @MockitoBean private FootballDataIntegration footballDataIntegration;

  @Nested
  @DisplayName("Obtención del catálogo de las ligas configuradas")
  class Snapshot {
    @Test
    void consolidaPorIdConservandoLaPrimeraLigaYLosContadoresOriginales() {
      for (String code : List.of("BL1", "SA", "FL1")) {
        when(footballDataIntegration.fetchCompetition(code))
            .thenReturn(new PlayerSnapshot(List.of(), 0, 0));
      }
      when(footballDataIntegration.fetchCompetition("PL"))
          .thenReturn(new PlayerSnapshot(List.of(new Player(1L, "N", "T", "First", "P")), 2, 1));
      when(footballDataIntegration.fetchCompetition("PD"))
          .thenReturn(
              new PlayerSnapshot(
                  List.of(
                      new Player(1L, "Other", "T", "Second", "P"),
                      new Player(2L, "N", "T", "Second", "P")),
                  2,
                  0));

      var result = footballDataPlayerService.fetchSnapshot();

      assertThat(result.obtained()).isEqualTo(4);
      assertThat(result.discardedInvalid()).isEqualTo(1);
      assertThat(result.players()).extracting(Player::getId).containsExactly(1L, 2L);
      assertThat(result.players().getFirst().getLeague()).isEqualTo("First");
    }

    @ParameterizedTest
    @ValueSource(strings = {"PL", "BL1", "PD", "SA", "FL1"})
    void propagaElFalloDeCualquierLigaRequerida(String failedLeague) {
      when(footballDataIntegration.fetchCompetition(anyString()))
          .thenAnswer(
              invocation -> {
                String code = invocation.getArgument(0);
                if (code.equals(failedLeague)) {
                  throw new FootballDataUnavailableException();
                }
                return new PlayerSnapshot(List.of(new Player(1L, "N", "T", code, "P")), 1, 0);
              });

      assertThatThrownBy(footballDataPlayerService::fetchSnapshot)
          .isInstanceOf(FootballDataUnavailableException.class);
    }

    @Test
    void incluyeLasCincoLigasEnElOrdenConfigurado() {
      var codes = List.of("PL", "BL1", "PD", "SA", "FL1");
      when(footballDataIntegration.fetchCompetition(anyString()))
          .thenAnswer(
              invocation -> {
                String code = invocation.getArgument(0);
                int index = codes.indexOf(code);
                assertThat(index).isNotNegative();
                return new PlayerSnapshot(
                    List.of(new Player(index + 1L, "N", "T", code, "P")), 1, 0);
              });
      var snapshot = footballDataPlayerService.fetchSnapshot();
      assertThat(snapshot.players())
          .extracting(Player::getLeague)
          .containsExactly("PL", "BL1", "PD", "SA", "FL1");
      assertThat(snapshot.obtained()).isEqualTo(5);
    }
  }
}
