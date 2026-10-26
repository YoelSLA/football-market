package footballmarket.repositories;

import static org.assertj.core.api.Assertions.assertThat;

import footballmarket.models.Player;
import footballmarket.support.PlayerCatalogQueryData;
import footballmarket.support.ResetPlayerCatalogListener;
import footballmarket.support.TestcontainersConfiguration;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.*;
import org.springframework.test.context.*;

@SpringBootTest
@ActiveProfiles("test")
@Import({TestcontainersConfiguration.class, PlayerCatalogQueryData.class})
@TestExecutionListeners(
    listeners = ResetPlayerCatalogListener.class,
    mergeMode = TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS)
@Tag("integration")
class PlayerCatalogQueryRepositoryTest {
  @Autowired private PlayerRepository repository;
  @Autowired private PlayerCatalogQueryData data;

  @Nested
  @DisplayName("Filtros globales del catálogo")
  class Query {
    @Test
    @DisplayName("El conteo global y el desempate por ID se conservan entre más de dos páginas")
    void pagesGlobalResults() {
      // Arrange
      PlayerCatalogQueryData.Fixture first = data.player("Ana", "Forward", "Spain", null);
      for (int index = 0; index < 26; index++) {
        data.player("Ana", "Forward", "Spain", first.teamId());
      }
      Sort sort = Sort.by(Sort.Order.asc("name").ignoreCase(), Sort.Order.asc("id"));
      // Act
      Page<Player> page =
          repository.findCatalog(
              "ANA", first.leagueId(), null, null, null, PageRequest.of(2, 12, sort));
      Page<Player> outside =
          repository.findCatalog(
              "ana", first.leagueId(), null, null, null, PageRequest.of(3, 12, sort));
      // Assert
      assertThat(page.getTotalElements()).isEqualTo(27);
      assertThat(page.getTotalPages()).isEqualTo(3);
      assertThat(page.getContent()).hasSize(3);
      assertThat(page.getContent()).extracting(Player::getId).isSorted();
      assertThat(outside.getContent()).isEmpty();
      assertThat(outside.getTotalElements()).isEqualTo(27);
    }

    @Test
    @DisplayName("Busca sin acentos y aplica todos los criterios antes del conteo y la página")
    void filtersBeforePagination() {
      // Arrange
      PlayerCatalogQueryData.Fixture fixture =
          data.player("José Álvarez", "Midfield", "Spain", null);
      data.player("Otro", "Forward", "France", null);
      data.player("JOSÉ Segundo", "Midfield", "Spain", fixture.teamId());
      // Act
      Page<Player> result =
          repository.findCatalog(
              "jose",
              fixture.leagueId(),
              fixture.teamId(),
              "Midfield",
              "Spain",
              PageRequest.of(0, 1, Sort.by("id")));
      // Assert
      assertThat(result.getTotalElements()).isEqualTo(2);
      assertThat(result.getTotalPages()).isEqualTo(2);
      assertThat(result.getContent())
          .singleElement()
          .satisfies(player -> assertThat(player.getName()).isEqualTo("José Álvarez"));
      assertThat(
              repository
                  .findCatalog("alvarez", null, null, null, null, PageRequest.of(0, 12))
                  .getTotalElements())
          .isEqualTo(1);
    }

    @Test
    @DisplayName("Trata comodines de entrada como caracteres literales")
    void escapesLiteralWildcards() {
      // Arrange
      data.player("Literal_%!", "Forward", null, null);
      data.player("Literal ABC", "Forward", null, null);
      // Act
      Page<Player> result =
          repository.findCatalog("_%!", null, null, null, null, PageRequest.of(0, 12));
      // Assert
      assertThat(result.getTotalElements()).isEqualTo(1);
      assertThat(result.getContent())
          .singleElement()
          .satisfies(player -> assertThat(player.getName()).isEqualTo("Literal_%!"));
    }

    @Test
    @DisplayName("Un filtro incompatible devuelve vacío sin cambiar opciones globales")
    void preservesGlobalOptions() {
      // Arrange
      PlayerCatalogQueryData.Fixture first = data.player("Primero", "Goalkeeper", "Spain", null);
      PlayerCatalogQueryData.Fixture second = data.player("Segundo", "Forward", "France", null);
      // Act
      Page<Player> result =
          repository.findCatalog(
              null, first.leagueId(), second.teamId(), null, null, PageRequest.of(0, 12));
      // Assert
      assertThat(result).isEmpty();
      assertThat(repository.findCatalogTeams()).hasSize(2);
      assertThat(repository.findCatalogNationalities())
          .containsExactlyInAnyOrder("Spain", "France");
    }
  }
}
