package footballmarket.integrations;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

import footballmarket.integrations.exceptions.FootballDataUnavailableException;
import footballmarket.models.records.PlayerCandidate;
import footballmarket.models.records.PlayerSnapshot;
import java.net.SocketTimeoutException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class FootballDataIntegrationTest {
  private MockRestServiceServer server;
  private FootballDataIntegration integration;
  private final List<Duration> waits = new ArrayList<>();

  @BeforeEach
  void setUp() {
    RestClient.Builder builder =
        RestClient.builder()
            .baseUrl("https://provider.example/v4")
            .defaultHeader("X-Auth-Token", "test-only");
    server = MockRestServiceServer.bindTo(builder).build();
    waits.clear();
    integration =
        new FootballDataIntegration(
            builder.build(),
            Clock.fixed(Instant.parse("2026-10-01T00:00:00Z"), ZoneOffset.UTC),
            waits::add);
  }

  private void respond(String path, String body) {
    server
        .expect(requestTo("https://provider.example/v4" + path))
        .andExpect(header("X-Auth-Token", "test-only"))
        .andRespond(withSuccess(body, MediaType.APPLICATION_JSON));
  }

  private void competition(String name) {
    respond("/competitions/PL", "{\"name\":" + name + "}");
    respond("/competitions/PL/teams", "{\"teams\":[{\"id\":10}]}");
  }

  @Nested
  @DisplayName("Mapeo de jugadores del proveedor")
  class PlayerMapping {
    @ParameterizedTest
    @ValueSource(
        strings = {"null", "\"\"", "\" \"", "\"not-a-date\"", "\"2026-02-30\"", "42", "{}", "[]"})
    @DisplayName("Una fecha opcional inválida no descarta al jugador")
    void conservaJugadorConFechaInvalida(String jsonValue) {
      competition("\"League\"");
      respond(
          "/teams/10",
          "{\"name\":\"T\",\"squad\":[{\"id\":44,\"name\":\"N\",\"position\":\"P\",\"dateOfBirth\":"
              + jsonValue
              + "}]}");
      PlayerSnapshot result = integration.fetchCompetition("PL");
      assertThat(result.discardedInvalid()).isZero();
      assertThat(result.players())
          .singleElement()
          .satisfies(
              candidate -> {
                assertThat(candidate.dateOfBirth()).isNull();
                assertThat(candidate.nationality()).isNull();
              });
      server.verify();
    }

    @ParameterizedTest
    @ValueSource(strings = {"null", "\"\"", "\" \"", "42", "{}", "[]", "true"})
    @DisplayName("Una nacionalidad opcional inválida no descarta al jugador")
    void conservaJugadorConNacionalidadInvalida(String jsonValue) {
      competition("\"League\"");
      respond(
          "/teams/10",
          "{\"name\":\"T\",\"squad\":[{\"id\":44,\"name\":\"N\",\"position\":\"P\",\"nationality\":"
              + jsonValue
              + "}]}");
      PlayerSnapshot result = integration.fetchCompetition("PL");
      assertThat(result.discardedInvalid()).isZero();
      assertThat(result.players().getFirst().nationality()).isNull();
      server.verify();
    }

    @Test
    @DisplayName("Lee los opcionales válidos e ignora imágenes del proveedor")
    void leeOpcionalesValidos() {
      competition("\"League\"");
      respond(
          "/teams/10",
          """
          {"name":"T","squad":[{"id":44,"name":"N","position":"P",
          "dateOfBirth":"1990-06-20","nationality":"Spain","imageUrl":"https://unused.example/image"}]}
          """);
      PlayerSnapshot result = integration.fetchCompetition("PL");
      assertThat(result.players().getFirst().dateOfBirth()).isEqualTo(LocalDate.of(1990, 6, 20));
      assertThat(result.players().getFirst().nationality()).isEqualTo("Spain");
      server.verify();
    }

    @Test
    @DisplayName("Mapea jugadores válidos y contabiliza cada registro descartado")
    void mapeaJugadoresValidosYCuentaCadaRegistroInvalido() {
      competition("\"League\"");
      respond(
          "/teams/10",
          """
          {"name":"Team","squad":[
          {"id":1,"name":"Name","position":"Forward"},
          {"name":"Missing id","position":"Forward"},
          {"id":2,"name":" ","position":"Forward"},
          {"id":3,"name":"Missing position"},null]}
          """);
      // Act
      PlayerSnapshot result = integration.fetchCompetition("PL");
      // Assert
      assertThat(result.obtained()).isEqualTo(5);
      assertThat(result.discardedInvalid()).isEqualTo(4);
      assertThat(result.players()).extracting(PlayerCandidate::name).containsExactly("Name");
      assertThat(result.players().getFirst().externalId()).isEqualTo("1");
      assertThat(result.players().getFirst().league()).isEqualTo("League");
      assertThat(result.players().getFirst().team()).isEqualTo("Team");
      server.verify();
    }

    @Test
    @DisplayName("Descarta jugadores cuya liga no está informada")
    void descartaJugadoresSinLigaOEquipoSinInventarValores() {
      competition("null");
      respond(
          "/teams/10",
          "{\"name\":\"Team\",\"squad\":[{\"id\":1,\"name\":\"N\",\"position\":\"P\"}]}");
      assertThat(integration.fetchCompetition("PL").discardedInvalid()).isEqualTo(1);
      server.verify();
    }

    @Test
    @DisplayName("Descarta jugadores cuyo equipo no está informado")
    void descartaJugadoresSinEquipo() {
      server.reset();
      competition("\"League\"");
      respond("/teams/10", "{\"squad\":[{\"id\":1,\"name\":\"N\",\"position\":\"P\"}]}");
      assertThat(integration.fetchCompetition("PL").discardedInvalid()).isEqualTo(1);
      server.verify();
    }
  }

  @Nested
  @DisplayName("Validación de las respuestas del proveedor")
  class ResponseValidation {
    @ParameterizedTest
    @DisplayName("Rechaza plantillas incompletas o mal formadas")
    @ValueSource(strings = {"{}", "{\"squad\":null}", "{\"squad\":{}}", "not-json", "null"})
    void rechazaPlantillaIncompleta(String body) {
      competition("\"League\"");
      respond("/teams/10", body);
      assertThatThrownBy(() -> integration.fetchCompetition("PL"))
          .isInstanceOf(FootballDataUnavailableException.class);
      server.verify();
    }

    @ParameterizedTest
    @DisplayName("Rechaza listas de equipos incompletas")
    @ValueSource(
        strings = {"{}", "{\"teams\":null}", "{\"teams\":[{}]}", "{\"teams\":[null]}", "null"})
    void rechazaListaDeEquiposIncompleta(String body) {
      respond("/competitions/PL", "{\"name\":\"League\"}");
      respond("/competitions/PL/teams", body);
      assertThatThrownBy(() -> integration.fetchCompetition("PL"))
          .isInstanceOf(FootballDataUnavailableException.class);
      server.verify();
    }

    @Test
    @DisplayName("Acepta una plantilla completa sin jugadores")
    void aceptaPlantillaCompletaVacia() {
      competition("\"League\"");
      respond("/teams/10", "{\"name\":\"Team\",\"squad\":[]}");
      assertThat(integration.fetchCompetition("PL").obtained()).isZero();
      server.verify();
    }
  }

  @Nested
  @DisplayName("Errores y reintentos del proveedor")
  class ProviderFailures {
    @Test
    @DisplayName("Mantiene la espera predeterminada cuando 429 no informa Retry-After")
    void usaEsperaPredeterminadaSinCabecera() {
      server
          .expect(requestTo("https://provider.example/v4/competitions/PL"))
          .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));
      respond("/competitions/PL", "{\"name\":\"League\"}");
      respond("/competitions/PL/teams", "{\"teams\":[]}");
      assertThat(integration.fetchCompetition("PL").obtained()).isZero();
      assertThat(waits).containsExactly(Duration.ofSeconds(60));
      server.verify();
    }

    @Test
    @DisplayName("No reintenta un error HTTP distinto después de un 429 recuperable")
    void detieneReintentosAnteOtroError() {
      server
          .expect(requestTo("https://provider.example/v4/competitions/PL"))
          .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS).header("Retry-After", "2"));
      server
          .expect(requestTo("https://provider.example/v4/competitions/PL"))
          .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));
      assertThatThrownBy(() -> integration.fetchCompetition("PL"))
          .isInstanceOf(FootballDataUnavailableException.class);
      assertThat(waits).containsExactly(Duration.ofSeconds(2));
      server.verify();
    }

    @ParameterizedTest
    @CsvSource(
        delimiter = '|',
        value = {"12|12", "Thu, 1 Oct 2026 00:00:15 GMT|15", "invalid|60", "-1|60"})
    @DisplayName("Respeta Retry-After numérico o fecha HTTP sin esperas reales en el test")
    void respetaEsperaYRecupera(String retryAfter, long seconds) {
      server
          .expect(requestTo("https://provider.example/v4/competitions/PL"))
          .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS).header("Retry-After", retryAfter));
      respond("/competitions/PL", "{\"name\":\"League\"}");
      respond("/competitions/PL/teams", "{\"teams\":[]}");
      assertThat(integration.fetchCompetition("PL").obtained()).isZero();
      assertThat(waits).containsExactly(Duration.ofSeconds(seconds));
      server.verify();
    }

    @ParameterizedTest
    @DisplayName("Traduce errores HTTP sin filtrar la respuesta ni reintentar")
    @ValueSource(ints = {301, 401, 500, 503})
    void rechazaErroresHttpSinExponerRespuestaNiReintentar(int status) {
      server
          .expect(requestTo("https://provider.example/v4/competitions/PL"))
          .andRespond(withStatus(HttpStatus.valueOf(status)).body("private-provider-response"));
      assertThatThrownBy(() -> integration.fetchCompetition("PL"))
          .isInstanceOf(FootballDataUnavailableException.class)
          .hasMessageNotContaining("private-provider-response")
          .hasMessageNotContaining("provider.example");
      server.verify();
    }

    @Test
    @DisplayName("Limita los reintentos ante demasiadas solicitudes")
    void reintentaElLimiteDeSolicitudesHastaElMaximoConfigurado() {
      for (int attempt = 0; attempt < 4; attempt++) {
        server
            .expect(requestTo("https://provider.example/v4/competitions/PL"))
            .andRespond(
                withStatus(HttpStatus.TOO_MANY_REQUESTS)
                    .header("Retry-After", "0")
                    .body("private-provider-response"));
      }

      assertThatThrownBy(() -> integration.fetchCompetition("PL"))
          .isInstanceOf(FootballDataUnavailableException.class)
          .hasMessageNotContaining("private-provider-response")
          .hasMessageNotContaining("provider.example");
      assertThat(waits).containsExactly(Duration.ZERO, Duration.ZERO, Duration.ZERO);
      server.verify();
    }

    @Test
    @DisplayName("Traduce un timeout sin reintentar ni filtrar detalles privados")
    void traduceTiempoDeEsperaAgotadoSinReintentar() {
      server
          .expect(requestTo("https://provider.example/v4/competitions/PL"))
          .andRespond(withException(new SocketTimeoutException("private-url")));
      assertThatThrownBy(() -> integration.fetchCompetition("PL"))
          .isInstanceOf(FootballDataUnavailableException.class)
          .hasMessageNotContaining("private-url");
      server.verify();
    }
  }
}
