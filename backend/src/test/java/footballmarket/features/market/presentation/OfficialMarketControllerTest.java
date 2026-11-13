package footballmarket.features.market.presentation;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import footballmarket.features.market.application.OfficialMarketCatalogService;
import footballmarket.features.market.domain.records.OfficialMarketPlayer;
import footballmarket.features.market.domain.records.OfficialMarketQuote;
import footballmarket.security.PersistedIdentityValidator;
import footballmarket.security.SecurityConfig;
import io.jsonwebtoken.Jwts;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Date;
import java.util.List;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
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

@WebMvcTest(OfficialMarketController.class)
@Import({SecurityConfig.class, OfficialMarketValidationExceptionHandler.class})
@ActiveProfiles("test")
@Tag("lightweight")
class OfficialMarketControllerTest {

  @Autowired private WebApplicationContext context;
  @Autowired private FilterChainProxy security;
  @Autowired private SecretKey key;
  @MockitoBean private OfficialMarketCatalogService catalogService;
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

  @Nested
  @DisplayName("Consultas exitosas del catálogo oficial")
  class SuccessfulQueries {

    @Test
    @DisplayName("Devuelve página 200 OK con metadatos y cotización consolidada")
    void returnsPagedOfficialCatalogWithQuotes() throws Exception {
      // Arrange
      OfficialMarketQuote quote =
          new OfficialMarketQuote(
              25000L,
              24000L,
              4.17,
              8.92,
              1.25,
              "MANUAL_RECALCULATION",
              LocalDate.of(2026, 11, 12),
              Instant.parse("2026-11-12T15:30:00Z"));
      OfficialMarketPlayer player =
          new OfficialMarketPlayer(
              7821L,
              "Lionel Messi",
              "DEL",
              "Argentina",
              45L,
              "Inter Miami CF",
              "https://images.footballmarket.com/teams/45.png",
              12L,
              "Major League Soccer",
              "https://images.footballmarket.com/players/7821.png",
              quote);

      when(catalogService.getOfficialCatalog(any()))
          .thenReturn(new PageImpl<>(List.of(player), PageRequest.of(0, 20), 1));

      // Act & Assert
      mvc.perform(
              get("/api/market/official")
                  .header("Authorization", authorization)
                  .param("page", "0")
                  .param("size", "20"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.page").value(0))
          .andExpect(jsonPath("$.size").value(20))
          .andExpect(jsonPath("$.totalElements").value(1))
          .andExpect(jsonPath("$.totalPages").value(1))
          .andExpect(jsonPath("$.content", hasSize(1)))
          .andExpect(jsonPath("$.content[0].id").value(7821))
          .andExpect(jsonPath("$.content[0].name").value("Lionel Messi"))
          .andExpect(jsonPath("$.content[0].quote.price").value(25000))
          .andExpect(jsonPath("$.content[0].quote.previousPrice").value(24000))
          .andExpect(jsonPath("$.content[0].quote.variationPercentage").value(4.17))
          .andExpect(jsonPath("$.content[0].quote.performanceScore").value(8.92))
          .andExpect(jsonPath("$.content[0].quote.multiplier").value(1.25))
          .andExpect(jsonPath("$.content[0].quote.calculationOrigin").value("MANUAL_RECALCULATION"))
          .andExpect(jsonPath("$.content[0].stock").doesNotExist())
          .andExpect(jsonPath("$.content[0].availableTokens").doesNotExist())
          .andExpect(jsonPath("$.content[0].balance").doesNotExist());
    }

    @Test
    @DisplayName("Devuelve quote null para jugadores sin cotización vigente persistida")
    void returnsNullQuoteWhenPlayerHasNoQuotes() throws Exception {
      // Arrange
      OfficialMarketPlayer player =
          new OfficialMarketPlayer(
              9102L,
              "Julián Álvarez",
              "DEL",
              "Argentina",
              23L,
              "Atlético de Madrid",
              "https://images.footballmarket.com/teams/23.png",
              8L,
              "La Liga",
              "https://images.footballmarket.com/players/9102.png",
              null);

      when(catalogService.getOfficialCatalog(any()))
          .thenReturn(new PageImpl<>(List.of(player), PageRequest.of(0, 20), 1));

      // Act & Assert
      mvc.perform(get("/api/market/official").header("Authorization", authorization))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.content[0].id").value(9102))
          .andExpect(jsonPath("$.content[0].quote", nullValue()));
    }

    @Test
    @DisplayName("Devuelve variationPercentage null cuando previousPrice es nulo o no positivo")
    void returnsNullVariationPercentageWhenPreviousPriceNonPositive() throws Exception {
      // Arrange
      OfficialMarketQuote quote =
          new OfficialMarketQuote(
              10000L,
              null,
              null,
              7.50,
              1.00,
              "INITIAL",
              LocalDate.of(2026, 11, 12),
              Instant.parse("2026-11-12T15:30:00Z"));
      OfficialMarketPlayer player =
          new OfficialMarketPlayer(
              1L, "Jugador Inicial", "MED", "España", 10L, "Club", "url", 2L, "Liga", "url", quote);

      when(catalogService.getOfficialCatalog(any()))
          .thenReturn(new PageImpl<>(List.of(player), PageRequest.of(0, 20), 1));

      // Act & Assert
      mvc.perform(get("/api/market/official").header("Authorization", authorization))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.content[0].quote.price").value(10000))
          .andExpect(jsonPath("$.content[0].quote.previousPrice", nullValue()))
          .andExpect(jsonPath("$.content[0].quote.variationPercentage", nullValue()));
    }

    @Test
    @DisplayName("Devuelve variationPercentage 0.0 cuando previousPrice es idéntico a price")
    void returnsZeroVariationPercentageWhenPricesIdentical() throws Exception {
      // Arrange
      OfficialMarketQuote quote =
          new OfficialMarketQuote(
              15000L,
              15000L,
              0.0,
              6.50,
              1.10,
              "CARRIED_FORWARD",
              LocalDate.of(2026, 11, 12),
              Instant.parse("2026-11-12T15:30:00Z"));
      OfficialMarketPlayer player =
          new OfficialMarketPlayer(
              2L, "Jugador Estable", "DEF", "Brasil", 10L, "Club", "url", 2L, "Liga", "url", quote);

      when(catalogService.getOfficialCatalog(any()))
          .thenReturn(new PageImpl<>(List.of(player), PageRequest.of(0, 20), 1));

      // Act & Assert
      mvc.perform(get("/api/market/official").header("Authorization", authorization))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.content[0].quote.price").value(15000))
          .andExpect(jsonPath("$.content[0].quote.previousPrice").value(15000))
          .andExpect(jsonPath("$.content[0].quote.variationPercentage").value(0.0));
    }
  }

  @Nested
  @DisplayName("Validaciones de errores controlados 400 Bad Request")
  class ValidationErrors {

    @Test
    @DisplayName("Rechaza rango de precio invertido con 400 INVALID_PRICE_RANGE")
    void rejectsInvertedPriceRange() throws Exception {
      mvc.perform(
              get("/api/market/official")
                  .header("Authorization", authorization)
                  .param("minPrice", "5000")
                  .param("maxPrice", "1000"))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.code").value("INVALID_PRICE_RANGE"));
    }

    @Test
    @DisplayName("Rechaza precio mínimo negativo con 400 INVALID_PRICE_RANGE")
    void rejectsNegativeMinPrice() throws Exception {
      mvc.perform(
              get("/api/market/official")
                  .header("Authorization", authorization)
                  .param("minPrice", "-10"))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.code").value("INVALID_PRICE_RANGE"));
    }

    @Test
    @DisplayName("Rechaza precio máximo negativo con 400 INVALID_PRICE_RANGE")
    void rejectsNegativeMaxPrice() throws Exception {
      mvc.perform(
              get("/api/market/official")
                  .header("Authorization", authorization)
                  .param("maxPrice", "-50"))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.code").value("INVALID_PRICE_RANGE"));
    }

    @Test
    @DisplayName("Rechaza número de página negativo con 400 INVALID_PAGINATION")
    void rejectsNegativePageNumber() throws Exception {
      mvc.perform(
              get("/api/market/official")
                  .header("Authorization", authorization)
                  .param("page", "-1"))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.code").value("INVALID_PAGINATION"));
    }

    @Test
    @DisplayName("Rechaza tamaño de página menor a 1 con 400 INVALID_PAGE_SIZE")
    void rejectsZeroPageSize() throws Exception {
      mvc.perform(
              get("/api/market/official").header("Authorization", authorization).param("size", "0"))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.code").value("INVALID_PAGE_SIZE"));
    }

    @Test
    @DisplayName("Rechaza tamaño de página superior a 100 con 400 INVALID_PAGE_SIZE")
    void rejectsExcessivePageSize() throws Exception {
      mvc.perform(
              get("/api/market/official")
                  .header("Authorization", authorization)
                  .param("size", "101"))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.code").value("INVALID_PAGE_SIZE"));
    }

    @Test
    @DisplayName("Rechaza criterio de ordenamiento desconocido con 400 INVALID_SORT_PARAMETER")
    void rejectsUnknownSortProperty() throws Exception {
      mvc.perform(
              get("/api/market/official")
                  .header("Authorization", authorization)
                  .param("sort", "salary,desc"))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.code").value("INVALID_SORT_PARAMETER"));
    }

    @Test
    @DisplayName("Rechaza dirección de ordenamiento inválida con 400 INVALID_SORT_PARAMETER")
    void rejectsInvalidSortDirection() throws Exception {
      mvc.perform(
              get("/api/market/official")
                  .header("Authorization", authorization)
                  .param("sort", "price,sideways"))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.code").value("INVALID_SORT_PARAMETER"));
    }

    @Test
    @DisplayName("Rechaza múltiples parámetros sort con 400 INVALID_SORT_PARAMETER")
    void rejectsMultipleSortParameters() throws Exception {
      mvc.perform(
              get("/api/market/official")
                  .header("Authorization", authorization)
                  .param("sort", "price,desc")
                  .param("sort", "name,asc"))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.code").value("INVALID_SORT_PARAMETER"));
    }

    @Test
    @DisplayName("Rechaza formato de tipo inválido con 400 INVALID_PARAMETER_TYPE")
    void rejectsInvalidParameterType() throws Exception {
      mvc.perform(
              get("/api/market/official")
                  .header("Authorization", authorization)
                  .param("minPrice", "no-es-un-numero"))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.code").value("INVALID_PARAMETER_TYPE"));
    }
  }

  @Nested
  @DisplayName("Autenticación y seguridad 401 Unauthorized")
  class SecurityTests {

    @Test
    @DisplayName("Rechaza con 401 Unauthorized cuando no se envía credencial JWT")
    void rejectsUnauthenticatedRequest() throws Exception {
      mvc.perform(get("/api/market/official")).andExpect(status().isUnauthorized());
    }
  }
}
