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
import footballmarket.services.internal.SynchronizationCounters;
import footballmarket.services.internal.SynchronizationOperation;
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

/**
 * Sincroniza el catálogo local de jugadores con una foto completa obtenida desde Football-Data.
 *
 * <p>Cada intento se ejecuta en una transacción independiente. Si una referencia externa genera un
 * conflicto recuperable, el candidato responsable se excluye y la sincronización se vuelve a
 * intentar desde una transacción limpia.
 */
@Service
@Transactional
public class PlayerCatalogServiceImpl implements PlayerCatalogService {

  private static final Logger LOG = LoggerFactory.getLogger(PlayerCatalogServiceImpl.class);

  private static final String REFERENCE_UNIQUE_CONSTRAINT =
      "uk_player_external_references_provider_external_id";

  private final PlayerRepository playerRepository;
  private final PlayerExternalReferenceRepository referenceRepository;
  private final TransactionTemplate transaction;

  /**
   * Construye el servicio y configura la transacción utilizada en cada intento de sincronización.
   *
   * @param playerRepository repositorio de jugadores
   * @param referenceRepository repositorio de referencias externas
   * @param transactionManager administrador de transacciones de Spring
   */
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

  /**
   * Aplica una foto completa del proveedor sobre el catálogo local.
   *
   * <p>Si durante una escritura aparece un conflicto recuperable de referencia externa, se excluye
   * únicamente al candidato responsable y se repite el intento dentro de una nueva transacción.
   *
   * @param snapshot foto obtenida desde Football-Data
   * @return resultado de la sincronización confirmada
   * @throws PlayerSynchronizationPersistenceException sí ocurre un error de persistencia no
   *     recuperable
   */
  @Override
  public PlayerSynchronizationResult applySynchronization(PlayerSnapshot snapshot) {
    Map<String, PlayerCandidate> candidates = uniqueCandidates(snapshot);

    Set<String> excludedExternalIds = new LinkedHashSet<>();
    Set<Long> protectedPlayerIds = new HashSet<>();

    int remainingAttempts = candidates.size() + 1;

    while (remainingAttempts-- > 0) {
      SynchronizationAttempt attempt = new SynchronizationAttempt();

      try {
        PlayerSynchronizationResult result =
            transaction.execute(
                status ->
                    synchronize(
                        snapshot, candidates, excludedExternalIds, protectedPlayerIds, attempt));

        logSynchronizationResult(result);
        return result;

      } catch (RuntimeException exception) {
        if (!canRecoverFromReferenceConflict(exception, attempt, excludedExternalIds)) {
          LOG.error("Sincronización fallida: error técnico de persistencia", exception);
          throw new PlayerSynchronizationPersistenceException();
        }
      }
    }

    throw new PlayerSynchronizationPersistenceException();
  }

  /**
   * Obtiene una página de jugadores activos ordenada por identificador.
   *
   * @param page número de página comenzando desde cero
   * @param size cantidad máxima de jugadores por página
   * @return página de jugadores activos
   */
  @Transactional(readOnly = true)
  @Override
  public Page<Player> getActivePlayers(int page, int size) {
    PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "id"));

    return playerRepository.findByActiveTrue(pageRequest);
  }

  /**
   * Ejecuta un intento completo de sincronización.
   *
   * <p>Primero resuelve referencias y conflictos, luego crea o actualiza jugadores y finalmente
   * inactiva aquellos que ya no aparecen en la foto recibida.
   *
   * @param snapshot foto original del proveedor
   * @param candidates candidatos únicos por identificador externo
   * @param excludedExternalIds identificadores descartados en intentos anteriores
   * @param protectedPlayerIds jugadores protegidos por conflictos
   * @param attempt estado escalar de la escritura actual
   * @return estadísticas del intento ejecutado
   */
  private PlayerSynchronizationResult synchronize(
      PlayerSnapshot snapshot,
      Map<String, PlayerCandidate> candidates,
      Set<String> excludedExternalIds,
      Set<Long> protectedPlayerIds,
      SynchronizationAttempt attempt) {

    Map<String, PlayerExternalReference> references =
        loadReferencesAndDetectConflicts(candidates, excludedExternalIds, protectedPlayerIds);

    Set<Long> protectedPlayers =
        resolveProtectedPlayers(references, excludedExternalIds, protectedPlayerIds);

    SynchronizationCounters counters =
        synchronizeCandidates(
            candidates, references, excludedExternalIds, protectedPlayers, attempt);

    int markedInactive = markMissingPlayersInactive(candidates, protectedPlayers);

    playerRepository.flush();

    return new PlayerSynchronizationResult(
        snapshot.obtained(),
        counters.created(),
        counters.updated(),
        markedInactive,
        snapshot.discardedInvalid() + excludedExternalIds.size());
  }

  /**
   * Elimina candidatos duplicados por identificador externo.
   *
   * <p>Se conserva la primera aparición de cada jugador recibida en el snapshot.
   *
   * @param snapshot foto recibida del proveedor
   * @return candidatos únicos indexados por identificador externo
   */
  private Map<String, PlayerCandidate> uniqueCandidates(PlayerSnapshot snapshot) {
    Map<String, PlayerCandidate> candidates = new LinkedHashMap<>();

    for (PlayerCandidate candidate : snapshot.players()) {
      candidates.putIfAbsent(candidate.externalId(), candidate);
    }

    return candidates;
  }

  /**
   * Carga las referencias existentes de los candidatos y detecta conflictos lógicos antes de
   * realizar escrituras.
   *
   * @param candidates candidatos del intento
   * @param excludedExternalIds identificadores descartados
   * @param protectedPlayerIds jugadores protegidos
   * @return referencias existentes indexadas por identificador externo
   */
  private Map<String, PlayerExternalReference> loadReferencesAndDetectConflicts(
      Map<String, PlayerCandidate> candidates,
      Set<String> excludedExternalIds,
      Set<Long> protectedPlayerIds) {

    Map<String, Long> expectedOwners = loadExpectedOwners();
    Map<String, PlayerExternalReference> references = new LinkedHashMap<>();

    for (String externalId : candidates.keySet()) {
      PlayerExternalReference reference =
          referenceRepository
              .findByProviderAndExternalId(PlayerProvider.FOOTBALL_DATA, externalId)
              .orElse(null);

      references.put(externalId, reference);

      detectLogicalConflict(
          externalId, reference, expectedOwners, excludedExternalIds, protectedPlayerIds);
    }

    return references;
  }

  /**
   * Obtiene el propietario esperado de cada referencia externa de Football-Data.
   *
   * @return mapa entre identificadores externos e identificadores internos de jugadores
   */
  private Map<String, Long> loadExpectedOwners() {
    Map<String, Long> owners = new LinkedHashMap<>();

    referenceRepository
        .findIdentitiesByProvider(PlayerProvider.FOOTBALL_DATA)
        .forEach(identity -> owners.put(identity.getExternalId(), identity.getPlayerId()));

    return owners;
  }

  /**
   * Detecta si una referencia externa cambió inesperadamente de propietario.
   *
   * <p>Ante una inconsistencia se protege tanto al propietario esperado como al actualmente
   * resuelto para evitar modificaciones o inactivaciones accidentales.
   *
   * @param externalId identificador externo
   * @param reference referencia actualmente resuelta
   * @param expectedOwners propietarios esperados
   * @param excludedExternalIds identificadores descartados
   * @param protectedPlayerIds jugadores protegidos
   */
  private void detectLogicalConflict(
      String externalId,
      PlayerExternalReference reference,
      Map<String, Long> expectedOwners,
      Set<String> excludedExternalIds,
      Set<Long> protectedPlayerIds) {

    Long expectedOwnerId = expectedOwners.get(externalId);

    if (expectedOwnerId == null) {
      return;
    }

    Long actualOwnerId = reference == null ? null : reference.getPlayer().getId();

    if (expectedOwnerId.equals(actualOwnerId)) {
      return;
    }

    protectedPlayerIds.add(expectedOwnerId);

    if (actualOwnerId != null) {
      protectedPlayerIds.add(actualOwnerId);
    }

    if (excludedExternalIds.add(externalId)) {
      LOG.warn(
          "Jugador descartado: conflicto lógico de referencia FOOTBALL_DATA, externalId={}",
          externalId);
    }
  }

  /**
   * Construye el conjunto efectivo de jugadores protegidos para el intento actual.
   *
   * @param references referencias resueltas
   * @param excludedExternalIds identificadores descartados
   * @param protectedPlayerIds jugadores protegidos previamente
   * @return conjunto efectivo de jugadores protegidos
   */
  private Set<Long> resolveProtectedPlayers(
      Map<String, PlayerExternalReference> references,
      Set<String> excludedExternalIds,
      Set<Long> protectedPlayerIds) {

    Set<Long> protectedPlayers = new HashSet<>(protectedPlayerIds);

    for (String externalId : excludedExternalIds) {
      PlayerExternalReference reference = references.get(externalId);

      if (reference != null) {
        protectedPlayers.add(reference.getPlayer().getId());
      }
    }

    return protectedPlayers;
  }

  /**
   * Crea o actualiza los candidatos que pueden sincronizarse.
   *
   * <p>Los candidatos ya excluidos se omiten. Cada candidato restante se delega a un método
   * específico para mantener el flujo principal simple.
   *
   * @param candidates candidatos disponibles
   * @param references referencias existentes
   * @param excludedExternalIds identificadores descartados
   * @param protectedPlayers jugadores protegidos
   * @param attempt estado de la escritura actual
   * @return cantidad de jugadores creados y actualizados
   */
  private SynchronizationCounters synchronizeCandidates(
      Map<String, PlayerCandidate> candidates,
      Map<String, PlayerExternalReference> references,
      Set<String> excludedExternalIds,
      Set<Long> protectedPlayers,
      SynchronizationAttempt attempt) {

    int created = 0;
    int updated = 0;

    for (PlayerCandidate candidate : candidates.values()) {
      if (excludedExternalIds.contains(candidate.externalId())) {
        continue;
      }

      SynchronizationOperation operation =
          synchronizeCandidate(
              candidate,
              references.get(candidate.externalId()),
              excludedExternalIds,
              protectedPlayers,
              attempt);

      if (operation == SynchronizationOperation.CREATED) {
        created++;
      } else if (operation == SynchronizationOperation.UPDATED) {
        updated++;
      }
    }

    return new SynchronizationCounters(created, updated);
  }

  /**
   * Sincroniza un único candidato.
   *
   * <p>Si la referencia pertenece a un jugador protegido, el candidato se descarta. Caso contrario,
   * se crea o actualiza el jugador y se fuerza un {@code flush} para detectar inmediatamente
   * posibles violaciones de unicidad.
   *
   * @param candidate candidato a procesar
   * @param reference referencia existente o {@code null}
   * @param excludedExternalIds identificadores descartados
   * @param protectedPlayers jugadores protegidos
   * @param attempt estado de la escritura actual
   * @return operación realizada
   */
  private SynchronizationOperation synchronizeCandidate(
      PlayerCandidate candidate,
      PlayerExternalReference reference,
      Set<String> excludedExternalIds,
      Set<Long> protectedPlayers,
      SynchronizationAttempt attempt) {

    if (belongsToProtectedPlayer(reference, protectedPlayers)) {
      excludeCandidate(candidate, excludedExternalIds);
      return SynchronizationOperation.NONE;
    }

    attempt.clear();

    SynchronizationOperation operation;

    if (reference == null) {
      createPlayer(candidate, attempt);
      operation = SynchronizationOperation.CREATED;
    } else {
      updatePlayer(reference.getPlayer(), candidate, attempt);
      operation = SynchronizationOperation.UPDATED;
    }

    playerRepository.flush();
    attempt.clear();

    return operation;
  }

  /**
   * Comprueba si la referencia corresponde a un jugador protegido.
   *
   * @param reference referencia externa
   * @param protectedPlayers jugadores protegidos
   * @return {@code true} si la referencia existe y pertenece a un jugador protegido
   */
  private boolean belongsToProtectedPlayer(
      PlayerExternalReference reference, Set<Long> protectedPlayers) {

    return reference != null && protectedPlayers.contains(reference.getPlayer().getId());
  }

  /**
   * Excluye un candidato que no debe participar de la sincronización.
   *
   * @param candidate candidato descartado
   * @param excludedExternalIds identificadores descartados
   */
  private void excludeCandidate(PlayerCandidate candidate, Set<String> excludedExternalIds) {

    if (excludedExternalIds.add(candidate.externalId())) {
      LOG.warn(
          "Jugador descartado: identidad implicada en un conflicto FOOTBALL_DATA, externalId={}",
          candidate.externalId());
    }
  }

  /**
   * Crea un nuevo jugador junto con su referencia externa.
   *
   * @param candidate datos obtenidos del proveedor
   * @param attempt estado de la escritura actual
   */
  private void createPlayer(PlayerCandidate candidate, SynchronizationAttempt attempt) {

    Player player =
        new Player(candidate.name(), candidate.team(), candidate.league(), candidate.position());

    player.updateOptionalDetails(candidate.dateOfBirth(), candidate.nationality());

    player.addExternalReference(PlayerProvider.FOOTBALL_DATA, candidate.externalId());

    attempt.setCurrentExternalId(candidate.externalId());

    playerRepository.save(player);
  }

  /**
   * Actualiza un jugador existente con los datos actuales del proveedor.
   *
   * @param player jugador persistido
   * @param candidate datos actuales
   * @param attempt estado de la escritura actual
   */
  private void updatePlayer(
      Player player, PlayerCandidate candidate, SynchronizationAttempt attempt) {

    player.update(candidate.name(), candidate.team(), candidate.league(), candidate.position());

    player.updateOptionalDetails(candidate.dateOfBirth(), candidate.nationality());

    player.activate();

    attempt.setCurrentExternalId(candidate.externalId());
  }

  /**
   * Inactivo jugador que ya no están presentes en la foto actual.
   *
   * <p>Los jugadores protegidos y aquellos que tienen al menos una referencia presente en el
   * snapshot se conservan activos.
   *
   * @param candidates candidatos presentes en el snapshot
   * @param protectedPlayers jugadores que no deben inactivarse
   * @return cantidad de jugadores inactivados
   */
  private int markMissingPlayersInactive(
      Map<String, PlayerCandidate> candidates, Set<Long> protectedPlayers) {

    List<PlayerExternalReference> references =
        referenceRepository.findByProvider(PlayerProvider.FOOTBALL_DATA);

    protectPlayersPresentInSnapshot(candidates, references, protectedPlayers);

    int markedInactive = 0;

    for (PlayerExternalReference reference : references) {
      Player player = reference.getPlayer();

      if (player.isActive() && !protectedPlayers.contains(player.getId())) {
        player.deactivate();
        markedInactive++;
      }
    }

    return markedInactive;
  }

  /**
   * Protege jugadores que poseen al menos una referencia presente en el snapshot recibido.
   *
   * @param candidates candidatos actuales
   * @param references referencias externas persistidas
   * @param protectedPlayers jugadores protegidos
   */
  private void protectPlayersPresentInSnapshot(
      Map<String, PlayerCandidate> candidates,
      List<PlayerExternalReference> references,
      Set<Long> protectedPlayers) {

    for (PlayerExternalReference reference : references) {
      if (candidates.containsKey(reference.getExternalId())) {
        protectedPlayers.add(reference.getPlayer().getId());
      }
    }
  }

  /**
   * Determina si un error de persistencia corresponde a un conflicto recuperable de referencia
   * externa.
   *
   * <p>Cuando el conflicto puede atribuirse al candidato actualmente escrito, su identificador se
   * agrega a los descartados para permitir un nuevo intento.
   *
   * @param exception error producido durante la transacción
   * @param attempt estado de la escritura que estaba ejecutándose
   * @param excludedExternalIds identificadores descartados
   * @return {@code true} si el conflicto puede recuperarse mediante un nuevo intento
   */
  private boolean canRecoverFromReferenceConflict(
      RuntimeException exception, SynchronizationAttempt attempt, Set<String> excludedExternalIds) {

    String externalId = attempt.currentExternalId();

    if (externalId == null) {
      return false;
    }

    if (!isReferenceUniqueConstraintViolation(exception)) {
      return false;
    }

    if (!excludedExternalIds.add(externalId)) {
      return false;
    }

    LOG.warn(
        "Jugador descartado: conflicto de referencia FOOTBALL_DATA, externalId={}", externalId);

    return true;
  }

  /**
   * Comprueba si una excepción representa una violación de la restricción única de referencias
   * externas.
   *
   * <p>Se recorre toda la cadena de causas porque Spring e Hibernate pueden envolver la excepción
   * original generada por PostgreSQL.
   *
   * @param failure error de persistencia
   * @return {@code true} si corresponde a la restricción única esperada
   */
  private static boolean isReferenceUniqueConstraintViolation(Throwable failure) {
    Set<Throwable> visited = new HashSet<>();

    Throwable cause = failure;

    while (cause != null && visited.add(cause)) {
      if (cause instanceof PSQLException postgres && isExpectedReferenceConstraint(postgres)) {
        return true;
      }

      cause = cause.getCause();
    }

    return false;
  }

  /**
   * Comprueba si una excepción concreta de PostgreSQL corresponde a la restricción única utilizada
   * por las referencias externas.
   *
   * @param postgres excepción original de PostgreSQL
   * @return {@code true} si coincide con la tabla, SQLState y restricción esperada
   */
  private static boolean isExpectedReferenceConstraint(PSQLException postgres) {
    ServerErrorMessage error = postgres.getServerErrorMessage();

    if (!"23505".equals(postgres.getSQLState()) || error == null) {
      return false;
    }

    return "player_external_references".equals(error.getTable())
        && REFERENCE_UNIQUE_CONSTRAINT.equals(error.getConstraint());
  }

  /**
   * Registra las estadísticas finales de una sincronización confirmada.
   *
   * @param result resultado final
   */
  private void logSynchronizationResult(PlayerSynchronizationResult result) {
    LOG.info(
        "Sincronización confirmada: obtenidos={}, creados={}, actualizados={}, "
            + "inactivos={}, descartados={}",
        result.obtained(),
        result.created(),
        result.updated(),
        result.markedInactive(),
        result.discardedInvalid());
  }

  /**
   * Mantiene únicamente el identificador externo de la escritura actualmente ejecutada.
   *
   * <p>No almacena entidades JPA porque este objeto puede sobrevivir al rollback de una
   * transacción.
   */
  private static final class SynchronizationAttempt {

    private String currentExternalId;

    /**
     * Devuelve el identificador externo de la escritura actual.
     *
     * @return identificador externo o {@code null} si no hay una escritura atribuible
     */
    private String currentExternalId() {
      return currentExternalId;
    }

    /**
     * Registra el candidato cuya escritura se está realizando.
     *
     * @param externalId identificador externo
     */
    private void setCurrentExternalId(String externalId) {
      this.currentExternalId = externalId;
    }

    /** Limpia el candidato asociado a la escritura actual. */
    private void clear() {
      this.currentExternalId = null;
    }
  }
}
