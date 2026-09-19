package footballmarket.services;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import footballmarket.models.Player;
import footballmarket.models.PlayerSnapshot;
import footballmarket.repositories.PlayerRepository;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
class PlayerCatalogServiceTest {
  private final PlayerRepository repository = mock(PlayerRepository.class);
  private final PlayerCatalogService service = new PlayerCatalogService(repository);

  @Test
  void createsUpdatesReactivatesAndMarksOnlyMissingActivePlayers() {
    Player existing = new Player(1L, "Old", "T", "L", "P");
    existing.deactivate();
    Player absent = new Player(3L, "Absent", "T", "L", "P");
    when(repository.findAllById(any())).thenReturn(List.of(existing));
    when(repository.findByActiveTrue()).thenReturn(List.of(existing, absent));
    var result =
        service.applySynchronization(
            new PlayerSnapshot(
                List.of(
                    new Player(1L, "New", "Team", "League", "Forward"),
                    new Player(2L, "Created", "T", "L", "P"),
                    new Player(2L, "Duplicate", "T", "Later", "P")),
                4,
                1));
    assertThat(result.created()).isEqualTo(1);
    assertThat(result.updated()).isEqualTo(1);
    assertThat(result.markedInactive()).isEqualTo(1);
    assertThat(result.obtained()).isEqualTo(4);
    assertThat(result.discardedInvalid()).isEqualTo(1);
    assertThat(existing.isActive()).isTrue();
    assertThat(existing.getName()).isEqualTo("New");
    assertThat(absent.isActive()).isFalse();
    verify(repository)
        .save(
            argThat(
                player ->
                    player.getId().equals(2L)
                        && player.getName().equals("Created")
                        && player.isActive()));
    verify(repository, never()).delete(any(Player.class));
  }

  @Test
  void completeEmptySnapshotInactivatesRemainingActivePlayers() {
    Player absent = new Player(3L, "Absent", "T", "L", "P");
    when(repository.findByActiveTrue()).thenReturn(List.of(absent));
    var result = service.applySynchronization(new PlayerSnapshot(List.of(), 0, 0));
    assertThat(result.markedInactive()).isEqualTo(1);
    assertThat(result.created()).isZero();
    assertThat(result.updated()).isZero();
    assertThat(absent.isActive()).isFalse();
  }

  @Test
  void persistenceFailurePropagatesForTransactionRollback() {
    doThrow(new org.springframework.dao.DataIntegrityViolationException("test"))
        .when(repository)
        .flush();
    assertThatThrownBy(() -> service.applySynchronization(new PlayerSnapshot(List.of(), 0, 0)))
        .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
  }

  @Test
  void readsOnlyLocalActivePlayersWithStablePagination() {
    var request = PageRequest.of(1, 2, Sort.by("id"));
    var expected = new PageImpl<>(List.of(new Player(7L, "N", "T", "L", "P")), request, 3);
    when(repository.findByActiveTrue(request)).thenReturn(expected);
    assertThat(service.getActivePlayers(1, 2)).isSameAs(expected);
    verify(repository).findByActiveTrue(request);
    verifyNoMoreInteractions(repository);
  }

  @Test
  void returnsEmptyPageMetadata() {
    var request = PageRequest.of(0, 20, Sort.by("id"));
    when(repository.findByActiveTrue(request)).thenReturn(Page.empty(request));
    var result = service.getActivePlayers(0, 20);
    assertThat(result.getContent()).isEmpty();
    assertThat(result.getSize()).isEqualTo(20);
    assertThat(result.getTotalElements()).isZero();
  }
}
