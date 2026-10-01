package footballmarket.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import footballmarket.integrations.FootballDataIntegration;
import footballmarket.integrations.exceptions.FootballDataUnavailableException;
import footballmarket.models.records.PlayerCandidate;
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
    @DisplayName("Consolida jugadores repetidos por ID y conserva los contadores de origen")
    void consolidaPorIdConservandoLaPrimeraLigaYLosContadoresOriginales() {
      // Arrange
      PlayerCandidate firstPlayer = new PlayerCandidate("1", "N", "T", "First", "P", null, null);
      PlayerCandidate duplicatePlayer =
          new PlayerCandidate("1", "Other", "T", "Second", "P", null, null);
      PlayerCandidate secondPlayer = new PlayerCandidate("2", "N", "T", "Second", "P", null, null);
      PlayerSnapshot emptySnapshot = new PlayerSnapshot(List.of(), 0, 0);
      PlayerSnapshot firstSnapshot = new PlayerSnapshot(List.of(firstPlayer), 2, 1);
      PlayerSnapshot secondSnapshot =
          new PlayerSnapshot(List.of(duplicatePlayer, secondPlayer), 2, 0);
      for (String code : List.of("BL1", "SA", "FL1")) {
        when(footballDataIntegration.fetchCompetition(code)).thenReturn(emptySnapshot);
      }
      when(footballDataIntegration.fetchCompetition("PL")).thenReturn(firstSnapshot);
      when(footballDataIntegration.fetchCompetition("PD")).thenReturn(secondSnapshot);

      // Act
      PlayerSnapshot result = footballDataPlayerService.fetchSnapshot();

      // Assert
      assertThat(result.obtained()).isEqualTo(4);
      assertThat(result.discardedInvalid()).isEqualTo(1);
      assertThat(result.players())
          .extracting(PlayerCandidate::externalId)
          .containsExactly("1", "2");
      assertThat(result.players().getFirst().league()).isEqualTo("First");
    }

    @ParameterizedTest
    @ValueSource(strings = {"PL", "BL1", "PD", "SA", "FL1"})
    @DisplayName("Propaga el fallo de cualquiera de las ligas requeridas")
    void propagaElFalloDeCualquierLigaRequerida(String failedLeague) {
      // Arrange
      when(footballDataIntegration.fetchCompetition(anyString()))
          .thenAnswer(
              invocation -> {
                String code = invocation.getArgument(0);
                if (code.equals(failedLeague)) {
                  throw new FootballDataUnavailableException();
                }
                PlayerCandidate player = new PlayerCandidate("1", "N", "T", code, "P", null, null);
                List<PlayerCandidate> players = List.of(player);
                return new PlayerSnapshot(players, 1, 0);
              });

      // Act / Assert
      assertThatThrownBy(footballDataPlayerService::fetchSnapshot)
          .isInstanceOf(FootballDataUnavailableException.class);
    }

    @Test
    @DisplayName("Incluye las cinco ligas en el orden configurado")
    void incluyeLasCincoLigasEnElOrdenConfigurado() {
      // Arrange
      List<String> codes = List.of("PL", "BL1", "PD", "SA", "FL1");
      when(footballDataIntegration.fetchCompetition(anyString()))
          .thenAnswer(
              invocation -> {
                String code = invocation.getArgument(0);
                int index = codes.indexOf(code);
                assertThat(index).isNotNegative();
                PlayerCandidate player =
                    new PlayerCandidate(Long.toString(index + 1L), "N", "T", code, "P", null, null);
                List<PlayerCandidate> players = List.of(player);
                return new PlayerSnapshot(players, 1, 0);
              });
      // Act
      PlayerSnapshot snapshot = footballDataPlayerService.fetchSnapshot();
      // Assert
      assertThat(snapshot.players())
          .extracting(PlayerCandidate::league)
          .containsExactly("PL", "BL1", "PD", "SA", "FL1");
      assertThat(snapshot.obtained()).isEqualTo(5);
    }
  }
}
