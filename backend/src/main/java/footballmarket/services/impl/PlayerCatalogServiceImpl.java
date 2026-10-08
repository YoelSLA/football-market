package footballmarket.services.impl;

import footballmarket.models.CatalogTransition;
import footballmarket.models.League;
import footballmarket.models.PendingReviewCase;
import footballmarket.models.Player;
import footballmarket.models.PlayerExternalReference;
import footballmarket.models.PlayerImageResolution;
import footballmarket.models.PlayerPresentation;
import footballmarket.models.Team;
import footballmarket.models.TeamNameNormalizer;
import footballmarket.models.enums.ExternalProvider;
import footballmarket.models.enums.ReviewCategory;
import footballmarket.models.enums.ReviewCause;
import footballmarket.models.enums.ReviewSubjectType;
import footballmarket.models.records.InvalidPlayerObservation;
import footballmarket.models.records.LeagueCandidate;
import footballmarket.models.records.PlayerCandidate;
import footballmarket.models.records.PlayerSnapshot;
import footballmarket.models.records.PlayerSynchronizationResult;
import footballmarket.models.records.TeamCandidate;
import footballmarket.repositories.CatalogTransitionRepository;
import footballmarket.repositories.LeagueExternalReferenceRepository;
import footballmarket.repositories.LeagueRepository;
import footballmarket.repositories.PendingReviewCaseRepository;
import footballmarket.repositories.PlayerExternalReferenceRepository;
import footballmarket.repositories.PlayerImageResolutionRepository;
import footballmarket.repositories.PlayerRepository;
import footballmarket.repositories.TeamExternalReferenceRepository;
import footballmarket.repositories.TeamRepository;
import footballmarket.services.PlayerCatalogService;
import footballmarket.services.exceptions.PlayerSynchronizationPersistenceException;
import footballmarket.services.internal.SynchronizationCounters;
import footballmarket.services.internal.SynchronizationOperation;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
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
  private final PlayerImageResolutionRepository imageResolutionRepository;
  private final TransactionTemplate transaction;
  private final LeagueRepository leagues;
  private final TeamRepository teams;
  private final LeagueExternalReferenceRepository leagueReferences;
  private final TeamExternalReferenceRepository teamReferences;
  private final PendingReviewCaseRepository cases;
  private final CatalogTransitionRepository transitions;

  /**
   * Construye el servicio y configura la transacción utilizada en cada intento de sincronización.
   *
   * @param playerRepository repositorio de jugadores
   * @param referenceRepository repositorio de referencias externas
   * @param imageResolutionRepository repositorio de resoluciones de imágenes
   * @param transactionManager administrador de transacciones de Spring
   */
  public PlayerCatalogServiceImpl(
      PlayerRepository playerRepository,
      PlayerExternalReferenceRepository referenceRepository,
      PlayerImageResolutionRepository imageResolutionRepository,
      PlatformTransactionManager transactionManager,
      LeagueRepository leagues,
      TeamRepository teams,
      LeagueExternalReferenceRepository leagueReferences,
      TeamExternalReferenceRepository teamReferences,
      PendingReviewCaseRepository cases,
      CatalogTransitionRepository transitions) {

    this.playerRepository = playerRepository;
    this.referenceRepository = referenceRepository;
    this.imageResolutionRepository = imageResolutionRepository;
    this.leagues = leagues;
    this.teams = teams;
    this.leagueReferences = leagueReferences;
    this.teamReferences = teamReferences;
    this.cases = cases;
    this.transitions = transitions;

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
    Map<ReviewSubjectType, Set<String>> domainExclusions =
        new java.util.EnumMap<>(ReviewSubjectType.class);
    domainExclusions.put(ReviewSubjectType.LEAGUE, new LinkedHashSet<>());
    domainExclusions.put(ReviewSubjectType.TEAM, new LinkedHashSet<>());

    int remainingAttempts =
        candidates.size() + snapshot.leagues().size() + snapshot.teams().size() + 1;

    while (remainingAttempts-- > 0) {
      SynchronizationAttempt attempt = new SynchronizationAttempt();

      try {
        PlayerSynchronizationResult result =
            transaction.execute(
                status ->
                    synchronize(
                        snapshot,
                        candidates,
                        excludedExternalIds,
                        protectedPlayerIds,
                        domainExclusions,
                        attempt));

        logSynchronizationResult(result);
        return result;

      } catch (RuntimeException exception) {
        if (!this.canRecoverDomainConflict(exception, attempt, domainExclusions)
            && !canRecoverFromReferenceConflict(exception, attempt, excludedExternalIds)) {
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

    return this.playerRepository.findByActiveTrue(pageRequest);
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
      Map<ReviewSubjectType, Set<String>> domainExclusions,
      SynchronizationAttempt attempt) {

    // Las exclusiones funcionales se recalculan por intento; solo conflictos técnicos se heredan.
    // Una reaplicación reconstruye también las presentaciones desde la foto original.
    candidates = this.uniqueCandidates(snapshot);
    Set<String> attemptExcluded = new LinkedHashSet<>(excludedExternalIds);
    Map<String, PlayerExternalReference> references =
        loadReferencesAndDetectConflicts(candidates, attemptExcluded, protectedPlayerIds);

    Set<Long> protectedPlayers =
        resolveProtectedPlayers(references, attemptExcluded, protectedPlayerIds);

    for (String externalId : excludedExternalIds) {
      PlayerExternalReference actual = references.get(externalId);
      Map<String, Object> evidence = new LinkedHashMap<>();
      evidence.put("externalId", externalId);
      evidence.put("intendedOwner", "NEW_PLAYER");
      if (actual != null) {
        evidence.put("currentOwnerId", actual.getPlayer().getId());
        if (actual.getPlayer().getTeam() == null) {
          evidence.put("originalTeamName", actual.getPlayer().getLegacyTeam());
        }
      }
      this.recordCase(
          ReviewCategory.EXTERNAL_IDENTITY_CONFLICT,
          ReviewCause.EXTERNAL_IDENTITY_OWNED,
          ReviewSubjectType.PLAYER,
          actual == null ? null : actual.getPlayer().getId(),
          externalId,
          "new-player",
          evidence);
    }

    Map<String, Team> resolvedTeams =
        this.applyTeamDomain(snapshot, protectedPlayers, domainExclusions, attempt);
    for (InvalidPlayerObservation observation : snapshot.invalidPlayers()) {
      this.protectAndRecordPlayer(
          observation.externalId(),
          observation.teamExternalId(),
          observation.teamName(),
          ReviewCause.MISSING_REQUIRED_DATA,
          protectedPlayers);
    }
    for (PlayerCandidate candidate : candidates.values()) {
      if (!resolvedTeams.containsKey(candidate.teamExternalId())) {
        attemptExcluded.add(candidate.externalId());
        this.protectAndRecordPlayer(
            candidate.externalId(),
            candidate.teamExternalId(),
            candidate.team(),
            ReviewCause.TEAM_UNRESOLVED,
            protectedPlayers);
      }
    }

    this.protectConflictingPlayerTeams(
        candidates, references, resolvedTeams, attemptExcluded, protectedPlayers);

    SynchronizationCounters counters =
        synchronizeCandidates(candidates, references, attemptExcluded, protectedPlayers, attempt);

    this.completeTransition(snapshot, candidates, protectedPlayers);

    int markedInactive = markMissingPlayersInactive(candidates, protectedPlayers);

    playerRepository.flush();

    return new PlayerSynchronizationResult(
        snapshot.obtained(),
        counters.created(),
        counters.updated(),
        markedInactive,
        snapshot.discardedInvalid() + attemptExcluded.size());
  }

  /** Valores opcionales consolidados por atributo e independientes entre sí. */
  private record OptionalValues(LocalDate dateOfBirth, String nationality) {}

  /**
   * Resuelve cada atributo opcional por separado sobre el conjunto completo de observaciones.
   *
   * <p>Sin valor válido no hay valor nuevo; con un único valor válido se usa; con valores válidos
   * incompatibles se conserva el persistido del Player, que es null en un alta sin valor previo. Un
   * conflicto en un atributo no impide consolidar el otro ni invalida al Player completo.
   *
   * @param observations observaciones válidas del mismo propietario
   * @param owner jugador propietario ya reconocido por sus referencias
   * @return valores opcionales consolidados
   */
  private OptionalValues consolidateOptionalValues(
      List<PlayerCandidate> observations, Player owner) {
    List<LocalDate> dates =
        observations.stream()
            .map(PlayerCandidate::dateOfBirth)
            .filter(java.util.Objects::nonNull)
            .distinct()
            .toList();
    List<String> nationalities =
        observations.stream()
            .map(PlayerCandidate::nationality)
            .filter(java.util.Objects::nonNull)
            .distinct()
            .toList();
    LocalDate dateOfBirth = dates.size() == 1 ? dates.getFirst() : owner.getDateOfBirth();
    String nationality =
        nationalities.size() == 1 ? nationalities.getFirst() : owner.getNationality();
    if (dates.size() > 1) {
      this.recordOptionalConflict(
          owner, ReviewCause.CONFLICTING_DATE_OF_BIRTH, "dateOfBirth", dates);
    }
    if (nationalities.size() > 1) {
      this.recordOptionalConflict(
          owner, ReviewCause.CONFLICTING_NATIONALITY, "nationality", nationalities);
    }
    return new OptionalValues(dateOfBirth, nationality);
  }

  /**
   * Registra el conflicto de un atributo opcional sin clasificar al Player como inválido.
   *
   * @param owner jugador propietario del conflicto
   * @param cause causa estable del atributo en conflicto
   * @param attribute nombre del atributo observado
   * @param values valores válidos incompatibles observados
   */
  private void recordOptionalConflict(
      Player owner, ReviewCause cause, String attribute, List<?> values) {
    Map<String, Object> evidence = new LinkedHashMap<>();
    evidence.put("attribute", attribute);
    evidence.put("receivedValues", values);
    if (owner.getDateOfBirth() != null || owner.getNationality() != null) {
      evidence.put(
          "previousValues",
          Map.of(
              "dateOfBirth",
                  owner.getDateOfBirth() == null ? "" : owner.getDateOfBirth().toString(),
              "nationality", owner.getNationality() == null ? "" : owner.getNationality()));
    }
    this.recordCase(
        ReviewCategory.PLAYER_OPTIONAL_CONFLICT,
        cause,
        ReviewSubjectType.PLAYER,
        owner.getId(),
        null,
        attribute,
        evidence);
  }

  /**
   * Clasifica al propietario antes de mutarlo, sin seleccionar entre asociaciones contradictorias.
   */
  private void protectConflictingPlayerTeams(
      Map<String, PlayerCandidate> candidates,
      Map<String, PlayerExternalReference> references,
      Map<String, Team> resolvedTeams,
      Set<String> excludedExternalIds,
      Set<Long> protectedPlayers) {
    Map<Long, List<PlayerCandidate>> observationsByOwner = new LinkedHashMap<>();
    for (PlayerCandidate candidate : candidates.values()) {
      PlayerExternalReference reference = references.get(candidate.externalId());
      if (reference != null
          && !excludedExternalIds.contains(candidate.externalId())
          && resolvedTeams.containsKey(candidate.teamExternalId())) {
        observationsByOwner
            .computeIfAbsent(reference.getPlayer().getId(), ignored -> new ArrayList<>())
            .add(candidate);
      }
    }
    for (Map.Entry<Long, List<PlayerCandidate>> entry : observationsByOwner.entrySet()) {
      List<PlayerCandidate> observations = entry.getValue();
      long distinctTeams =
          observations.stream()
              .map(candidate -> resolvedTeams.get(candidate.teamExternalId()).getId())
              .distinct()
              .count();
      long distinctNames =
          observations.stream()
              .map(candidate -> TeamNameNormalizer.normalize(candidate.name()))
              .distinct()
              .count();
      long distinctPositions =
          observations.stream()
              .map(candidate -> TeamNameNormalizer.normalize(candidate.position()))
              .distinct()
              .count();
      if (distinctTeams < 2 && distinctNames < 2 && distinctPositions < 2) {
        Player owner = references.get(observations.getFirst().externalId()).getPlayer();
        String name =
            PlayerPresentation.equivalent(owner.getName(), observations.getFirst().name())
                ? owner.getName()
                : PlayerPresentation.canonical(
                    observations.stream().map(PlayerCandidate::name).toList());
        String position =
            PlayerPresentation.equivalent(owner.getPosition(), observations.getFirst().position())
                ? owner.getPosition()
                : PlayerPresentation.canonical(
                    observations.stream().map(PlayerCandidate::position).toList());
        OptionalValues optionals = this.consolidateOptionalValues(observations, owner);
        for (PlayerCandidate candidate : observations) {
          candidates.put(
              candidate.externalId(),
              new PlayerCandidate(
                  candidate.externalId(),
                  name,
                  candidate.team(),
                  candidate.league(),
                  position,
                  optionals.dateOfBirth(),
                  optionals.nationality(),
                  candidate.teamExternalId()));
        }
        continue;
      }
      Player player = references.get(observations.getFirst().externalId()).getPlayer();
      protectedPlayers.add(player.getId());
      observations.forEach(candidate -> excludedExternalIds.add(candidate.externalId()));
      Map<String, Object> evidence = new LinkedHashMap<>();
      evidence.put("previousActive", player.isActive());
      if (player.getTeam() == null) {
        evidence.put("originalTeamName", player.getLegacyTeam());
      } else {
        evidence.put("previousTeamId", player.getTeam().getId());
      }
      // El orden solo canoniza la evidencia; nunca elige una asociación para el Player.
      List<Map<String, Object>> observed =
          observations.stream()
              .sorted(java.util.Comparator.comparing(PlayerCandidate::externalId))
              .map(
                  candidate ->
                      Map.<String, Object>of(
                          "provider", ExternalProvider.FOOTBALL_DATA.name(),
                          "externalId", candidate.externalId(),
                          "name", candidate.name(),
                          "position", candidate.position(),
                          "teamExternalId", candidate.teamExternalId(),
                          "teamId", resolvedTeams.get(candidate.teamExternalId()).getId()))
              .toList();
      evidence.put("observations", observed);
      ReviewCause cause =
          distinctTeams > 1
              ? ReviewCause.CONFLICTING_PLAYER_TEAMS
              : ReviewCause.CONFLICTING_PLAYER_STATE;
      // Conserva la clave del caso por sujeto: los atributos contradictorios son evidencia, no
      // identidad.
      String key = "INVALID_SUBJECT_DATA:PLAYER:" + player.getId() + ":CONFLICTING_PLAYER_TEAMS";
      Instant now = Instant.now();
      PendingReviewCase review = this.cases.findByCaseKey(key).orElse(null);
      if (review == null) {
        this.cases.save(
            new PendingReviewCase(
                ReviewCategory.INVALID_SUBJECT_DATA,
                cause,
                ReviewSubjectType.PLAYER,
                player.getId(),
                null,
                null,
                key,
                now,
                evidence));
      } else {
        review.observe(cause, now, evidence);
      }
    }
  }

  /**
   * Elimina candidatos duplicados por identificador externo.
   *
   * <p>Se conserva prioridad semántica por referencia y se seleccionan sus formas equivalentes
   * mediante el ranking de presentación independiente del orden.
   *
   * @param snapshot foto recibida del proveedor
   * @return candidatos únicos indexados por identificador externo
   */
  private Map<String, PlayerCandidate> uniqueCandidates(PlayerSnapshot snapshot) {
    Map<String, PlayerCandidate> candidates = new LinkedHashMap<>();

    for (PlayerCandidate candidate : snapshot.players()) {
      candidates.merge(candidate.externalId(), candidate, PlayerCandidate::consolidatePresentation);
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
              .findByProviderAndExternalId(ExternalProvider.FOOTBALL_DATA, externalId)
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
        .findIdentitiesByProvider(ExternalProvider.FOOTBALL_DATA)
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
      Map<String, Object> evidence = new LinkedHashMap<>();
      evidence.put("intendedOwnerId", expectedOwnerId);
      if (actualOwnerId != null) {
        evidence.put("currentOwnerId", actualOwnerId);
      }
      this.recordCase(
          ReviewCategory.EXTERNAL_IDENTITY_CONFLICT,
          ReviewCause.EXTERNAL_IDENTITY_OWNED,
          ReviewSubjectType.PLAYER,
          expectedOwnerId,
          externalId,
          "owner:" + expectedOwnerId,
          evidence);
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

    Player player = new Player(candidate.name(), this.resolveTeam(candidate), candidate.position());

    player.updateOptionalDetails(candidate.dateOfBirth(), candidate.nationality());

    player.addExternalReference(ExternalProvider.FOOTBALL_DATA, candidate.externalId());

    attempt.setCurrentExternalId(candidate.externalId());

    playerRepository.save(player);
    this.reconcileCases(ReviewSubjectType.PLAYER, candidate.externalId(), player.getId());
    this.imageResolutionRepository.save(new PlayerImageResolution(player));
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

    player.update(candidate.name(), this.resolveTeam(candidate), candidate.position());

    player.updateOptionalDetails(candidate.dateOfBirth(), candidate.nationality());

    player.activate();
    this.reconcileCases(ReviewSubjectType.PLAYER, candidate.externalId(), player.getId());

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
        referenceRepository.findByProvider(ExternalProvider.FOOTBALL_DATA);

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

  private Team resolveTeam(PlayerCandidate candidate) {
    return this.teamReferences
        .findByProviderAndExternalId(ExternalProvider.FOOTBALL_DATA, candidate.teamExternalId())
        .orElseThrow(PlayerSynchronizationPersistenceException::new)
        .getTeam();
  }

  /** Aplica identidades reales de la foto y calcula vigencia antes del backfill. */
  private Map<String, Team> applyTeamDomain(
      PlayerSnapshot snapshot,
      Set<Long> protectedPlayers,
      Map<ReviewSubjectType, Set<String>> domainExclusions,
      SynchronizationAttempt attempt) {
    Map<String, League> resolvedLeagues = new LinkedHashMap<>();
    for (LeagueCandidate candidate : snapshot.leagues()) {
      if (!validText(candidate.externalId())) {
        throw new PlayerSynchronizationPersistenceException();
      }
      if (domainExclusions.get(ReviewSubjectType.LEAGUE).contains(candidate.externalId())) {
        League actual =
            this.leagueReferences
                .findByProviderAndExternalId(ExternalProvider.FOOTBALL_DATA, candidate.externalId())
                .map(reference -> reference.getLeague())
                .orElse(null);
        this.recordDomainConflict(
            ReviewSubjectType.LEAGUE,
            candidate.externalId(),
            actual == null ? null : actual.getId());
        continue;
      }
      if (!validText(candidate.name())) {
        this.recordCase(
            ReviewCategory.INVALID_SUBJECT_DATA,
            ReviewCause.MISSING_REQUIRED_DATA,
            ReviewSubjectType.LEAGUE,
            null,
            candidate.externalId(),
            "data",
            Map.of("missing", "name"));
        continue;
      }
      League league =
          this.leagueReferences
              .findByProviderAndExternalId(ExternalProvider.FOOTBALL_DATA, candidate.externalId())
              .map(reference -> reference.getLeague())
              .orElse(null);
      if (league == null) {
        attempt.setDomain(ReviewSubjectType.LEAGUE, candidate.externalId());
        league = new League(candidate.name());
        league.addExternalReference(ExternalProvider.FOOTBALL_DATA, candidate.externalId());
        this.leagues.saveAndFlush(league);
        attempt.clear();
      } else {
        league.rename(candidate.name());
      }
      resolvedLeagues.put(candidate.externalId(), league);
      this.reconcileCases(ReviewSubjectType.LEAGUE, candidate.externalId(), league.getId());
    }
    Map<String, Team> resolvedTeams = new LinkedHashMap<>();
    Set<Long> protectedTeams = new HashSet<>();
    for (TeamCandidate candidate : snapshot.teams()) {
      if (!validText(candidate.externalId())) {
        throw new PlayerSynchronizationPersistenceException();
      }
      Team team =
          this.teamReferences
              .findByProviderAndExternalId(ExternalProvider.FOOTBALL_DATA, candidate.externalId())
              .map(reference -> reference.getTeam())
              .orElse(null);
      League league = resolvedLeagues.get(candidate.leagueExternalId());
      if (domainExclusions.get(ReviewSubjectType.TEAM).contains(candidate.externalId())) {
        if (team != null) {
          protectedTeams.add(team.getId());
          this.protectTeamPlayers(team, protectedPlayers);
        }
        this.recordDomainConflict(
            ReviewSubjectType.TEAM, candidate.externalId(), team == null ? null : team.getId());
        continue;
      }
      if (!validText(candidate.name()) || league == null || !candidate.rosterKnown()) {
        if (team != null) {
          protectedTeams.add(team.getId());
          this.protectTeamPlayers(team, protectedPlayers);
        }
        this.recordCase(
            ReviewCategory.TEAM_LEAGUE_UNRESOLVED,
            ReviewCause.LEAGUE_UNRESOLVED,
            ReviewSubjectType.TEAM,
            team == null ? null : team.getId(),
            candidate.externalId(),
            candidate.leagueExternalId() == null ? "missing" : candidate.leagueExternalId(),
            Map.of(
                "receivedLeagueExternalId",
                candidate.leagueExternalId() == null ? "" : candidate.leagueExternalId()));
        continue;
      }
      if (team == null) {
        attempt.setDomain(ReviewSubjectType.TEAM, candidate.externalId());
        team = new Team(candidate.name(), league, true);
        team.addExternalReference(ExternalProvider.FOOTBALL_DATA, candidate.externalId());
        this.teams.saveAndFlush(team);
        attempt.clear();
      } else {
        team.update(candidate.name(), league);
        team.markCurrent();
      }
      resolvedTeams.put(candidate.externalId(), team);
      this.reconcileCases(ReviewSubjectType.TEAM, candidate.externalId(), team.getId());
    }
    Set<Long> presentTeams = new HashSet<>();
    resolvedTeams.values().forEach(team -> presentTeams.add(team.getId()));
    for (Team team : this.teams.findAll()) {
      if (!presentTeams.contains(team.getId()) && !protectedTeams.contains(team.getId())) {
        team.retire();
      }
    }
    return resolvedTeams;
  }

  private void protectTeamPlayers(Team team, Set<Long> protectedPlayers) {
    for (Player previous : this.playerRepository.findAll()) {
      if (previous.getTeam() != null && previous.getTeam().getId().equals(team.getId())) {
        protectedPlayers.add(previous.getId());
      }
    }
  }

  private void recordDomainConflict(ReviewSubjectType type, String externalId, Long ownerId) {
    Map<String, Object> evidence = new LinkedHashMap<>();
    evidence.put("externalId", externalId);
    evidence.put("intendedOwner", "NEW_" + type);
    if (ownerId != null) {
      evidence.put("currentOwnerId", ownerId);
    }
    this.recordCase(
        ReviewCategory.EXTERNAL_IDENTITY_CONFLICT,
        ReviewCause.EXTERNAL_IDENTITY_OWNED,
        type,
        ownerId,
        externalId,
        "new-" + type,
        evidence);
  }

  /** Reconoce únicamente la UNIQUE de identidad del tipo cuya creación estaba en curso. */
  private boolean canRecoverDomainConflict(
      RuntimeException exception,
      SynchronizationAttempt attempt,
      Map<ReviewSubjectType, Set<String>> exclusions) {
    if (attempt.subjectType == null || attempt.currentExternalId() == null) {
      return false;
    }
    String table =
        attempt.subjectType == ReviewSubjectType.LEAGUE
            ? "league_external_references"
            : "team_external_references";
    Set<Throwable> visited = new HashSet<>();
    for (Throwable cause = exception;
        cause != null && visited.add(cause);
        cause = cause.getCause()) {
      if (cause instanceof PSQLException postgres
          && "23505".equals(postgres.getSQLState())
          && postgres.getServerErrorMessage() != null
          && table.equals(postgres.getServerErrorMessage().getTable())
          && ("uk_" + table + "_provider_external_id")
              .equals(postgres.getServerErrorMessage().getConstraint())) {
        return exclusions.get(attempt.subjectType).add(attempt.currentExternalId());
      }
    }
    return false;
  }

  private static boolean validText(String text) {
    return text != null && !text.isBlank() && text.length() <= 255;
  }

  private void protectAndRecordPlayer(
      String externalId,
      String teamExternalId,
      String teamName,
      ReviewCause cause,
      Set<Long> protectedPlayers) {
    Player player =
        this.referenceRepository
            .findByProviderAndExternalId(ExternalProvider.FOOTBALL_DATA, externalId)
            .map(reference -> reference.getPlayer())
            .orElse(null);
    Map<String, Object> evidence = new LinkedHashMap<>();
    evidence.put("receivedTeamExternalId", teamExternalId == null ? "" : teamExternalId);
    evidence.put("receivedTeamName", teamName == null ? "" : teamName);
    if (player != null) {
      protectedPlayers.add(player.getId());
      if (player.getTeam() == null) {
        evidence.put("originalTeamName", player.getLegacyTeam());
      } else {
        evidence.put("previousTeamId", player.getTeam().getId());
      }
    }
    this.recordCase(
        ReviewCategory.PLAYER_TEAM_UNRESOLVED,
        cause,
        ReviewSubjectType.PLAYER,
        player == null ? null : player.getId(),
        externalId,
        teamExternalId == null ? "missing" : teamExternalId,
        evidence);
  }

  /** Conserva claves por referencia de origen para reconciliar sujetos externos e internos. */
  private void reconcileCases(ReviewSubjectType type, String externalId, Long subjectId) {
    for (PendingReviewCase review :
        this.cases.findBySubjectTypeAndSubjectProviderAndSubjectExternalId(
            type, ExternalProvider.FOOTBALL_DATA, externalId)) {
      if (review.getSubjectId() == null) {
        review.recognizeSubject(subjectId);
      }
    }
  }

  /** Conserva claves por referencia de origen para reconciliar sujetos externos e internos. */
  private void recordCase(
      ReviewCategory category,
      ReviewCause cause,
      ReviewSubjectType type,
      Long subjectId,
      String externalId,
      String relation,
      Map<String, Object> evidence) {
    String key =
        externalId == null
            ? category + ":" + type + ":" + subjectId + ":" + relation.length() + ":" + relation
            : category
                + ":"
                + type
                + ":FOOTBALL_DATA:"
                + externalId.length()
                + ":"
                + externalId
                + ":"
                + relation.length()
                + ":"
                + relation;
    Instant now = Instant.now();
    PendingReviewCase review = this.cases.findByCaseKey(key).orElse(null);
    if (review == null) {
      review =
          new PendingReviewCase(
              category,
              cause,
              type,
              subjectId,
              externalId == null ? null : ExternalProvider.FOOTBALL_DATA,
              externalId,
              key,
              now,
              evidence);
      this.cases.save(review);
    } else {
      if (subjectId != null) {
        review.recognizeSubject(subjectId);
      }
      review.observe(cause, now, evidence);
    }
  }

  /** Backfill único de ausentes y marcador se confirman junto con el catálogo principal. */
  private void completeTransition(
      PlayerSnapshot snapshot,
      Map<String, PlayerCandidate> candidates,
      Set<Long> protectedPlayers) {
    CatalogTransition transition =
        this.transitions
            .findForApplication()
            .orElseThrow(PlayerSynchronizationPersistenceException::new);
    Set<Long> presentPlayers = new HashSet<>();
    Set<String> presentExternalIds = new HashSet<>(candidates.keySet());
    snapshot
        .invalidPlayers()
        .forEach(observation -> presentExternalIds.add(observation.externalId()));
    for (String externalId : presentExternalIds) {
      this.referenceRepository
          .findByProviderAndExternalId(ExternalProvider.FOOTBALL_DATA, externalId)
          .ifPresent(reference -> presentPlayers.add(reference.getPlayer().getId()));
    }
    List<Team> current = this.teams.findByCurrentTrueOrderByIdAsc();
    for (Player player : this.playerRepository.findAll()) {
      if (protectedPlayers.contains(player.getId())) {
        continue;
      }
      if (player.getTeam() != null) {
        player.refreshLegacyMirror();
        continue;
      }
      if (transition.getCompletedAt() != null || presentPlayers.contains(player.getId())) {
        continue;
      }
      List<Team> matches =
          current.stream()
              .filter(
                  team ->
                      TeamNameNormalizer.normalize(team.getName())
                          .equals(TeamNameNormalizer.normalize(player.getLegacyTeam())))
              .toList();
      if (matches.size() == 1) {
        player.associateTeam(matches.getFirst());
      } else {
        String key = "LEGACY_TEAM_ASSOCIATION:PLAYER:" + player.getId();
        Map<String, Object> evidence = new LinkedHashMap<>();
        evidence.put("originalTeamName", player.getLegacyTeam());
        List<Map<String, Object>> observed = new ArrayList<>();
        matches.forEach(team -> observed.add(Map.of("id", team.getId(), "name", team.getName())));
        evidence.put("candidates", observed);
        ReviewCause cause =
            matches.isEmpty() ? ReviewCause.NO_TEAM_MATCH : ReviewCause.MULTIPLE_TEAM_MATCHES;
        PendingReviewCase review = this.cases.findByCaseKey(key).orElse(null);
        Instant now = Instant.now();
        if (review == null) {
          this.cases.save(
              new PendingReviewCase(
                  ReviewCategory.LEGACY_TEAM_ASSOCIATION,
                  cause,
                  ReviewSubjectType.PLAYER,
                  player.getId(),
                  null,
                  null,
                  key,
                  now,
                  evidence));
        } else {
          review.observe(cause, now, evidence);
        }
      }
    }
    if (transition.getCompletedAt() == null) {
      boolean covered =
          this.playerRepository.findAll().stream()
              .allMatch(
                  player ->
                      player.getTeam() != null
                          || this.cases
                              .findBySubjectTypeAndSubjectIdAndCategory(
                                  ReviewSubjectType.PLAYER,
                                  player.getId(),
                                  ReviewCategory.LEGACY_TEAM_ASSOCIATION)
                              .stream()
                              .anyMatch(
                                  review -> review.getEvidence().containsKey("originalTeamName"))
                          || this.cases
                              .findBySubjectTypeAndSubjectIdAndCategory(
                                  ReviewSubjectType.PLAYER,
                                  player.getId(),
                                  ReviewCategory.PLAYER_TEAM_UNRESOLVED)
                              .stream()
                              .anyMatch(
                                  review -> review.getEvidence().containsKey("originalTeamName"))
                          || this.cases
                              .findBySubjectTypeAndSubjectIdAndCategory(
                                  ReviewSubjectType.PLAYER,
                                  player.getId(),
                                  ReviewCategory.INVALID_SUBJECT_DATA)
                              .stream()
                              .anyMatch(
                                  review ->
                                      (review.getCauseCode() == ReviewCause.CONFLICTING_PLAYER_TEAMS
                                              || review.getCauseCode()
                                                  == ReviewCause.CONFLICTING_PLAYER_STATE)
                                          && review.getEvidence().get("originalTeamName")
                                              instanceof String original
                                          && !original.isBlank()
                                          && review.getEvidence().get("observations")
                                              instanceof List<?> observations
                                          && observations.size() > 1));
      if (!covered) {
        throw new PlayerSynchronizationPersistenceException();
      }
      transition.complete(Instant.now());
    }
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
    private ReviewSubjectType subjectType;

    private void setDomain(ReviewSubjectType subjectType, String externalId) {
      this.subjectType = subjectType;
      this.currentExternalId = externalId;
    }

    /**
     * Devuelve el identificador externo de la escritura actual.
     *
     * @return identificador externo o {@code null} si no hay una escritura atribuible
     */
    private String currentExternalId() {
      return this.currentExternalId;
    }

    /**
     * Registra el candidato cuya escritura se está realizando.
     *
     * @param externalId identificador externo
     */
    private void setCurrentExternalId(String externalId) {
      this.subjectType = null;
      this.currentExternalId = externalId;
    }

    /** Limpia el candidato asociado a la escritura actual. */
    private void clear() {
      this.currentExternalId = null;
      this.subjectType = null;
    }
  }
}
