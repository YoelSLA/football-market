package footballmarket.features.monitoring.security;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import footballmarket.features.auth.application.AuthenticationService;
import footballmarket.features.auth.domain.User;
import footballmarket.support.TestcontainersConfiguration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@Transactional
@Tag("integration")
class ActuatorMonitoringSecurityIntegrationTest {
  @Autowired private MockMvc mvc;
  @Autowired private AuthenticationService authenticationService;

  @Nested
  @DisplayName("Autenticación de Prometheus")
  class PrometheusAuthentication {
    @Test
    @DisplayName("Rechaza solicitudes anónimas y con token inválido")
    void rechazaSolicitudesAnonimasYTokensInvalidos() throws Exception {
      mvc.perform(get("/actuator/prometheus"))
          .andExpect(status().isUnauthorized())
          .andExpect(content().string(""));

      mvc.perform(get("/actuator/prometheus").header("Authorization", "Bearer invalid-token"))
          .andExpect(status().isUnauthorized())
          .andExpect(content().string(""));
    }

    @Test
    @DisplayName("Permite Prometheus a cualquier identidad con JWT válido")
    void permiteAccederAIdentidadesAutenticadas() throws Exception {
      User user = new User("monitoring.security", "monitoring-security@test.com", "password123");
      authenticationService.register(user);
      String token = authenticationService.login(user.getEmail(), "password123");

      mvc.perform(get("/actuator/prometheus").header("Authorization", "Bearer " + token))
          .andExpect(status().isOk());
    }
  }

  @Nested
  @DisplayName("Acceso público a información de monitoreo")
  class PublicInformation {
    @Test
    @DisplayName("Permite consultar info anónimamente sin información sensible")
    void permiteConsultarInfoAnonimamenteSinDatosSensibles() throws Exception {
      mvc.perform(get("/actuator/info"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.app.name").value("Football Market"))
          .andExpect(jsonPath("$.app.description").value("API de mercado de jugadores de fútbol"))
          .andExpect(content().string(not(containsString("password"))))
          .andExpect(content().string(not(containsString("secret"))))
          .andExpect(content().string(not(containsString("token"))));
    }
  }

  @Nested
  @DisplayName("Superficie HTTP de Actuator")
  class ActuatorExposure {
    @Test
    @DisplayName("No expone la raíz, subendpoints ni otros endpoints aun con JWT válido")
    void rechazaEndpointsNoExpuestosParaIdentidadAutenticada() throws Exception {
      User user = new User("monitoring.surface", "monitoring-surface@test.com", "password123");
      authenticationService.register(user);
      String token = authenticationService.login(user.getEmail(), "password123");
      String[] paths = {
        "/actuator",
        "/actuator/health/details",
        "/actuator/info/build",
        "/actuator/metrics",
        "/actuator/env"
      };

      for (String path : paths) {
        MvcResult result =
            mvc.perform(get(path).header("Authorization", "Bearer " + token))
                .andExpect(status().is4xxClientError())
                .andReturn();
        org.assertj.core.api.Assertions.assertThat(result.getResponse().getContentAsString())
            .doesNotContain("href");
      }
    }
  }
}
