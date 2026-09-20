package footballmarket.services;

import static org.assertj.core.api.Assertions.assertThat;

import footballmarket.models.Player;
import footballmarket.models.records.PlayerSnapshot;
import footballmarket.support.TestcontainersConfiguration;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
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
    this.playerCatalogService.applySynchronization(new PlayerSnapshot(List.of(), 0, 0));
  }

  @Test
  void createsUpdatesReactivatesAndMarksOnlyMissingActivePlayers() {
    long firstId = 91001L;
    long createdId = 91002L;
    long absentId = 91003L;
    this.playerCatalogService.applySynchronization(
        new PlayerSnapshot(
            List.of(
                new Player(firstId, "Old", "T", "L", "P"),
                new Player(absentId, "Absent", "T", "L", "P")),
            2,
            0));
    this.playerCatalogService.applySynchronization(
        new PlayerSnapshot(List.of(new Player(absentId, "Absent", "T", "L", "P")), 1, 0));

    var result =
        this.playerCatalogService.applySynchronization(
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
    assertThat(this.playerCatalogService.getActivePlayers(0, 100).getContent())
        .extracting(Player::getId)
        .containsExactly(firstId, createdId);
    assertThat(this.playerCatalogService.getActivePlayers(0, 100).getContent())
        .filteredOn(player -> player.getId().equals(firstId))
        .extracting(Player::getName)
        .containsExactly("New");
  }

  @Test
  void completeEmptySnapshotInactivatesRemainingActivePlayers() {
    this.playerCatalogService.applySynchronization(
        new PlayerSnapshot(List.of(new Player(92003L, "Absent", "T", "L", "P")), 1, 0));

    var result =
        this.playerCatalogService.applySynchronization(new PlayerSnapshot(List.of(), 0, 0));

    assertThat(result.markedInactive()).isEqualTo(1);
    assertThat(result.created()).isZero();
    assertThat(result.updated()).isZero();
    assertThat(this.playerCatalogService.getActivePlayers(0, 20).getContent()).isEmpty();
  }

  @Test
  void readsOnlyLocalActivePlayersWithStablePagination() {
    this.playerCatalogService.applySynchronization(
        new PlayerSnapshot(
            List.of(
                new Player(93001L, "First", "T", "L", "P"),
                new Player(93002L, "Second", "T", "L", "P"),
                new Player(93003L, "Third", "T", "L", "P")),
            3,
            0));

    var result = this.playerCatalogService.getActivePlayers(1, 2);

    assertThat(result.getContent()).extracting(Player::getId).containsExactly(93003L);
    assertThat(result.getSize()).isEqualTo(2);
    assertThat(result.getTotalElements()).isEqualTo(3);
  }

  @Test
  void returnsEmptyPageMetadata() {
    var result = this.playerCatalogService.getActivePlayers(0, 20);

    assertThat(result.getContent()).isEmpty();
    assertThat(result.getSize()).isEqualTo(20);
    assertThat(result.getTotalElements()).isZero();
  }
}
