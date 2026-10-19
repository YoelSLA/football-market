package footballmarket.integrations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import footballmarket.integrations.exceptions.WhoScoredStatisticsException;
import footballmarket.support.WhoScoredStatisticsProviderStub;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

/**
 * Ritmo por canal del pacer estadístico y aplazamiento por {@code Retry-After}.
 *
 * <p>Usa el proveedor controlado de loopback y esperas sustituibles: nunca accede a Internet ni
 * depende de tiempos reales.
 */
class WhoScoredStatisticsRequestPacerTest {
  private WhoScoredStatisticsProviderStub provider;
  private List<Long> waits;
  private AtomicLong now;
  private WhoScoredRequestPacer pacer;
  private RestClient client;

  @BeforeEach
  void setUp() throws Exception {
    provider = new WhoScoredStatisticsProviderStub();
    waits = new ArrayList<>();
    now = new AtomicLong(0);
    pacer =
        new WhoScoredRequestPacer(
            2500,
            now::get,
            milliseconds -> {
              waits.add(milliseconds);
              now.addAndGet(milliseconds);
            },
            Clock.fixed(Instant.parse("2026-10-15T12:00:00Z"), ZoneOffset.UTC));
    client = RestClient.builder().baseUrl(provider.baseUrl()).build();
  }

  @AfterEach
  void tearDown() {
    provider.close();
  }

  private void advance(long millis) {
    now.addAndGet(millis);
  }

  @Nested
  @DisplayName("Ritmo por canal y worker")
  class Rate {
    @Test
    @DisplayName("Dos inicios consecutivos del mismo worker respetan el intervalo configurado")
    void respetaIntervaloPorWorker() {
      // Arrange
      WhoScoredRequestPacer.StatisticsWorker worker = pacer.statisticsWorker(2500);
      provider.enqueue(200, "application/json", "{}", null);

      // Act
      pacer.executeStatistics(
          worker, null, () -> client.get().uri("/one").retrieve().toBodilessEntity());
      advance(1000);
      provider.enqueue(200, "application/json", "{}", null);
      pacer.executeStatistics(
          worker, null, () -> client.get().uri("/two").retrieve().toBodilessEntity());

      // Assert
      assertThat(waits).containsExactly(1500L);
      assertThat(provider.requests()).hasSize(2);
    }

    @Test
    @DisplayName("Workers distintos no se serializan entre sí")
    void workersIndependientes() {
      // Arrange
      WhoScoredRequestPacer.StatisticsWorker first = pacer.statisticsWorker(2500);
      WhoScoredRequestPacer.StatisticsWorker second = pacer.statisticsWorker(2500);
      provider.enqueue(200, "application/json", "{}", null);
      provider.enqueue(200, "application/json", "{}", null);

      // Act
      pacer.executeStatistics(
          first, null, () -> client.get().uri("/one").retrieve().toBodilessEntity());
      pacer.executeStatistics(
          second, null, () -> client.get().uri("/two").retrieve().toBodilessEntity());

      // Assert
      assertThat(waits).isEmpty();
    }
  }

  @Nested
  @DisplayName("Aplazamiento por Retry-After y corte de lote")
  class Deferral {
    @Test
    @DisplayName(
        "Un Retry-After numérico aplaza el siguiente inicio sin repetir la petición fallida")
    void aplazaSiguienteInicio() {
      // Arrange
      WhoScoredRequestPacer.StatisticsWorker worker = pacer.statisticsWorker(1);
      provider.enqueue(429, "application/json", "{}", "30");
      provider.enqueue(200, "application/json", "{}", null);

      // Act
      assertThatCode(
              () ->
                  pacer.executeStatistics(
                      worker,
                      null,
                      () -> {
                        client.get().uri("/limited").exchange((request, response) -> null);
                        pacer.defer("30");
                        return null;
                      }))
          .doesNotThrowAnyException();
      pacer.executeStatistics(
          worker, null, () -> client.get().uri("/after").retrieve().toBodilessEntity());

      // Assert
      assertThat(waits).containsExactly(30_000L);
      assertThat(provider.requests())
          .extracting(java.net.URI::getPath)
          .containsExactly("/limited", "/after");
    }

    @Test
    @DisplayName("Un plazo restante insuficiente interrumpe antes de iniciar otra navegación")
    void interrumpeSinPresupuesto() {
      // Arrange
      WhoScoredRequestPacer.StatisticsWorker worker = pacer.statisticsWorker(2500);
      provider.enqueue(200, "application/json", "{}", null);

      // Act
      assertThatThrownBy(
              () ->
                  pacer.executeStatistics(
                      worker,
                      Instant.parse("2026-10-15T12:00:00Z"),
                      () -> client.get().uri("/one").retrieve().toBodilessEntity()))
          .isInstanceOf(WhoScoredStatisticsException.class);

      // Assert
      assertThat(provider.requests()).isEmpty();
    }
  }
}
