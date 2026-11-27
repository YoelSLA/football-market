package footballmarket.features.market.presentation;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import footballmarket.features.market.application.P2PMarketCatalogService;
import footballmarket.features.market.domain.records.P2PMarketCatalog;
import footballmarket.features.market.domain.records.P2PMarketPlayer;
import footballmarket.features.market.domain.records.P2PMarketPriceRange;
import footballmarket.security.PersistedIdentityValidator;
import footballmarket.security.SecurityConfig;
import io.jsonwebtoken.Jwts;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@WebMvcTest(P2PMarketController.class)
@Import({SecurityConfig.class, OfficialMarketValidationExceptionHandler.class})
@ActiveProfiles("test")
@Tag("lightweight")
class P2PMarketControllerTest {

  @Autowired private WebApplicationContext context;
  @Autowired private FilterChainProxy security;
  @Autowired private SecretKey key;
  @MockitoBean private P2PMarketCatalogService p2pCatalogService;
  @MockitoBean private PersistedIdentityValidator identityValidator;

  private MockMvc mvc;
  private String authorization;

  @BeforeEach
  void setUp() {
    when(identityValidator.exists("user@example.com")).thenReturn(true);
    mvc = MockMvcBuilders.webAppContextSetup(context).addFilters(security).build();
    authorization =
        "Bearer "
            + Jwts.builder()
                .subject("user@example.com")
                .expiration(Date.from(Instant.now().plusSeconds(300)))
                .signWith(key)
                .compact();
  }

  @Test
  @DisplayName("GET /api/market/p2p responde 200 con catálogo paginado y estructura correcta")
  void getP2PCatalogOk() throws Exception {
    P2PMarketPlayer player =
        new P2PMarketPlayer(
            1L,
            "Julian Alvarez",
            "DEL",
            "Argentina",
            10L,
            "Atletico Madrid",
            null,
            1L,
            "LaLiga",
            null,
            3L,
            25L,
            1800L);
    P2PMarketCatalog catalog =
        new P2PMarketCatalog(
            new PageImpl<>(List.of(player), PageRequest.of(0, 8), 1),
            new P2PMarketPriceRange(1500L, 5000L));

    when(p2pCatalogService.getP2PCatalog(eq("user@example.com"), any())).thenReturn(catalog);

    mvc.perform(
            get("/api/market/p2p")
                .header("Authorization", authorization)
                .param("page", "0")
                .param("size", "8")
                .param("sort", "price,asc"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content", hasSize(1)))
        .andExpect(jsonPath("$.content[0].name").value("Julian Alvarez"))
        .andExpect(jsonPath("$.content[0].listingCount").value(3))
        .andExpect(jsonPath("$.content[0].availableQuantity").value(25))
        .andExpect(jsonPath("$.content[0].minimumUnitPrice").value(1800))
        .andExpect(jsonPath("$.totalElements").value(1))
        .andExpect(jsonPath("$.priceRange.min").value(1500))
        .andExpect(jsonPath("$.priceRange.max").value(5000));
  }

  @Test
  @DisplayName("GET /api/market/p2p responde 400 cuando el ordenamiento es inválido")
  void getP2PCatalogInvalidSort() throws Exception {
    mvc.perform(
            get("/api/market/p2p")
                .header("Authorization", authorization)
                .param("sort", "unknownField,asc"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("INVALID_SORT_PARAMETER"));
  }

  @Test
  @DisplayName("GET /api/market/p2p responde 400 cuando la paginación es negativa")
  void getP2PCatalogInvalidPagination() throws Exception {
    mvc.perform(get("/api/market/p2p").header("Authorization", authorization).param("page", "-1"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("INVALID_PAGINATION"));
  }

  @Test
  @DisplayName("GET /api/market/p2p responde 400 cuando el tamaño de página está fuera de rango")
  void getP2PCatalogInvalidPageSize() throws Exception {
    mvc.perform(get("/api/market/p2p").header("Authorization", authorization).param("size", "150"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("INVALID_PAGE_SIZE"));
  }

  @Test
  @DisplayName("GET /api/market/p2p responde 400 cuando el rango de precios es inválido")
  void getP2PCatalogInvalidPriceRange() throws Exception {
    mvc.perform(
            get("/api/market/p2p")
                .header("Authorization", authorization)
                .param("minPrice", "5000")
                .param("maxPrice", "1000"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("INVALID_PRICE_RANGE"));
  }
}
