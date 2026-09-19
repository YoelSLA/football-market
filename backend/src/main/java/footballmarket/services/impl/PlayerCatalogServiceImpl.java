package footballmarket.services.impl;

import footballmarket.models.Player;
import footballmarket.models.records.PlayerSnapshot;
import footballmarket.models.records.PlayerSynchronizationResult;
import footballmarket.repositories.PlayerRepository;
import footballmarket.services.PlayerCatalogService;
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
@Transactional
@RequiredArgsConstructor
/** Implementación transaccional del catálogo local de jugadores. */
public class PlayerCatalogServiceImpl implements PlayerCatalogService {
  private final PlayerRepository playerRepository;

  /** {@inheritDoc} */
  @Override
  public PlayerSynchronizationResult applySynchronization(PlayerSnapshot snapshot) {

    Map<Long, Player> candidates = new LinkedHashMap<>();

    snapshot.players().forEach(player -> candidates.putIfAbsent(player.getId(), player));

    Map<Long, Player> existing =
        this.playerRepository.findAllById(candidates.keySet()).stream()
            .collect(Collectors.toMap(Player::getId, player -> player));

    int created = 0;
    int updated = 0;

    for (Player candidate : candidates.values()) {
      Player player = existing.get(candidate.getId());

      if (player == null) {
        this.playerRepository.save(
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
    for (Player player : this.playerRepository.findByActiveTrue()) {
      if (!candidates.containsKey(player.getId())) {
        player.deactivate();
        markedInactive++;
      }
    }

    return new PlayerSynchronizationResult(
        snapshot.obtained(), created, updated, markedInactive, snapshot.discardedInvalid());
  }

  @Transactional(readOnly = true)
  /** {@inheritDoc} */
  @Override
  public Page<Player> getActivePlayers(int page, int size) {
    return this.playerRepository.findByActiveTrue(PageRequest.of(page, size, Sort.by("id")));
  }
}
