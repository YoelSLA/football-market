package footballmarket.features.images.application.impl;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import footballmarket.features.catalog.domain.enums.ExternalProvider;
import footballmarket.features.catalog.domain.models.player.Player;
import footballmarket.features.catalog.domain.models.player.PlayerExternalReference;
import footballmarket.features.catalog.domain.models.team.Team;
import footballmarket.features.catalog.infraestructure.PlayerExternalReferenceRepository;
import footballmarket.features.catalog.infraestructure.PlayerRepository;
import footballmarket.features.images.domain.enums.PlayerImageResolutionStatus;
import footballmarket.features.images.domain.enums.PlayerImageSkipReason;
import footballmarket.features.images.domain.enums.PlayerImageSyncRunItemResult;
import footballmarket.features.images.domain.models.PlayerImageResolution;
import footballmarket.features.images.domain.models.PlayerImageSyncRun;
import footballmarket.features.images.domain.models.PlayerImageSyncRunItem;
import footballmarket.features.images.infraestructure.PlayerImageResolutionRepository;
import footballmarket.features.images.infraestructure.PlayerImageSyncRunItemRepository;
import footballmarket.features.images.infraestructure.PlayerImageSyncRunRepository;
import footballmarket.integrations.thesportsdb.TheSportsDbIntegration;
import footballmarket.integrations.thesportsdb.TheSportsDbIntegration.PlayerData;
import footballmarket.integrations.thesportsdb.config.TheSportsDbProperties;
import footballmarket.integrations.thesportsdb.exceptions.InvalidTheSportsDbResponseException;
import footballmarket.integrations.thesportsdb.exceptions.TheSportsDbRateLimitException;
import footballmarket.integrations.thesportsdb.exceptions.TheSportsDbUnavailableException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.postgresql.util.PSQLException;
import org.postgresql.util.ServerErrorMessage;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;

@org.junit.jupiter.api.Tag("unit")
class PlayerImageProcessorTest {
  private static final Long RUN_ID = 100L;
  private static final Long PLAYER_ID = 200L;
  private static final Long ITEM_ID = 300L;
  private static final String PLAYER_NAME = "Lionel Messi";
  private static final String TEAM_NAME = "Inter Miami";

  private PlayerRepository players;
  private PlayerExternalReferenceRepository references;
  private PlayerImageResolutionRepository resolutions;
  private PlayerImageSyncRunRepository runs;
  private PlayerImageSyncRunItemRepository items;
  private TheSportsDbIntegration integration;
  private TheSportsDbProperties properties;
  private PlatformTransactionManager transactionManager;

  private Player player;
  private Team team;
  private PlayerImageResolution resolution;
  private PlayerImageSyncRun run;
  private PlayerImageSyncRunItem item;

  private PlayerImageProcessor processor;

  @BeforeEach
  void setUp() {
    players = mock(PlayerRepository.class);
    references = mock(PlayerExternalReferenceRepository.class);
    resolutions = mock(PlayerImageResolutionRepository.class);
    runs = mock(PlayerImageSyncRunRepository.class);
    items = mock(PlayerImageSyncRunItemRepository.class);
    integration = mock(TheSportsDbIntegration.class);
    properties = mock(TheSportsDbProperties.class);
    transactionManager = mock(PlatformTransactionManager.class);

    // Mock transaction manager executing in-memory
    TransactionStatus status = mock(TransactionStatus.class);
    when(transactionManager.getTransaction(any())).thenReturn(status);

    when(properties.maxRetries()).thenReturn(2);
    when(properties.imageRetryDays()).thenReturn(7);

    // Default player setup
    player = mock(Player.class);
    team = mock(Team.class);
    when(team.getName()).thenReturn(TEAM_NAME);
    when(player.getId()).thenReturn(PLAYER_ID);
    when(player.getName()).thenReturn(PLAYER_NAME);
    when(player.isActive()).thenReturn(true);
    when(player.getTeam()).thenReturn(team);

    resolution = mock(PlayerImageResolution.class);
    when(resolution.getStatus()).thenReturn(PlayerImageResolutionStatus.PENDING);

    run = mock(PlayerImageSyncRun.class);
    item = mock(PlayerImageSyncRunItem.class);
    when(item.getId()).thenReturn(ITEM_ID);

    when(players.findById(PLAYER_ID)).thenReturn(Optional.of(player));
    when(resolutions.findById(PLAYER_ID)).thenReturn(Optional.of(resolution));
    when(runs.findById(RUN_ID)).thenReturn(Optional.of(run));
    when(items.findByRunIdAndPlayerId(RUN_ID, PLAYER_ID)).thenReturn(Optional.of(item));
    when(items.findById(ITEM_ID)).thenReturn(Optional.of(item));

    processor =
        new PlayerImageProcessor(
            players,
            references,
            resolutions,
            runs,
            items,
            integration,
            properties,
            transactionManager);
  }

  private PlayerData sampleCandidate(String extId, String cutout, String thumb) {
    return new PlayerData(extId, PLAYER_NAME, null, TEAM_NAME, "Soccer", null, null, cutout, thumb);
  }

  @Nested
  @DisplayName("Resiliencia ante colisiones de BD (isReferenceConflict)")
  class DatabaseConflictScenarios {
    @Test
    @DisplayName(
        "Captura violación de restricción única uk_player_external_references y finaliza con conflicto")
    void handlesPostgresReferenceUniqueConstraintConflict() {
      // Arrange
      PlayerData candidate = sampleCandidate("tsdb-1", "https://cutout.png", null);
      when(integration.search(PLAYER_NAME)).thenReturn(List.of(candidate));

      PSQLException postgres = mock(PSQLException.class);
      ServerErrorMessage serverError = mock(ServerErrorMessage.class);
      when(postgres.getSQLState()).thenReturn("23505");
      when(postgres.getServerErrorMessage()).thenReturn(serverError);
      when(serverError.getTable()).thenReturn("player_external_references");
      when(serverError.getConstraint())
          .thenReturn("uk_player_external_references_provider_external_id");

      DataIntegrityViolationException dive =
          new DataIntegrityViolationException("duplicate key violation", postgres);
      doThrow(dive).when(players).saveAndFlush(player);

      // Act
      boolean processed = processor.processPlayer(RUN_ID, PLAYER_ID, false);

      // Assert
      assertThat(processed).isTrue();
      verify(item)
          .finish(
              eq(PlayerImageResolutionStatus.FAILED),
              eq(PlayerImageSyncRunItemResult.FAILED),
              isNull(),
              eq(false),
              eq(false),
              eq(true), // conflict = true
              eq(true),
              eq("Identidad externa ya vinculada a otro jugador"),
              any(Instant.class));
      verify(run).result(eq(PlayerImageSyncRunItemResult.FAILED), isNull(), eq(true), eq(true));
      verify(resolution).recordAttempt(eq(PlayerImageResolutionStatus.FAILED), any(Instant.class));
    }

    @Test
    @DisplayName(
        "Relanza DataIntegrityViolationException si la restricción violada no es la de referencia externa")
    void rethrowsDataIntegrityViolationWhenConstraintDoesNotMatch() {
      // Arrange
      PlayerData candidate = sampleCandidate("tsdb-1", "https://cutout.png", null);
      when(integration.search(PLAYER_NAME)).thenReturn(List.of(candidate));

      PSQLException postgres = mock(PSQLException.class);
      ServerErrorMessage serverError = mock(ServerErrorMessage.class);
      when(postgres.getSQLState()).thenReturn("23505");
      when(postgres.getServerErrorMessage()).thenReturn(serverError);
      when(serverError.getTable()).thenReturn("other_table");
      when(serverError.getConstraint()).thenReturn("uk_other_constraint");

      DataIntegrityViolationException dive =
          new DataIntegrityViolationException("other table violation", postgres);
      doThrow(dive).when(players).saveAndFlush(player);

      // Act & Assert
      assertThatThrownBy(() -> processor.processPlayer(RUN_ID, PLAYER_ID, false)).isSameAs(dive);
    }

    @Test
    @DisplayName("Relanza DataIntegrityViolationException si la causa no es PSQLException")
    void rethrowsDataIntegrityViolationWhenCauseIsNotPostgres() {
      // Arrange
      PlayerData candidate = sampleCandidate("tsdb-1", "https://cutout.png", null);
      when(integration.search(PLAYER_NAME)).thenReturn(List.of(candidate));

      DataIntegrityViolationException dive =
          new DataIntegrityViolationException("generic dive", new RuntimeException("root cause"));
      doThrow(dive).when(players).saveAndFlush(player);

      // Act & Assert
      assertThatThrownBy(() -> processor.processPlayer(RUN_ID, PLAYER_ID, false)).isSameAs(dive);
    }
  }

  @Nested
  @DisplayName("Reintentos ante fallos del proveedor (Rate Limit y Disponibilidad)")
  class RetryAndProviderFailureScenarios {
    @Test
    @DisplayName(
        "Reintenta ante TheSportsDbRateLimitException y procesa exitosamente en segundo intento")
    void retriesOnRateLimitExceptionAndSucceeds() {
      // Arrange
      PlayerData candidate = sampleCandidate("tsdb-1", "https://cutout.png", null);
      when(integration.search(PLAYER_NAME))
          .thenThrow(new TheSportsDbRateLimitException(Instant.now().plusSeconds(60)))
          .thenReturn(List.of(candidate));

      // Act
      boolean processed = processor.processPlayer(RUN_ID, PLAYER_ID, false);

      // Assert
      assertThat(processed).isTrue();
      verify(integration, times(2)).search(PLAYER_NAME);
      verify(resolution, times(2)).markAttempt(any(Instant.class));
      verify(player).applyImages("https://cutout.png", null);
      verify(players).saveAndFlush(player);
    }

    @Test
    @DisplayName("Reintenta ante TheSportsDbUnavailableException y procesa exitosamente")
    void retriesOnUnavailableExceptionAndSucceeds() {
      // Arrange
      PlayerData candidate = sampleCandidate("tsdb-1", "https://cutout.png", null);
      when(integration.search(PLAYER_NAME))
          .thenThrow(new TheSportsDbUnavailableException())
          .thenReturn(List.of(candidate));

      // Act
      boolean processed = processor.processPlayer(RUN_ID, PLAYER_ID, false);

      // Assert
      assertThat(processed).isTrue();
      verify(integration, times(2)).search(PLAYER_NAME);
      verify(resolution, times(2)).markAttempt(any(Instant.class));
    }

    @Test
    @DisplayName("Agotados los reintentos temporales finaliza con RETRYABLE_ERROR")
    void exhaustsRetriesAndFinishesWithRetryableError() {
      // Arrange (maxRetries = 2, so 3 attempts total)
      when(integration.search(PLAYER_NAME))
          .thenThrow(new TheSportsDbUnavailableException())
          .thenThrow(new TheSportsDbUnavailableException())
          .thenThrow(new TheSportsDbUnavailableException());

      // Act
      boolean processed = processor.processPlayer(RUN_ID, PLAYER_ID, false);

      // Assert
      assertThat(processed).isTrue();
      verify(integration, times(3)).search(PLAYER_NAME);
      verify(resolution, times(3)).markAttempt(any(Instant.class));
      verify(item)
          .finish(
              eq(PlayerImageResolutionStatus.RETRYABLE_ERROR),
              eq(PlayerImageSyncRunItemResult.RETRYABLE_ERROR),
              isNull(),
              eq(false),
              eq(false),
              eq(false),
              eq(true),
              eq("El proveedor no devolvió datos utilizables"),
              any(Instant.class));
      verify(resolution)
          .recordAttempt(eq(PlayerImageResolutionStatus.RETRYABLE_ERROR), any(Instant.class));
    }

    @Test
    @DisplayName(
        "InvalidTheSportsDbResponseException aborta inmediatamente sin reintentar y finaliza FAILED")
    void doesNotRetryOnInvalidResponseException() {
      // Arrange
      when(integration.search(PLAYER_NAME)).thenThrow(new InvalidTheSportsDbResponseException());

      // Act
      boolean processed = processor.processPlayer(RUN_ID, PLAYER_ID, false);

      // Assert
      assertThat(processed).isTrue();
      verify(integration, times(1)).search(PLAYER_NAME);
      verify(item)
          .finish(
              eq(PlayerImageResolutionStatus.FAILED),
              eq(PlayerImageSyncRunItemResult.FAILED),
              isNull(),
              eq(false),
              eq(false),
              eq(false),
              eq(true),
              eq("El proveedor no devolvió datos utilizables"),
              any(Instant.class));
      verify(resolution).recordAttempt(eq(PlayerImageResolutionStatus.FAILED), any(Instant.class));
    }

    @Test
    @DisplayName("Candidatos nulos finalizan con FAILED de forma permanente")
    void finishesPermanentlyFailedWhenCandidatesNull() {
      // Arrange
      when(integration.search(PLAYER_NAME)).thenReturn(null);

      // Act
      boolean processed = processor.processPlayer(RUN_ID, PLAYER_ID, false);

      // Assert
      assertThat(processed).isTrue();
      verify(item)
          .finish(
              eq(PlayerImageResolutionStatus.FAILED),
              eq(PlayerImageSyncRunItemResult.FAILED),
              isNull(),
              eq(false),
              eq(false),
              eq(false),
              eq(true),
              eq("El proveedor no devolvió datos utilizables"),
              any(Instant.class));
    }
  }

  @Nested
  @DisplayName("Filtros de elegibilidad y Skip")
  class EligibilityAndSkipScenarios {
    @Test
    @DisplayName("Omite jugador inactivo sin consultar al proveedor")
    void skipsInactivePlayer() {
      when(player.isActive()).thenReturn(false);

      boolean processed = processor.processPlayer(RUN_ID, PLAYER_ID, false);

      assertThat(processed).isFalse();
      verify(integration, never()).search(anyString());
    }

    @Test
    @DisplayName("Omite jugador sin equipo sin consultar al proveedor")
    void skipsPlayerWithoutTeam() {
      when(player.getTeam()).thenReturn(null);

      boolean processed = processor.processPlayer(RUN_ID, PLAYER_ID, false);

      assertThat(processed).isFalse();
      verify(integration, never()).search(anyString());
    }

    @Test
    @DisplayName("Omite jugador FOUND sin force; con force procede a la consulta remota")
    void skipsFoundUnlessForce() {
      when(resolution.getStatus()).thenReturn(PlayerImageResolutionStatus.FOUND);

      // Without force
      boolean processedWithoutForce = processor.processPlayer(RUN_ID, PLAYER_ID, false);
      assertThat(processedWithoutForce).isFalse();
      verify(item)
          .finish(
              eq(PlayerImageResolutionStatus.FOUND),
              eq(PlayerImageSyncRunItemResult.SKIPPED),
              eq(PlayerImageSkipReason.FOUND),
              eq(false),
              eq(false),
              eq(false),
              eq(false),
              eq("Omitido por elegibilidad"),
              any(Instant.class));
      verify(integration, never()).search(anyString());

      // With force
      when(integration.search(PLAYER_NAME))
          .thenReturn(List.of(sampleCandidate("tsdb-1", "https://img.png", null)));
      boolean processedWithForce = processor.processPlayer(RUN_ID, PLAYER_ID, true);
      assertThat(processedWithForce).isTrue();
      verify(integration, times(1)).search(PLAYER_NAME);
    }

    @Test
    @DisplayName("Omite jugador FAILED sin force; con force procede")
    void skipsFailedUnlessForce() {
      when(resolution.getStatus()).thenReturn(PlayerImageResolutionStatus.FAILED);

      boolean processedWithoutForce = processor.processPlayer(RUN_ID, PLAYER_ID, false);
      assertThat(processedWithoutForce).isFalse();
      verify(item)
          .finish(
              eq(PlayerImageResolutionStatus.FAILED),
              eq(PlayerImageSyncRunItemResult.SKIPPED),
              eq(PlayerImageSkipReason.FAILED),
              anyBoolean(),
              anyBoolean(),
              anyBoolean(),
              anyBoolean(),
              anyString(),
              any());

      when(integration.search(PLAYER_NAME))
          .thenReturn(List.of(sampleCandidate("tsdb-1", "https://img.png", null)));
      boolean processedWithForce = processor.processPlayer(RUN_ID, PLAYER_ID, true);
      assertThat(processedWithForce).isTrue();
    }

    @Test
    @DisplayName("Omite NOT_FOUND dentro de la ventana de reintento sin force")
    void skipsNotFoundWithinRetryWindow() {
      when(resolution.getStatus()).thenReturn(PlayerImageResolutionStatus.NOT_FOUND);
      when(resolution.getLastAttemptAt())
          .thenReturn(Instant.now().minusSeconds(3600)); // 1 hour ago (window is 7 days)

      boolean processed = processor.processPlayer(RUN_ID, PLAYER_ID, false);

      assertThat(processed).isFalse();
      verify(item)
          .finish(
              eq(PlayerImageResolutionStatus.NOT_FOUND),
              eq(PlayerImageSyncRunItemResult.SKIPPED),
              eq(PlayerImageSkipReason.RETRY_WINDOW),
              anyBoolean(),
              anyBoolean(),
              anyBoolean(),
              anyBoolean(),
              anyString(),
              any());
      verify(integration, never()).search(anyString());
    }
  }

  @Nested
  @DisplayName("Consultas por External ID y Colisiones de Aplicación")
  class ExternalIdLookupAndApplicationConflictScenarios {
    @Test
    @DisplayName("Usa lookup en lugar de search cuando ya existe referencia externa")
    void usesLookupWhenExternalIdPresent() {
      PlayerExternalReference ref = mock(PlayerExternalReference.class);
      when(ref.getExternalId()).thenReturn("tsdb-existing");
      when(references.findByPlayerIdAndProvider(PLAYER_ID, ExternalProvider.THE_SPORTS_DB))
          .thenReturn(Optional.of(ref));

      PlayerData candidate = sampleCandidate("tsdb-existing", "https://new-cutout.png", null);
      when(integration.lookup("tsdb-existing")).thenReturn(List.of(candidate));

      boolean processed = processor.processPlayer(RUN_ID, PLAYER_ID, true);

      assertThat(processed).isTrue();
      verify(integration).lookup("tsdb-existing");
      verify(integration, never()).search(anyString());
      verify(players).saveAndFlush(player);
    }

    @Test
    @DisplayName("Falla si lookup devuelve más de un candidato")
    void failsWhenLookupReturnsMultipleCandidates() {
      PlayerExternalReference ref = mock(PlayerExternalReference.class);
      when(ref.getExternalId()).thenReturn("tsdb-existing");
      when(references.findByPlayerIdAndProvider(PLAYER_ID, ExternalProvider.THE_SPORTS_DB))
          .thenReturn(Optional.of(ref));

      PlayerData c1 = sampleCandidate("tsdb-existing", null, null);
      PlayerData c2 = sampleCandidate("tsdb-other", null, null);
      when(integration.lookup("tsdb-existing")).thenReturn(List.of(c1, c2));

      boolean processed = processor.processPlayer(RUN_ID, PLAYER_ID, true);

      assertThat(processed).isTrue();
      verify(item)
          .finish(
              eq(PlayerImageResolutionStatus.FAILED),
              eq(PlayerImageSyncRunItemResult.FAILED),
              isNull(),
              eq(false),
              eq(false),
              eq(false),
              eq(true),
              eq("Consulta de identidad inválida"),
              any());
    }

    @Test
    @DisplayName("Falla si lookup devuelve candidato con externalId distinto")
    void failsWhenLookupReturnsDifferentExternalId() {
      PlayerExternalReference ref = mock(PlayerExternalReference.class);
      when(ref.getExternalId()).thenReturn("tsdb-existing");
      when(references.findByPlayerIdAndProvider(PLAYER_ID, ExternalProvider.THE_SPORTS_DB))
          .thenReturn(Optional.of(ref));

      PlayerData mismatch = sampleCandidate("tsdb-different", null, null);
      when(integration.lookup("tsdb-existing")).thenReturn(List.of(mismatch));

      boolean processed = processor.processPlayer(RUN_ID, PLAYER_ID, true);

      assertThat(processed).isTrue();
      verify(item)
          .finish(
              eq(PlayerImageResolutionStatus.FAILED),
              eq(PlayerImageSyncRunItemResult.FAILED),
              isNull(),
              eq(false),
              eq(false),
              eq(false),
              eq(true),
              eq("Identidad incompatible con el jugador"),
              any());
    }

    @Test
    @DisplayName(
        "Detecta conflicto a nivel de aplicación cuando la referencia ya pertenece a otro jugador")
    void detectsApplicationLevelOwnerConflict() {
      PlayerData candidate = sampleCandidate("tsdb-shared", "https://img.png", null);
      when(integration.search(PLAYER_NAME)).thenReturn(List.of(candidate));

      Player otherPlayer = mock(Player.class);
      when(otherPlayer.getId()).thenReturn(999L); // Different player id
      PlayerExternalReference existingOwnerRef = mock(PlayerExternalReference.class);
      when(existingOwnerRef.getPlayer()).thenReturn(otherPlayer);

      when(references.findByProviderAndExternalId(ExternalProvider.THE_SPORTS_DB, "tsdb-shared"))
          .thenReturn(Optional.of(existingOwnerRef));

      boolean processed = processor.processPlayer(RUN_ID, PLAYER_ID, false);

      assertThat(processed).isTrue();
      verify(item)
          .finish(
              eq(PlayerImageResolutionStatus.FAILED),
              eq(PlayerImageSyncRunItemResult.FAILED),
              isNull(),
              eq(false),
              eq(false),
              eq(true), // conflict = true
              eq(true),
              eq("Identidad externa ya vinculada a otro jugador"),
              any());
      verify(player, never()).addExternalReference(any(), any());
      verify(players, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("Finaliza NOT_FOUND cuando la búsqueda no produce candidatos coincidentes")
    void finishesNotFoundWhenNoCandidatesMatch() {
      when(integration.search(PLAYER_NAME)).thenReturn(List.of());

      boolean processed = processor.processPlayer(RUN_ID, PLAYER_ID, false);

      assertThat(processed).isTrue();
      verify(item)
          .finish(
              eq(PlayerImageResolutionStatus.NOT_FOUND),
              eq(PlayerImageSyncRunItemResult.NOT_FOUND),
              isNull(),
              eq(false),
              eq(false),
              eq(false),
              eq(false),
              eq("Sin imagen o identidad confiable"),
              any());
    }
  }
}
