package footballmarket.services;

import footballmarket.models.Player;
import footballmarket.models.PlayerSnapshot;
import footballmarket.models.PlayerSynchronizationResult;
import footballmarket.repositories.PlayerRepository;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PlayerCatalogService {
  private final PlayerRepository repository;

  /** Aplica una foto completa de forma atómica y conserva las filas ausentes como inactivas. */
  @Transactional
  public PlayerSynchronizationResult applySynchronization(PlayerSnapshot snapshot) {
    Map<Long, Player> candidates = new LinkedHashMap<>();
    snapshot.players().forEach(player -> candidates.putIfAbsent(player.getId(), player));
    Map<Long, Player> existing =
        repository.findAllById(candidates.keySet()).stream()
            .collect(Collectors.toMap(Player::getId, player -> player));
    int created = 0;
    int updated = 0;
    for (Player candidate : candidates.values()) {
      Player player = existing.get(candidate.getId());
      if (player == null) {
        repository.save(
            new Player(
                candidate.getId(),
                candidate.getName(),
                candidate.getTeam(),
                candidate.getLeague(),
                candidate.getPosition()));
        created++;
      } else {
        player.update(
            candidate.getName(),
            candidate.getTeam(),
            candidate.getLeague(),
            candidate.getPosition());
        player.activate();
        updated++;
      }
    }
    int markedInactive = 0;
    for (Player player : repository.findByActiveTrue()) {
      if (!candidates.containsKey(player.getId())) {
        player.deactivate();
        markedInactive++;
      }
    }
    repository.flush();
    return new PlayerSynchronizationResult(
        snapshot.obtained(), created, updated, markedInactive, snapshot.discardedInvalid());
  }

  @Transactional(readOnly = true)
  public Page<Player> getActivePlayers(int page, int size) {
    return repository.findByActiveTrue(PageRequest.of(page, size, Sort.by("id")));
  }
}
