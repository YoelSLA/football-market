package footballmarket.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import footballmarket.services.PlayerImageRunRecoveryService;
import footballmarket.services.exceptions.PlayerImageResolutionPersistenceException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class PlayerImageStartupConfigurationTest {
  @Nested
  @DisplayName("Disparador de recuperación al iniciar el contexto")
  class Startup {
    @Test
    @DisplayName("Invoca la recuperación al iniciar sin llamar a ningún proveedor")
    void invokesRecovery() {
      // Arrange
      PlayerImageRunRecoveryService recovery = mock(PlayerImageRunRecoveryService.class);
      ApplicationContextRunner context =
          new ApplicationContextRunner()
              .withBean(PlayerImageRunRecoveryService.class, () -> recovery)
              .withUserConfiguration(PlayerImageStartupConfiguration.class);

      // Act / Assert
      context.run(
          started -> {
            assertThat(started).hasNotFailed();
            verify(recovery).recover();
            verifyNoMoreInteractions(recovery);
          });
    }

    @Test
    @DisplayName("Un error persistente de recuperación impide iniciar el contexto")
    void abortsStartupOnFailure() {
      // Arrange
      PlayerImageRunRecoveryService recovery = mock(PlayerImageRunRecoveryService.class);

      doThrow(new PlayerImageResolutionPersistenceException()).when(recovery).recover();

      ApplicationContextRunner context =
          new ApplicationContextRunner()
              .withBean(PlayerImageRunRecoveryService.class, () -> recovery)
              .withUserConfiguration(PlayerImageStartupConfiguration.class);

      // Act / Assert
      context.run(
          started -> {
            assertThat(started).hasFailed();

            assertThat(started.getStartupFailure())
                .isInstanceOf(PlayerImageResolutionPersistenceException.class);

            verify(recovery).recover();
            verifyNoMoreInteractions(recovery);
          });
    }
  }
}
