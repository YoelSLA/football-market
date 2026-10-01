package footballmarket.services;

import static org.assertj.core.api.Assertions.*;

import footballmarket.models.Player;
import footballmarket.models.records.PlayerCandidate;
import footballmarket.models.records.PlayerSnapshot;
import footballmarket.models.records.PlayerSynchronizationResult;
import footballmarket.services.exceptions.PlayerSynchronizationPersistenceException;
import footballmarket.support.ResetPlayerCatalogListener;
import footballmarket.support.TestcontainersConfiguration;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestExecutionListeners;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@TestExecutionListeners(
    listeners = ResetPlayerCatalogListener.class,
    mergeMode = TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS)
class PlayerCatalogServiceTest {
  @Autowired private PlayerCatalogService playerCatalogService;

  private PlayerCandidate candidate(String id, String name) {
    return new PlayerCandidate(id, name, "Team", "League", "Forward", null, null);
  }

  private PlayerSnapshot snapshot(PlayerCandidate... candidates) {
    return new PlayerSnapshot(List.of(candidates), candidates.length, 0);
  }

  @Nested
  @DisplayName("Sincronización del catálogo")
  class Synchronization {
    @Test
    @DisplayName("Crea un jugador con identidad interna y opcionales desconocidos")
    void creaJugadoresNuevos() {
      PlayerCandidate player = candidate("91001", "New Player");
      PlayerSnapshot photo = snapshot(player);
      PlayerSynchronizationResult result = playerCatalogService.applySynchronization(photo);
      assertThat(result.created()).isEqualTo(1);
      assertThat(result.updated()).isZero();
      assertThat(result.markedInactive()).isZero();
      assertThat(playerCatalogService.getActivePlayers(0, 20).getContent())
          .singleElement()
          .satisfies(
              local -> {
                assertThat(local.getId()).isNotNull();
                assertThat(local.getName()).isEqualTo("New Player");
                assertThat(local.getDateOfBirth()).isNull();
                assertThat(local.getNationality()).isNull();
                assertThat(local.getImageUrl()).isNull();
              });
    }

    @Test
    @DisplayName("Resuelve por referencia y actualiza sin cambiar el identificador interno")
    void actualizaJugadoresExistentes() {
      PlayerCandidate original = candidate("91002", "Old");
      playerCatalogService.applySynchronization(snapshot(original));
      Long internalId =
          playerCatalogService.getActivePlayers(0, 20).getContent().getFirst().getId();
      PlayerCandidate changed =
          new PlayerCandidate("91002", "New", "New Team", "New League", "Goalkeeper", null, null);
      PlayerSynchronizationResult result =
          playerCatalogService.applySynchronization(snapshot(changed));
      assertThat(result.created()).isZero();
      assertThat(result.updated()).isEqualTo(1);
      assertThat(playerCatalogService.getActivePlayers(0, 20).getContent())
          .singleElement()
          .satisfies(
              local -> {
                assertThat(local.getId()).isEqualTo(internalId);
                assertThat(local.getName()).isEqualTo("New");
                assertThat(local.getTeam()).isEqualTo("New Team");
                assertThat(local.getLeague()).isEqualTo("New League");
                assertThat(local.getPosition()).isEqualTo("Goalkeeper");
              });
    }

    @Test
    @DisplayName("Reactiva por la misma referencia sin crear otro jugador")
    void reactivaJugadorInactivoCuandoReaparece() {
      PlayerCandidate player = candidate("91003", "Player");
      playerCatalogService.applySynchronization(snapshot(player));
      Long id = playerCatalogService.getActivePlayers(0, 20).getContent().getFirst().getId();
      playerCatalogService.applySynchronization(snapshot());
      PlayerSynchronizationResult result =
          playerCatalogService.applySynchronization(snapshot(player));
      assertThat(result.created()).isZero();
      assertThat(result.updated()).isEqualTo(1);
      assertThat(playerCatalogService.getActivePlayers(0, 20).getContent())
          .extracting(Player::getId)
          .containsExactly(id);
    }

    @Test
    @DisplayName("Inactiva únicamente al jugador ausente de la foto completa")
    void inactivaJugadoresActivosAusentesEnLaSincronizacion() {
      PlayerCandidate present = candidate("91004", "Present");
      PlayerCandidate absent = candidate("91005", "Absent");
      playerCatalogService.applySynchronization(snapshot(present, absent));
      PlayerSynchronizationResult result =
          playerCatalogService.applySynchronization(snapshot(present));
      assertThat(result.markedInactive()).isEqualTo(1);
      assertThat(playerCatalogService.getActivePlayers(0, 20).getContent())
          .extracting(Player::getName)
          .containsExactly("Present");
    }

    @Test
    @DisplayName("No vuelve a contar a un jugador que ya estaba inactivo")
    void noVuelveAContarComoInactivoUnJugadorQueYaEstabaInactivo() {
      PlayerCandidate active = candidate("91006", "Active");
      PlayerCandidate inactive = candidate("91007", "Inactive");
      playerCatalogService.applySynchronization(snapshot(active, inactive));
      playerCatalogService.applySynchronization(snapshot(active));
      PlayerSynchronizationResult result =
          playerCatalogService.applySynchronization(snapshot(active));
      assertThat(result.markedInactive()).isZero();
      assertThat(result.created()).isZero();
      assertThat(result.updated()).isEqualTo(1);
      assertThat(playerCatalogService.getActivePlayers(0, 20).getContent())
          .extracting(Player::getName)
          .containsExactly("Active");
    }

    @Test
    @DisplayName("Conserva los contadores de obtención y descarte de la fuente")
    void conservaLosContadoresOriginalesDelSnapshot() {
      PlayerCandidate player = candidate("91008", "Player");
      PlayerSnapshot photo = new PlayerSnapshot(List.of(player), 2, 1);
      PlayerSynchronizationResult result = playerCatalogService.applySynchronization(photo);
      assertThat(result.obtained()).isEqualTo(2);
      assertThat(result.discardedInvalid()).isEqualTo(1);
      assertThat(playerCatalogService.getActivePlayers(0, 20).getTotalElements()).isEqualTo(1);
    }

    @Test
    @DisplayName("Consolida un jugador repetido y mantiene la primera liga")
    void resuelveJugadoresDuplicadosSinCrearDosRegistros() {
      PlayerCandidate first = candidate("91009", "First");
      PlayerCandidate duplicate =
          new PlayerCandidate("91009", "Duplicate", "T", "Later", "P", null, null);
      PlayerSynchronizationResult result =
          playerCatalogService.applySynchronization(snapshot(first, duplicate));
      assertThat(result.created()).isEqualTo(1);
      assertThat(playerCatalogService.getActivePlayers(0, 20).getContent())
          .singleElement()
          .satisfies(
              local -> {
                assertThat(local.getName()).isEqualTo("First");
                assertThat(local.getLeague()).isEqualTo("League");
              });
    }

    @Test
    @DisplayName("Una foto completa vacía inactiva a los jugadores de Football-Data")
    void inactivaTodosLosJugadoresActivosAnteUnConjuntoCompletoVacio() {
      PlayerCandidate player = candidate("91010", "Player");
      playerCatalogService.applySynchronization(snapshot(player));
      PlayerSynchronizationResult result = playerCatalogService.applySynchronization(snapshot());
      assertThat(result.markedInactive()).isEqualTo(1);
      assertThat(playerCatalogService.getActivePlayers(0, 20)).isEmpty();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t"})
    @DisplayName("Los opcionales no informados no borran datos previamente conocidos")
    void conservaOpcionales(String nationality) {
      LocalDate birth = LocalDate.of(1990, 6, 20);
      PlayerCandidate original = new PlayerCandidate("44", "N", "T", "L", "P", birth, "Spain");
      playerCatalogService.applySynchronization(snapshot(original));
      PlayerCandidate degraded = new PlayerCandidate("44", "N", "T", "L", "P", null, nationality);
      playerCatalogService.applySynchronization(snapshot(degraded));
      assertThat(playerCatalogService.getActivePlayers(0, 20).getContent())
          .singleElement()
          .satisfies(
              local -> {
                assertThat(local.getDateOfBirth()).isEqualTo(birth);
                assertThat(local.getNationality()).isEqualTo("Spain");
                assertThat(local.getImageUrl()).isNull();
              });
    }

    @Test
    @DisplayName("Un valor opcional válido reemplaza el valor previamente conocido")
    void actualizaOpcionalesValidos() {
      PlayerCandidate original =
          new PlayerCandidate("44", "N", "T", "L", "P", LocalDate.of(1990, 1, 1), "Spain");
      playerCatalogService.applySynchronization(snapshot(original));
      PlayerCandidate changed =
          new PlayerCandidate("44", "N", "T", "L", "P", LocalDate.of(1991, 1, 1), "Argentina");
      playerCatalogService.applySynchronization(snapshot(changed));
      Player local = playerCatalogService.getActivePlayers(0, 20).getContent().getFirst();
      assertThat(local.getDateOfBirth()).isEqualTo(changed.dateOfBirth());
      assertThat(local.getNationality()).isEqualTo("Argentina");
    }

    @Test
    @DisplayName("Un fallo técnico revierte las escrituras previas sin convertirlas en descartes")
    void revierteAnteFalloTecnico() {
      PlayerCandidate existing = candidate("44", "Original");
      playerCatalogService.applySynchronization(snapshot(existing));
      PlayerCandidate changed = candidate("44", "Changed");
      PlayerCandidate tooLong = candidate("45", "X".repeat(256));
      assertThatThrownBy(
              () -> playerCatalogService.applySynchronization(snapshot(changed, tooLong)))
          .isInstanceOf(PlayerSynchronizationPersistenceException.class);
      assertThat(playerCatalogService.getActivePlayers(0, 20).getContent())
          .extracting(Player::getName)
          .containsExactly("Original");
    }
  }

  @Nested
  @DisplayName("Consulta paginada de jugadores activos")
  class Pagination {
    @Test
    @DisplayName("La segunda página devuelve el jugador restante y metadatos correctos")
    void devuelveLaPaginaSolicitada() {
      PlayerCandidate first = candidate("93001", "First");
      PlayerCandidate second = candidate("93002", "Second");
      PlayerCandidate third = candidate("93003", "Third");
      playerCatalogService.applySynchronization(snapshot(first, second, third));
      Page<Player> result = playerCatalogService.getActivePlayers(1, 2);
      assertThat(result.getContent()).extracting(Player::getName).containsExactly("Third");
      assertThat(result.getNumber()).isEqualTo(1);
      assertThat(result.getSize()).isEqualTo(2);
      assertThat(result.getTotalElements()).isEqualTo(3);
      assertThat(result.getTotalPages()).isEqualTo(2);
      assertThat(result.hasPrevious()).isTrue();
      assertThat(result.hasNext()).isFalse();
    }

    @Test
    @DisplayName("La consulta no expone jugadores inactivos")
    void noDevuelveJugadoresInactivos() {
      PlayerCandidate active = candidate("93007", "Active");
      PlayerCandidate absent = candidate("93008", "Absent");
      playerCatalogService.applySynchronization(snapshot(active, absent));
      playerCatalogService.applySynchronization(snapshot(active));
      Page<Player> result = playerCatalogService.getActivePlayers(0, 20);
      assertThat(result.getContent()).extracting(Player::getName).containsExactly("Active");
      assertThat(result.getTotalElements()).isEqualTo(1);
    }

    @Test
    @DisplayName("Una página vacía conserva los metadatos solicitados")
    void devuelveMetadatosDePaginaVacia() {
      Page<Player> result = playerCatalogService.getActivePlayers(0, 20);
      assertThat(result.getContent()).isEmpty();
      assertThat(result.getNumber()).isZero();
      assertThat(result.getSize()).isEqualTo(20);
      assertThat(result.getTotalElements()).isZero();
      assertThat(result.getTotalPages()).isZero();
    }
  }
}
