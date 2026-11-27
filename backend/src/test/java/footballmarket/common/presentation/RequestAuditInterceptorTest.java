package footballmarket.common.presentation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/** Pruebas MVC del contrato de auditoría de peticiones REST. */
@Tag("lightweight")
@DisplayName("Auditoría de peticiones REST")
class RequestAuditInterceptorTest {

  private MockMvc mockMvc;
  private Logger auditLogger;
  private ListAppender<ILoggingEvent> logAppender;

  @BeforeEach
  void setUp() {
    mockMvc =
        MockMvcBuilders.standaloneSetup(new AuditController())
            .addInterceptors(new RequestAuditInterceptor())
            .build();
    auditLogger = (Logger) LoggerFactory.getLogger(RequestAuditInterceptor.class);
    logAppender = new ListAppender<>();
    logAppender.start();
    auditLogger.addAppender(logAppender);
    SecurityContextHolder.clearContext();
  }

  @AfterEach
  void tearDown() {
    auditLogger.detachAppender(logAppender);
    SecurityContextHolder.clearContext();
  }

  @Nested
  @DisplayName("Peticiones completadas")
  class CompletedRequests {

    @Test
    @DisplayName("Registra identidad autenticada y solo parámetros reconocidos no sensibles")
    void logsAuthenticatedRequestAndFiltersQueryParameters() throws Exception {
      // Arrange
      SecurityContextHolder.getContext()
          .setAuthentication(
              UsernamePasswordAuthenticationToken.authenticated("ada", "ignored", List.of()));

      // Act
      mockMvc
          .perform(
              org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(
                      "/api/audit/42")
                  .queryParam("term", "forward")
                  .queryParam("leagueId", "5")
                  .queryParam("password", "secret")
                  .queryParam("unknown", "ignored"))
          .andReturn();

      // Assert
      String message = onlyAuditMessage();
      assertThat(message)
          .contains("user=ada")
          .contains("method=GET")
          .contains("operation=/api/audit/{id}")
          .contains("parameters={term=[forward]}")
          .contains("durationMs=");
      assertThat(message)
          .doesNotContain("leagueId")
          .doesNotContain("password")
          .doesNotContain("secret")
          .doesNotContain("unknown")
          .doesNotContain("/api/audit/42");
    }

    @Test
    @DisplayName("Registra anonymous y omite parámetros cuando no hay valores reconocidos")
    void logsAnonymousRequestWithoutParameters() throws Exception {
      // Act
      mockMvc
          .perform(
              org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(
                  "/api/audit/7"))
          .andReturn();

      // Assert
      String message = onlyAuditMessage();
      assertThat(message)
          .contains("user=anonymous")
          .contains("method=GET")
          .contains("operation=/api/audit/{id}")
          .contains("parameters={}")
          .contains("durationMs=");
    }
  }

  @Nested
  @DisplayName("Peticiones con error")
  class FailedRequests {

    @Test
    @DisplayName("Registra la petición aunque el endpoint termine con una excepción")
    void logsRequestWhenHandlerFails() {
      // Act
      assertThatThrownBy(
              () ->
                  mockMvc
                      .perform(
                          org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(
                              "/api/audit/failure"))
                      .andReturn())
          .isInstanceOf(Exception.class);

      // Assert
      String message = onlyAuditMessage();
      assertThat(message)
          .contains("user=anonymous")
          .contains("method=GET")
          .contains("operation=/api/audit/failure")
          .contains("durationMs=");
    }
  }

  private String onlyAuditMessage() {
    assertThat(logAppender.list).hasSize(1);
    return logAppender.list.getFirst().getFormattedMessage();
  }

  @RestController
  static class AuditController {

    @GetMapping("/api/audit/{id}")
    void read(@PathVariable Long id, @ModelAttribute AuditQuery query) {}

    @GetMapping("/api/audit/failure")
    void fail() {
      throw new IllegalStateException("failure");
    }

    record AuditQuery(String term, Long leagueId, String password) {}
  }
}
