package footballmarket.integrations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import footballmarket.integrations.exceptions.InvalidTheSportsDbResponseException;
import footballmarket.integrations.exceptions.TheSportsDbRateLimitException;
import footballmarket.integrations.exceptions.TheSportsDbUnavailableException;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class TheSportsDbIntegrationTest {
  private static final String BASE =
      "https://provider.example/api/v1/json/test-only-not-a-credential";

  @Nested
  @DisplayName("Contratos de búsqueda y lookup v1")
  class V1Requests {
    @Test
    @DisplayName("La búsqueda usa la clave en la ruta y reutiliza la imagen de la misma respuesta")
    void searchUsesSingleResponse() {
      RestClient.Builder builder = RestClient.builder().baseUrl(BASE);
      MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
      server
          .expect(requestTo(BASE + "/searchplayers.php?p=Name"))
          .andRespond(
              withSuccess(
                  """
              {"player":[{"idPlayer":"123","strPlayer":"Name","strTeam":"Team",
              "strSport":"Soccer","strCutout":"https://r2.thesportsdb.com/cutout.png"}]}
              """,
                  MediaType.APPLICATION_JSON));
      TheSportsDbIntegration integration = createIntegration(builder);

      List<TheSportsDbIntegration.PlayerData> candidates = integration.search("Name");

      assertThat(candidates).hasSize(1);
      assertThat(candidates.getFirst().externalId()).isEqualTo("123");
      assertThat(candidates.getFirst().cutout()).isEqualTo("https://r2.thesportsdb.com/cutout.png");
      server.verify();
    }

    @Test
    @DisplayName("Una raíz nula en el lookup representa ausencia funcional de resultados")
    void handlesEmptyLookup() {
      RestClient.Builder builder = RestClient.builder().baseUrl(BASE);
      MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
      server
          .expect(requestTo(BASE + "/lookupplayer.php?id=123"))
          .andRespond(withSuccess("{\"players\":null}", MediaType.APPLICATION_JSON));

      assertThat(createIntegration(builder).lookup("123")).isEmpty();
      server.verify();
    }

    @Test
    @DisplayName("La búsqueda vacía es un resultado funcional, no un error técnico")
    void handlesEmptySearch() {
      RestClient.Builder builder = RestClient.builder().baseUrl(BASE);
      MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
      server
          .expect(requestTo(BASE + "/searchplayers.php?p=Name"))
          .andRespond(withSuccess("{\"player\":null}", MediaType.APPLICATION_JSON));
      assertThat(createIntegration(builder).search("Name")).isEmpty();
      server.verify();
    }

    @Test
    @DisplayName("Persiste el candidato único aunque no informe ninguna imagen")
    void candidateWithoutImage() {
      RestClient.Builder builder = RestClient.builder().baseUrl(BASE);
      MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
      server
          .expect(requestTo(BASE + "/searchplayers.php?p=Name"))
          .andRespond(
              withSuccess(
                  """
              {"player":[{"idPlayer":"123","strPlayer":"Name","strCutout":null,"strThumb":null}]}
              """,
                  MediaType.APPLICATION_JSON));
      List<TheSportsDbIntegration.PlayerData> candidates =
          createIntegration(builder).search("Name");
      assertThat(candidates).hasSize(1);
      assertThat(candidates.getFirst().externalId()).isEqualTo("123");
      assertThat(candidates.getFirst().cutout()).isNull();
      server.verify();
    }

    @Test
    @DisplayName("El error permanente 4xx no expone la clave de la URL")
    void rejectsPermanentHttpError() {
      RestClient.Builder builder = RestClient.builder().baseUrl(BASE);
      MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
      server
          .expect(requestTo(BASE + "/searchplayers.php?p=Name"))
          .andRespond(withStatus(HttpStatus.BAD_REQUEST));
      assertThatThrownBy(() -> createIntegration(builder).search("Name"))
          .isInstanceOf(InvalidTheSportsDbResponseException.class)
          .hasMessageNotContaining("test-only-not-a-credential");
      server.verify();
    }

    @Test
    @DisplayName("Los errores 5xx se traducen a indisponibilidad segura")
    void translatesServerError() {
      RestClient.Builder builder = RestClient.builder().baseUrl(BASE);
      MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
      server
          .expect(requestTo(BASE + "/searchplayers.php?p=Name"))
          .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));
      assertThatThrownBy(() -> createIntegration(builder).search("Name"))
          .isInstanceOf(TheSportsDbUnavailableException.class);
      server.verify();
    }

    @Test
    @DisplayName("Los fallos de red se traducen sin propagar la URL con la clave")
    void translatesNetworkError() {
      RestClient.Builder builder = RestClient.builder().baseUrl(BASE);
      MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
      server
          .expect(requestTo(BASE + "/searchplayers.php?p=Name"))
          .andRespond(withException(new IOException("Network unavailable")));
      assertThatThrownBy(() -> createIntegration(builder).search("Name"))
          .isInstanceOf(TheSportsDbUnavailableException.class)
          .hasMessageNotContaining("test-only-not-a-credential");
      server.verify();
    }

    @Test
    @DisplayName("Traduce el timeout de lectura sin incluir datos técnicos de la solicitud")
    void translatesTimeout() {
      RestClient.Builder builder = RestClient.builder().baseUrl(BASE);
      MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
      server
          .expect(requestTo(BASE + "/lookupplayer.php?id=123"))
          .andRespond(withException(new SocketTimeoutException("timeout")));
      assertThatThrownBy(() -> createIntegration(builder).lookup("123"))
          .isInstanceOf(TheSportsDbUnavailableException.class)
          .hasMessageNotContaining("test-only-not-a-credential");
      server.verify();
    }

    @Test
    @DisplayName("Un 429 sin Retry-After válido exige una espera de al menos un minuto")
    void handlesRateLimitWithoutHeader() {
      RestClient.Builder builder = RestClient.builder().baseUrl(BASE);
      MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
      server
          .expect(requestTo(BASE + "/searchplayers.php?p=Name"))
          .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS).header("Retry-After", "invalid"));
      Instant now = Instant.parse("2026-10-01T00:00:00Z");
      assertThatThrownBy(() -> createIntegration(builder).search("Name"))
          .isInstanceOf(TheSportsDbRateLimitException.class)
          .satisfies(
              exception ->
                  assertThat(((TheSportsDbRateLimitException) exception).getRetryAt())
                      .isEqualTo(now.plusSeconds(60)));
      server.verify();
    }

    @Test
    @DisplayName("Rechaza hostname por substring y conserva solo imágenes HTTPS del dominio")
    void rejectsUntrustedImageUrls() {
      RestClient.Builder builder = RestClient.builder().baseUrl(BASE);
      MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
      server
          .expect(requestTo(BASE + "/searchplayers.php?p=Name"))
          .andRespond(
              withSuccess(
                  """
              {"player":[{"idPlayer":"123","strPlayer":"Name",
              "strCutout":"https://thesportsdb.com.attacker.example/c.png",
              "strThumb":"http://thesportsdb.com/t.png"}]}
              """,
                  MediaType.APPLICATION_JSON));

      List<TheSportsDbIntegration.PlayerData> candidates =
          createIntegration(builder).search("Name");

      assertThat(candidates.getFirst().cutout()).isNull();
      assertThat(candidates.getFirst().thumbnail()).isNull();
      server.verify();
    }

    @Test
    @DisplayName("Rechaza respuestas sin la raíz esperada sin exponer la URL del proveedor")
    void rejectsMissingRoot() {
      RestClient.Builder builder = RestClient.builder().baseUrl(BASE);
      MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
      server
          .expect(requestTo(BASE + "/searchplayers.php?p=Name"))
          .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

      assertThatThrownBy(() -> createIntegration(builder).search("Name"))
          .isInstanceOf(InvalidTheSportsDbResponseException.class)
          .hasMessageNotContaining("test-only-not-a-credential");
      server.verify();
    }
  }

  private TheSportsDbIntegration createIntegration(RestClient.Builder builder) {
    Clock clock = Clock.fixed(Instant.parse("2026-10-01T00:00:00Z"), ZoneOffset.UTC);
    TheSportsDbRequestPacer pacer = new TheSportsDbRequestPacer(2500, clock, duration -> {});
    return new TheSportsDbIntegration(builder.build(), pacer, clock);
  }
}
