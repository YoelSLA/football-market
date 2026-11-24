package footballmarket.features.monitoring.presentation;

import static org.hamcrest.Matchers.containsString;
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
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@Transactional
@Tag("integration")
class ActuatorMetricsHttpIntegrationTest {
  @Autowired private MockMvc mvc;
  @Autowired private AuthenticationService authenticationService;

  @Nested
  @DisplayName("Métricas Prometheus")
  class PrometheusMetrics {
    @Test
    @DisplayName("Expone métricas estándar con un JWT válido")
    void exponeMetricasEstandarConIdentidadAutenticada() throws Exception {
      User user = new User("monitoring.metrics", "monitoring-metrics@test.com", "password123");
      authenticationService.register(user);
      String token = authenticationService.login(user.getEmail(), "password123");

      mvc.perform(get("/actuator/health")).andExpect(status().isOk());

      System.gc();

      mvc.perform(get("/actuator/prometheus").header("Authorization", "Bearer " + token))
          .andExpect(status().isOk())
          .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_PLAIN))
          .andExpect(content().string(containsString("# HELP")))
          .andExpect(content().string(containsString("system_cpu_usage")))
          .andExpect(content().string(containsString("process_cpu_usage")))
          .andExpect(content().string(containsString("jvm_memory_used_bytes")))
          .andExpect(content().string(containsString("jvm_threads_live_threads")))
          .andExpect(content().string(containsString("jvm_gc_pause_seconds")))
          .andExpect(content().string(containsString("http_server_requests_seconds_count")))
          .andExpect(content().string(org.hamcrest.Matchers.not(containsString("password"))))
          .andExpect(content().string(org.hamcrest.Matchers.not(containsString("secret"))))
          .andExpect(content().string(org.hamcrest.Matchers.not(containsString("token"))));
    }

    @Test
    @DisplayName("Expone health sin detalles de componentes")
    void exponeSoloElEstadoMinimoDeSalud() throws Exception {
      mvc.perform(get("/actuator/health"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.status").exists())
          .andExpect(jsonPath("$.components").doesNotExist())
          .andExpect(content().string(org.hamcrest.Matchers.not(containsString("password"))))
          .andExpect(content().string(org.hamcrest.Matchers.not(containsString("secret"))))
          .andExpect(content().string(org.hamcrest.Matchers.not(containsString("token"))));
    }
  }
}
