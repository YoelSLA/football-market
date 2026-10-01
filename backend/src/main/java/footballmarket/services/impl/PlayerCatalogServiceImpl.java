package footballmarket.services.impl;

import footballmarket.models.Player;
import footballmarket.models.PlayerExternalReference;
import footballmarket.models.PlayerProvider;
import footballmarket.models.records.PlayerCandidate;
import footballmarket.models.records.PlayerSnapshot;
import footballmarket.models.records.PlayerSynchronizationResult;
import footballmarket.repositories.PlayerExternalReferenceRepository;
import footballmarket.repositories.PlayerRepository;
import footballmarket.services.PlayerCatalogService;
import footballmarket.services.exceptions.PlayerSynchronizationPersistenceException;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.postgresql.util.PSQLException;
import org.postgresql.util.ServerErrorMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

/** Aplica una foto inmutable con un único commit y recuperación aislada de conflictos externos. */
@Service
public class PlayerCatalogServiceImpl implements PlayerCatalogService {
  private static final Logger LOG = LoggerFactory.getLogger(PlayerCatalogServiceImpl.class);
  private static final String REFERENCE_UNIQUE =
      "uk_player_external_references_provider_external_id";
  private final PlayerRepository playerRepository;
  private final PlayerExternalReferenceRepository referenceRepository;
  private final TransactionTemplate transaction;

  public PlayerCatalogServiceImpl(
      PlayerRepository playerRepository,
      PlayerExternalReferenceRepository referenceRepository,
      PlatformTransactionManager transactionManager) {
    this.playerRepository = playerRepository;
    this.referenceRepository = referenceRepository;
    this.transaction = new TransactionTemplate(transactionManager);
    this.transaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    this.transaction.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);
  }

  /** {@inheritDoc} */
  @Override
  public PlayerSynchronizationResult applySynchronization(PlayerSnapshot snapshot) {
    Map<String, PlayerCandidate> candidates = new LinkedHashMap<>();
    snapshot
        .players()
        .forEach(candidate -> candidates.putIfAbsent(candidate.externalId(), candidate));
    Set<String> excluded = new LinkedHashSet<>();
    Set<Long> protectedIds = new HashSet<>();
    // Solo claves y escalares sobreviven al rollback, nunca entidades administradas.
    for (int attempt = 0; attempt <= candidates.size(); attempt++) {
      Attempt current = new Attempt();
      try {
        PlayerSynchronizationResult result =
            this.transaction.execute(
                status -> this.applyAttempt(snapshot, candidates, excluded, protectedIds, current));
        if (result == null) {
          throw new PlayerSynchronizationPersistenceException();
        }
        LOG.info(
            "Sincronización confirmada: obtenidos={}, creados={}, actualizados={}, inactivos={}, descartados={}",
            result.obtained(),
            result.created(),
            result.updated(),
            result.markedInactive(),
            result.discardedInvalid());
        return result;
      } catch (RuntimeException failure) {
        // execute ya finalizó el rollback: nunca se continúa en el callback fallido.
        if (current.externalId != null
            && referenceConflict(failure)
            && excluded.add(current.externalId)) {
          LOG.warn("Jugador descartado: conflicto de referencia FOOTBALL_DATA");
          continue;
        }
        LOG.error("Sincronización fallida: error técnico de persistencia");
        throw new PlayerSynchronizationPersistenceException();
      }
    }
    throw new PlayerSynchronizationPersistenceException();
  }

  private PlayerSynchronizationResult applyAttempt(
      PlayerSnapshot snapshot,
      Map<String, PlayerCandidate> candidates,
      Set<String> excluded,
      Set<Long> protectedIds,
      Attempt current) {
    int created = 0;
    int updated = 0;
    Set<Long> protectedPlayers = new HashSet<>(protectedIds);
    Map<String, Long> expectedOwners = new LinkedHashMap<>();
    this.referenceRepository
        .findIdentitiesByProvider(PlayerProvider.FOOTBALL_DATA)
        .forEach(identity -> expectedOwners.put(identity.getExternalId(), identity.getPlayerId()));
    Map<String, PlayerExternalReference> resolved = new LinkedHashMap<>();
    // Preflight sin escrituras: una referencia existente normal siempre resuelve a su propietario.
    // Solo una asociación incompatible con la identidad previamente resuelta es conflicto lógico.
    for (String externalId : candidates.keySet()) {
      PlayerExternalReference reference =
          this.referenceRepository
              .findByProviderAndExternalId(PlayerProvider.FOOTBALL_DATA, externalId)
              .orElse(null);
      resolved.put(externalId, reference);
      Long expectedOwner = expectedOwners.get(externalId);
      if (expectedOwner != null
          && (reference == null || !expectedOwner.equals(reference.getPlayer().getId()))) {
        protectedIds.add(expectedOwner);
        if (reference != null) {
          protectedIds.add(reference.getPlayer().getId());
        }
        if (excluded.add(externalId)) {
          LOG.warn("Jugador descartado: conflicto lógico de referencia FOOTBALL_DATA");
        }
      }
    }
    protectedPlayers.addAll(protectedIds);
    for (String externalId : excluded) {
      PlayerExternalReference owner = resolved.get(externalId);
      if (owner != null) {
        protectedPlayers.add(owner.getPlayer().getId());
      }
    }
    for (PlayerCandidate candidate : candidates.values()) {
      if (excluded.contains(candidate.externalId())) {
        continue;
      }
      // Las lecturas previas ocurren sin candidato atribuible; solo su escritura puede descartarse.
      current.externalId = null;
      PlayerExternalReference reference = resolved.get(candidate.externalId());
      if (reference != null && protectedPlayers.contains(reference.getPlayer().getId())) {
        if (excluded.add(candidate.externalId())) {
          LOG.warn("Jugador descartado: identidad implicada en un conflicto FOOTBALL_DATA");
        }
        continue;
      }
      Player player;
      if (reference == null) {
        player =
            new Player(
                candidate.name(), candidate.team(), candidate.league(), candidate.position());
        player.addExternalReference(PlayerProvider.FOOTBALL_DATA, candidate.externalId());
      } else {
        player = reference.getPlayer();
        player.update(candidate.name(), candidate.team(), candidate.league(), candidate.position());
        player.activate();
      }
      player.updateOptionalDetails(candidate.dateOfBirth(), candidate.nationality());
      current.externalId = candidate.externalId();
      if (reference == null) {
        this.playerRepository.save(player);
        created++;
      } else {
        updated++;
      }
      this.playerRepository.flush();
      current.externalId = null;
    }
    int markedInactive = 0;
    List<PlayerExternalReference> footballReferences =
        this.referenceRepository.findByProvider(PlayerProvider.FOOTBALL_DATA);
    for (PlayerExternalReference reference : footballReferences) {
      Player player = reference.getPlayer();
      // Si otro alias del mismo jugador estuvo presente, tampoco debe inactivarse.
      if (candidates.containsKey(reference.getExternalId())) {
        protectedPlayers.add(player.getId());
      }
    }
    for (PlayerExternalReference reference : footballReferences) {
      Player player = reference.getPlayer();
      if (player.isActive() && !protectedPlayers.contains(player.getId())) {
        player.deactivate();
        markedInactive++;
      }
    }
    this.playerRepository.flush();
    return new PlayerSynchronizationResult(
        snapshot.obtained(),
        created,
        updated,
        markedInactive,
        snapshot.discardedInvalid() + excluded.size());
  }

  private static boolean referenceConflict(Throwable failure) {
    Set<Throwable> visited = new HashSet<>();
    for (Throwable cause = failure; cause != null && visited.add(cause); cause = cause.getCause()) {
      if (cause instanceof PSQLException postgres) {
        ServerErrorMessage error = postgres.getServerErrorMessage();
        return "23505".equals(postgres.getSQLState())
            && error != null
            && "player_external_references".equals(error.getTable())
            && REFERENCE_UNIQUE.equals(error.getConstraint());
      }
    }
    return false;
  }

  /** Solo contiene la clave escalar de la escritura en curso, nunca una entidad JPA. */
  private static final class Attempt {
    private String externalId;
  }

  /** {@inheritDoc} */
  @Transactional(readOnly = true)
  @Override
  public Page<Player> getActivePlayers(int page, int size) {
    return this.playerRepository.findByActiveTrue(PageRequest.of(page, size, Sort.by("id")));
  }
}
