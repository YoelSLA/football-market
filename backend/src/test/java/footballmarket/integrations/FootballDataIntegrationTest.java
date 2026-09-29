package footballmarket.integrations;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

import footballmarket.integrations.exceptions.FootballDataUnavailableException;
import footballmarket.models.Player;
import java.net.SocketTimeoutException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class FootballDataIntegrationTest {
  private MockRestServiceServer server;
  private FootballDataIntegration integration;

  @BeforeEach
  void setUp() {
    var builder =
        RestClient.builder()
            .baseUrl("https://provider.example/v4")
            .defaultHeader("X-Auth-Token", "test-only");
    server = MockRestServiceServer.bindTo(builder).build();
    integration = new FootballDataIntegration(builder.build());
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
    @Test
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
      var result = integration.fetchCompetition("PL");
      assertThat(result.obtained()).isEqualTo(5);
      assertThat(result.discardedInvalid()).isEqualTo(4);
      assertThat(result.players()).extracting(Player::getName).containsExactly("Name");
      assertThat(result.players().getFirst().getLeague()).isEqualTo("League");
      assertThat(result.players().getFirst().getTeam()).isEqualTo("Team");
      server.verify();
    }

    @Test
    void descartaJugadoresSinLigaOEquipoSinInventarValores() {
      competition("null");
      respond(
          "/teams/10",
          "{\"name\":\"Team\",\"squad\":[{\"id\":1,\"name\":\"N\",\"position\":\"P\"}]}");
      assertThat(integration.fetchCompetition("PL").discardedInvalid()).isEqualTo(1);
      server.verify();
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
    @ValueSource(strings = {"{}", "{\"squad\":null}", "{\"squad\":{}}", "not-json", "null"})
    void rechazaPlantillaIncompleta(String body) {
      competition("\"League\"");
      respond("/teams/10", body);
      assertThatThrownBy(() -> integration.fetchCompetition("PL"))
          .isInstanceOf(FootballDataUnavailableException.class);
      server.verify();
    }

    @ParameterizedTest
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
    @ParameterizedTest
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
      server.verify();
    }

    @Test
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
