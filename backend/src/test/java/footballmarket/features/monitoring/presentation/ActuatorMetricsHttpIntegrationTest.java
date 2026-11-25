package footballmarket.features.monitoring.presentation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import footballmarket.features.auth.application.AuthenticationService;
import footballmarket.features.auth.domain.User;
import footballmarket.support.TestcontainersConfiguration;
import io.jsonwebtoken.Jwts;
import java.time.Instant;
import java.util.Date;
import java.util.Set;
import java.util.stream.Collectors;
import javax.crypto.SecretKey;
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
import org.springframework.test.web.servlet.MvcResult;
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
  @Autowired private SecretKey jwtSecretKey;

  @Nested
  @DisplayName("Acceso público a métricas Prometheus")
  class PrometheusMetrics {
    @Test
    @DisplayName(
        "Devuelve las mismas familias de métricas sin JWT, con JWT válido, vencido o inválido")
    void devuelveMetricasIgualesConCualquierEstadoDeJwt() throws Exception {
      User user = new User("monitoring.metrics", "monitoring-metrics@test.com", "password123");
      authenticationService.register(user);
      String validToken = authenticationService.login(user.getEmail(), "password123");
      Instant now = Instant.now();
      String expiredToken =
          Jwts.builder()
              .subject(user.getEmail())
              .issuedAt(Date.from(now.minusSeconds(120)))
              .expiration(Date.from(now.minusSeconds(60)))
              .signWith(jwtSecretKey, Jwts.SIG.HS256)
              .compact();

      mvc.perform(get("/actuator/health")).andExpect(status().isOk());

      System.gc();

      MvcResult anonymousResult =
          mvc.perform(get("/actuator/prometheus"))
              .andExpect(status().isOk())
              .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_PLAIN))
              .andReturn();
      String anonymousMetrics = anonymousResult.getResponse().getContentAsString();
      Set<String> expectedFamilies = metricFamilies(anonymousMetrics);

      assertThat(anonymousMetrics)
          .contains(
              "# HELP",
              "system_cpu_usage",
              "process_cpu_usage",
              "jvm_memory_used_bytes",
              "jvm_threads_live_threads",
              "jvm_gc_pause_seconds",
              "http_server_requests_seconds_count")
          .doesNotContain("password", "secret", "token");

      MvcResult validResult =
          mvc.perform(get("/actuator/prometheus").header("Authorization", "Bearer " + validToken))
              .andExpect(status().isOk())
              .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_PLAIN))
              .andReturn();
      MvcResult expiredResult =
          mvc.perform(get("/actuator/prometheus").header("Authorization", "Bearer " + expiredToken))
              .andExpect(status().isOk())
              .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_PLAIN))
              .andReturn();
      MvcResult invalidResult =
          mvc.perform(get("/actuator/prometheus").header("Authorization", "Bearer invalid-token"))
              .andExpect(status().isOk())
              .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_PLAIN))
              .andReturn();

      assertThat(metricFamilies(validResult.getResponse().getContentAsString()))
          .containsExactlyInAnyOrderElementsOf(expectedFamilies);
      String validMetrics = validResult.getResponse().getContentAsString();
      String expiredMetrics = expiredResult.getResponse().getContentAsString();
      String invalidMetrics = invalidResult.getResponse().getContentAsString();

      assertThat(validMetrics).doesNotContain("password", "secret", "token");
      assertThat(metricFamilies(expiredMetrics))
          .containsExactlyInAnyOrderElementsOf(expectedFamilies);
      assertThat(expiredMetrics).doesNotContain("password", "secret", "token");
      assertThat(metricFamilies(invalidMetrics))
          .containsExactlyInAnyOrderElementsOf(expectedFamilies);
      assertThat(invalidMetrics).doesNotContain("password", "secret", "token");
    }

    private Set<String> metricFamilies(String exposition) {
      return exposition
          .lines()
          .filter(line -> line.startsWith("# TYPE "))
          .map(line -> line.substring("# TYPE ".length()).split(" ")[0])
          .collect(Collectors.toSet());
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
