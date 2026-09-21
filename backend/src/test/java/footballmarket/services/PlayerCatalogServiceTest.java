package footballmarket.services;

import static org.assertj.core.api.Assertions.assertThat;

import footballmarket.models.Player;
import footballmarket.models.records.PlayerSnapshot;
import footballmarket.support.TestcontainersConfiguration;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@Transactional
class PlayerCatalogServiceTest {

  @Autowired private PlayerCatalogService playerCatalogService;

  @BeforeEach
  void clearActiveCatalog() {
    playerCatalogService.applySynchronization(new PlayerSnapshot(List.of(), 0, 0));
  }

  @Nested
  @DisplayName("Sincronización del catálogo")
  class Synchronization {
    @Test
    void creaActualizaReactivaEInactivaSoloJugadoresActivosAusentes() {
      long firstId = 91001L;
      long createdId = 91002L;
      long absentId = 91003L;
      playerCatalogService.applySynchronization(
          new PlayerSnapshot(
              List.of(
                  new Player(firstId, "Old", "T", "L", "P"),
                  new Player(absentId, "Absent", "T", "L", "P")),
              2,
              0));
      playerCatalogService.applySynchronization(
          new PlayerSnapshot(List.of(new Player(absentId, "Absent", "T", "L", "P")), 1, 0));

      var result =
          playerCatalogService.applySynchronization(
              new PlayerSnapshot(
                  List.of(
                      new Player(firstId, "New", "Team", "League", "Forward"),
                      new Player(createdId, "Created", "T", "L", "P"),
                      new Player(createdId, "Duplicate", "T", "Later", "P")),
                  4,
                  1));

      assertThat(result.created()).isEqualTo(1);
      assertThat(result.updated()).isEqualTo(1);
      assertThat(result.markedInactive()).isEqualTo(1);
      assertThat(result.obtained()).isEqualTo(4);
      assertThat(result.discardedInvalid()).isEqualTo(1);
      assertThat(playerCatalogService.getActivePlayers(0, 100).getContent())
          .extracting(Player::getId)
          .containsExactly(firstId, createdId);
      assertThat(playerCatalogService.getActivePlayers(0, 100).getContent())
          .filteredOn(player -> player.getId().equals(firstId))
          .extracting(Player::getName)
          .containsExactly("New");
    }

    @Test
    void inactivaLosJugadoresActivosAnteUnConjuntoCompletoVacio() {
      playerCatalogService.applySynchronization(
          new PlayerSnapshot(List.of(new Player(92003L, "Absent", "T", "L", "P")), 1, 0));

      var result = playerCatalogService.applySynchronization(new PlayerSnapshot(List.of(), 0, 0));

      assertThat(result.markedInactive()).isEqualTo(1);
      assertThat(result.created()).isZero();
      assertThat(result.updated()).isZero();
      assertThat(playerCatalogService.getActivePlayers(0, 20).getContent()).isEmpty();
    }
  }

  @Nested
  @DisplayName("Consulta paginada de jugadores activos")
  class Pagination {
    @Test
    void consultaSoloJugadoresActivosLocalesConPaginacionEstable() {
      playerCatalogService.applySynchronization(
          new PlayerSnapshot(
              List.of(
                  new Player(93001L, "First", "T", "L", "P"),
                  new Player(93002L, "Second", "T", "L", "P"),
                  new Player(93003L, "Third", "T", "L", "P")),
              3,
              0));

      var result = playerCatalogService.getActivePlayers(1, 2);

      assertThat(result.getContent()).extracting(Player::getId).containsExactly(93003L);
      assertThat(result.getSize()).isEqualTo(2);
      assertThat(result.getTotalElements()).isEqualTo(3);
    }

    @Test
    void devuelveMetadatosDePaginaVacia() {
      var result = playerCatalogService.getActivePlayers(0, 20);

      assertThat(result.getContent()).isEmpty();
      assertThat(result.getSize()).isEqualTo(20);
      assertThat(result.getTotalElements()).isZero();
    }
  }
}
