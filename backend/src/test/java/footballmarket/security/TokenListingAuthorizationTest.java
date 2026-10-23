package footballmarket.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import footballmarket.models.User;
import footballmarket.services.AuthenticationService;
import footballmarket.services.SuperuserProvisioningService;
import footballmarket.support.TestcontainersConfiguration;
import io.jsonwebtoken.Jwts;
import java.time.Instant;
import java.util.Date;
import java.util.concurrent.atomic.AtomicLong;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/** Autorización de mercado con JWT y servicios reales, sin acceso a Repository desde Security. */
@SpringBootTest(
    properties = {
      "footballmarket.superuser.enabled=true",
      "footballmarket.superuser.username=listingowner",
      "footballmarket.superuser.email=listingowner@example.com",
      "footballmarket.superuser.password=fixture-password-123"
    })
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
@org.junit.jupiter.api.Tag("integration")
class TokenListingAuthorizationTest {
  private static final AtomicLong SEQUENCE = new AtomicLong();
  @Autowired private AuthenticationService authentication;
  @Autowired private SuperuserProvisioningService provisioning;
  @Autowired private MockMvc mvc;
  @Autowired private SecretKey key;
  private User normal;
  private User superuser;

  @BeforeEach
  void arrange() {
    long id = SEQUENCE.incrementAndGet();
    normal =
        new User("listinguser" + id, "listinguser" + id + "@example.com", "fixture-password-123");
    authentication.register(normal);
    superuser = provisioning.findSuperuser().orElseThrow();
  }

  private String misleadingToken(User user, String claimedRole) {
    return "Bearer "
        + Jwts.builder()
            .subject(user.getEmail())
            .claim("role", claimedRole)
            .expiration(Date.from(Instant.now().plusSeconds(300)))
            .signWith(key)
            .compact();
  }

  @Nested
  @DisplayName("El rol persistido prevalece sobre los claims")
  class PersistedRole {
    @Test
    @DisplayName("Un SUPERUSER persistido no publica aunque el JWT afirme USER")
    void rechazaPublicacionSuperusuario() throws Exception {
      // Act / Assert
      mvc.perform(
              post("/api/orders/sell")
                  .header("Authorization", misleadingToken(superuser, "USER"))
                  .contentType(MediaType.APPLICATION_JSON)
                  .content("{\"playerId\":-1,\"quantity\":1}"))
          .andExpect(status().isForbidden())
          .andExpect(jsonPath("$.code").value("SUPERUSER_CANNOT_SELL"));
    }

    @Test
    @DisplayName("Un USER persistido no hereda la prohibición de SUPERUSER desde un claim")
    void ignoraClaimSuperusuario() throws Exception {
      // Act / Assert
      mvc.perform(
              post("/api/orders/sell")
                  .header("Authorization", misleadingToken(normal, "SUPERUSER"))
                  .contentType(MediaType.APPLICATION_JSON)
                  .content("{\"playerId\":-1,\"quantity\":1}"))
          .andExpect(status().isNotFound())
          .andExpect(jsonPath("$.code").value("PLAYER_NOT_FOUND"));
    }

    @Test
    @DisplayName("El SUPERUSER tampoco compra publicaciones aunque el JWT afirme USER")
    void rechazaCompraSuperusuario() throws Exception {
      // Act / Assert
      mvc.perform(
              post("/api/orders/buy")
                  .header("Authorization", misleadingToken(superuser, "USER"))
                  .contentType(MediaType.APPLICATION_JSON)
                  .content("{\"listingId\":-1}"))
          .andExpect(status().isForbidden())
          .andExpect(jsonPath("$.code").value("SUPERUSER_CANNOT_BUY"));
    }
  }
}
