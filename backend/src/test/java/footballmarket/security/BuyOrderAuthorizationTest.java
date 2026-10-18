package footballmarket.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import footballmarket.models.Player;
import footballmarket.models.User;
import footballmarket.support.MarketTestData;
import footballmarket.support.TestcontainersConfiguration;
import io.jsonwebtoken.Jwts;
import java.time.Instant;
import java.util.Date;
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

/**
 * Origen de la autorización de compra.
 *
 * <p>Categoría Security con componentes reales: el token que emite el proyecto solo lleva {@code
 * subject}, {@code issuedAt} y {@code expiration}, de modo que no existe claim de rol. La decisión
 * de compra depende por tanto del rol persistido, que es lo que estas pruebas comprueban.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
class BuyOrderAuthorizationTest {
  private static final int QUANTITY = 10;

  @Autowired private MockMvc mvc;
  @Autowired private SecretKey key;
  @Autowired private MarketTestData market;

  private User buyer;
  private User superuser;
  private Player player;

  @BeforeEach
  void arrange() {
    buyer = market.registerBuyer();
    superuser = market.registerSuperuser();
    player = market.createVisiblePlayer();
    market.emitInitialTokens(player, superuser);
  }

  private String tokenFor(User user) {
    return "Bearer "
        + Jwts.builder()
            .subject(user.getEmail())
            .expiration(Date.from(Instant.now().plusSeconds(300)))
            .signWith(key)
            .compact();
  }

  private String body() {
    return "{\"playerId\":" + player.getId() + ",\"quantity\":" + QUANTITY + "}";
  }

  @Nested
  @DisplayName("Rol persistido")
  class PersistedRole {
    @Test
    @DisplayName("Un usuario persistido como superusuario recibe 403 aunque su sesión sea válida")
    void elSuperusuarioPersistenteRecibeProhibido() throws Exception {
      // Act / Assert
      mvc.perform(
              post("/api/orders/buy")
                  .header("Authorization", tokenFor(superuser))
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(body()))
          .andExpect(status().isForbidden())
          .andExpect(jsonPath("$.code").value("SUPERUSER_CANNOT_BUY"));
    }

    @Test
    @DisplayName("Un usuario persistido como usuario normal completa la compra")
    void elUsuarioNormalCompletaLaCompra() throws Exception {
      // Act / Assert
      mvc.perform(
              post("/api/orders/buy")
                  .header("Authorization", tokenFor(buyer))
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(body()))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.transactionId").exists());
    }

    @Test
    @DisplayName("La sesión se valida por identidad persistida y no por el contenido del token")
    void laSesionSeValidaPorIdentidadPersistida() throws Exception {
      // Arrange: token firmado correctamente con una identidad que no está persistida.
      String orphan =
          "Bearer "
              + Jwts.builder()
                  .subject("nobody@example.com")
                  .expiration(Date.from(Instant.now().plusSeconds(300)))
                  .signWith(key)
                  .compact();

      // Act / Assert
      mvc.perform(
              post("/api/orders/buy")
                  .header("Authorization", orphan)
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(body()))
          .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Una ausencia de sesión se rechaza antes de alcanzar el caso de uso")
    void laAusenciaDeSesionSeRechaza() throws Exception {
      // Act / Assert
      mvc.perform(post("/api/orders/buy").contentType(MediaType.APPLICATION_JSON).content(body()))
          .andExpect(status().isUnauthorized());
    }
  }
}
