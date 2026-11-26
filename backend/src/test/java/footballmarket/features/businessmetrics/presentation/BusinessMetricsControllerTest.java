package footballmarket.features.businessmetrics.presentation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import footballmarket.features.businessmetrics.application.BusinessMetricsService;
import footballmarket.features.businessmetrics.domain.records.BusinessMetricsTotals;
import footballmarket.features.businessmetrics.presentation.dtos.response.BusinessMetricsResponseDTO;
import footballmarket.security.PersistedIdentityValidator;
import footballmarket.security.SecurityConfig;
import io.jsonwebtoken.Jwts;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.Set;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/** Verifica el contrato HTTP y la autenticación de las consultas de métricas de negocio. */
@WebMvcTest(BusinessMetricsController.class)
@Import(SecurityConfig.class)
@ActiveProfiles("test")
@Tag("lightweight")
class BusinessMetricsControllerTest {
  private static final String HISTORICAL_PATH = "/api/business-metrics/historical";
  private static final String CURRENT_PATH = "/api/business-metrics/current";
  private static final Set<String> RESPONSE_FIELDS =
      Set.of(
          "purchases",
          "sales",
          "cancelledListings",
          "tokensTransferred",
          "creditsTradedInCents",
          "queriedAt");

  @Autowired private MockMvc mvc;
  @Autowired private SecretKey key;
  private final ObjectMapper objectMapper = new ObjectMapper();
  @MockitoBean private BusinessMetricsService service;
  @MockitoBean private PersistedIdentityValidator identityValidator;

  @BeforeEach
  void setUp() {
    when(identityValidator.exists("user@example.com")).thenReturn(true);
    when(identityValidator.exists("admin@example.com")).thenReturn(true);
    when(identityValidator.isSuperuser("admin@example.com")).thenReturn(true);
    BusinessMetricsTotals totals =
        new BusinessMetricsTotals(7L, 3L, 2L, 14L, 1250L, Instant.parse("2026-11-26T12:00:00Z"));
    when(service.getHistoricalMetrics()).thenReturn(totals);
    when(service.getCurrentMetrics()).thenReturn(totals);
  }

  @Nested
  @DisplayName("Contrato común de las métricas")
  class SharedContract {
    @Test
    @DisplayName("Ambas rutas devuelven exactamente los campos y valores agregados del contrato")
    void returnsExactSharedContract() throws Exception {
      // Act
      MvcResult historicalResult =
          mvc.perform(
                  get(HISTORICAL_PATH)
                      .header(HttpHeaders.AUTHORIZATION, authorization("user@example.com")))
              .andExpect(status().isOk())
              .andReturn();
      MvcResult currentResult =
          mvc.perform(
                  get(CURRENT_PATH)
                      .header(HttpHeaders.AUTHORIZATION, authorization("user@example.com")))
              .andExpect(status().isOk())
              .andReturn();

      // Assert
      JsonNode historical =
          objectMapper.readTree(historicalResult.getResponse().getContentAsString());
      JsonNode current = objectMapper.readTree(currentResult.getResponse().getContentAsString());
      assertThat(fieldNames(historical)).containsExactlyInAnyOrderElementsOf(RESPONSE_FIELDS);
      assertThat(fieldNames(current)).containsExactlyInAnyOrderElementsOf(RESPONSE_FIELDS);
      assertThat(historical).isEqualTo(current);
      assertThat(historical.get("purchases").longValue()).isEqualTo(7L);
      assertThat(historical.get("sales").longValue()).isEqualTo(3L);
      assertThat(historical.get("cancelledListings").longValue()).isEqualTo(2L);
      assertThat(historical.get("tokensTransferred").longValue()).isEqualTo(14L);
      assertThat(historical.get("creditsTradedInCents").longValue()).isEqualTo(1250L);
      assertThat(historical.get("queriedAt").textValue()).isEqualTo("2026-11-26T12:00:00Z");
    }

    @Test
    @DisplayName("Las anotaciones OpenAPI documentan las rutas protegidas y sus campos compartidos")
    void documentsBothProtectedRoutes() throws Exception {
      // Arrange
      Operation historical =
          BusinessMetricsController.class
              .getMethod("getHistoricalMetrics")
              .getAnnotation(Operation.class);
      Operation current =
          BusinessMetricsController.class
              .getMethod("getCurrentMetrics")
              .getAnnotation(Operation.class);
      SecurityRequirement security =
          BusinessMetricsController.class.getAnnotation(SecurityRequirement.class);
      Schema credits =
          BusinessMetricsResponseDTO.class
              .getRecordComponents()[4]
              .getAccessor()
              .getAnnotation(Schema.class);

      // Assert
      assertThat(historical).isNotNull();
      assertThat(current).isNotNull();
      assertThat(security.name()).isEqualTo("bearerAuth");
      assertThat(historical.description())
          .contains("registros", "Bearer", "contrato", "centésimas", "UTC");
      assertThat(current.description())
          .contains("Micrometer", "Bearer", "contrato", "centésimas", "UTC");
      assertThat(credits.description()).contains("centésimas de crédito");
    }
  }

  @Nested
  @DisplayName("Acceso autenticado")
  class Authentication {
    @Test
    @DisplayName("Rechaza solicitudes sin credenciales para ambas rutas")
    void rejectsRequestsWithoutCredentials() throws Exception {
      // Act / Assert
      mvc.perform(get(HISTORICAL_PATH)).andExpect(status().isUnauthorized());
      mvc.perform(get(CURRENT_PATH)).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Permite el acceso a cuentas autenticadas sin exigir un rol concreto")
    void permitsAuthenticatedAccountsRegardlessOfRole() throws Exception {
      // Act / Assert
      mvc.perform(
              get(HISTORICAL_PATH)
                  .header(HttpHeaders.AUTHORIZATION, authorization("user@example.com")))
          .andExpect(status().isOk());
      mvc.perform(
              get(CURRENT_PATH)
                  .header(HttpHeaders.AUTHORIZATION, authorization("admin@example.com")))
          .andExpect(status().isOk());
    }
  }

  private String authorization(String subject) {
    return "Bearer "
        + Jwts.builder()
            .subject(subject)
            .expiration(Date.from(Instant.now().plusSeconds(300)))
            .signWith(key)
            .compact();
  }

  private Set<String> fieldNames(JsonNode json) {
    ArrayList<String> names = new ArrayList<>();
    json.fieldNames().forEachRemaining(names::add);
    return Set.copyOf(names);
  }
}
