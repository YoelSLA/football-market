package footballmarket.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import footballmarket.integrations.FootballDataIntegration;
import footballmarket.integrations.FootballDataIntegration.CompetitionSnapshot;
import footballmarket.integrations.FootballDataIntegration.PlayerData;
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
    @DisplayName(
        "No pierde una presentación equivalente de mayor calidad al consolidar la referencia entre competiciones")
    void preserveCanonicalPresentationAcrossCompetitions() {
      // Arrange
      PlayerData upper =
          new PlayerData("1", "JOSE PEREZ", "Team", "League", "MIDFIELDER", null, null, "10");
      PlayerData natural =
          new PlayerData("1", "José Pérez", "Team", "League", "Midfielder", null, null, "10");
      CompetitionSnapshot empty =
          new CompetitionSnapshot(List.of(), 0, 0, List.of(), List.of(), List.of());
      CompetitionSnapshot first =
          new CompetitionSnapshot(List.of(upper), 1, 0, List.of(), List.of(), List.of());
      CompetitionSnapshot second =
          new CompetitionSnapshot(List.of(natural), 1, 0, List.of(), List.of(), List.of());
      when(footballDataIntegration.fetchCompetition(anyString())).thenReturn(empty);
      when(footballDataIntegration.fetchCompetition("PL")).thenReturn(first);
      when(footballDataIntegration.fetchCompetition("PD")).thenReturn(second);

      // Act
      PlayerSnapshot result = footballDataPlayerService.fetchSnapshot();

      // Assert
      assertThat(result.obtained()).isEqualTo(2);
      assertThat(result.discardedInvalid()).isZero();
      assertThat(result.players())
          .singleElement()
          .satisfies(
              player -> {
                assertThat(player.name()).isEqualTo("José Pérez");
                assertThat(player.position()).isEqualTo("Midfielder");
              });
    }

    @Test
    @DisplayName("Consolida jugadores repetidos por ID y conserva los contadores de origen")
    void consolidaPorIdConservandoLaPrimeraLigaYLosContadoresOriginales() {
      // Arrange
      PlayerData firstPlayer = new PlayerData("1", "N", "T", "First", "P", null, null, "10");
      PlayerData duplicatePlayer =
          new PlayerData("1", "Other", "T", "Second", "P", null, null, "20");
      PlayerData secondPlayer = new PlayerData("2", "N", "T", "Second", "P", null, null, "20");
      CompetitionSnapshot emptySnapshot =
          new CompetitionSnapshot(List.of(), 0, 0, List.of(), List.of(), List.of());
      CompetitionSnapshot firstSnapshot =
          new CompetitionSnapshot(List.of(firstPlayer), 2, 1, List.of(), List.of(), List.of());
      CompetitionSnapshot secondSnapshot =
          new CompetitionSnapshot(
              List.of(duplicatePlayer, secondPlayer), 2, 0, List.of(), List.of(), List.of());
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
                PlayerData player = new PlayerData("1", "N", "T", code, "P", null, null, "10");
                List<PlayerData> players = List.of(player);
                return new CompetitionSnapshot(players, 1, 0, List.of(), List.of(), List.of());
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
                PlayerData player =
                    new PlayerData(
                        Long.toString(index + 1L), "N", "T", code, "P", null, null, "10");
                List<PlayerData> players = List.of(player);
                return new CompetitionSnapshot(players, 1, 0, List.of(), List.of(), List.of());
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
