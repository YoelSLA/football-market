package footballmarket.features.market.presentation.dtos.request;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.exc.InvalidFormatException;

@Tag("lightweight")
class OrderBuyRequestDeserializerTest {

  private final ObjectMapper mapper = new ObjectMapper();

  @Test
  @DisplayName("Deserializa orden de compra del Mercado Oficial (playerId y quantity)")
  void deserializeDirectPurchase() throws Exception {
    String json = "{\"playerId\": 123, \"quantity\": 5}";

    OrderBuyRequestDTO request = mapper.readValue(json, OrderBuyRequestDTO.class);

    assertThat(request).isNotNull();
    assertThat(request.playerId()).isEqualTo(123L);
    assertThat(request.quantity()).isEqualTo(5);
    assertThat(request.listingId()).isNull();
  }

  @Test
  @DisplayName("Deserializa orden de compra del Mercado P2P (listingId)")
  void deserializeP2PPurchase() throws Exception {
    String json = "{\"listingId\": 456}";

    OrderBuyRequestDTO request = mapper.readValue(json, OrderBuyRequestDTO.class);

    assertThat(request).isNotNull();
    assertThat(request.listingId()).isEqualTo(456L);
    assertThat(request.playerId()).isNull();
    assertThat(request.quantity()).isNull();
  }

  @Test
  @DisplayName("Rechaza listingId que no es número entero")
  void rejectNonIntegralListingId() {
    String json = "{\"listingId\": \"abc\"}";

    assertThatThrownBy(() -> mapper.readValue(json, OrderBuyRequestDTO.class))
        .isInstanceOf(InvalidFormatException.class);
  }

  @Test
  @DisplayName("Rechaza JSON que no sea un objeto")
  void rejectNonObject() {
    String json = "\"string\"";

    assertThatThrownBy(() -> mapper.readValue(json, OrderBuyRequestDTO.class))
        .isInstanceOf(InvalidFormatException.class);
  }

  @Test
  @DisplayName("listingId válido ignora campos adicionales aunque tengan tipos incompatibles")
  void deserializeP2PIgnoresExtraneousFields() throws Exception {
    String json =
        "{\"listingId\": 25, \"playerId\": \"abc\", \"quantity\": \"invalid\", \"price\": 1.5}";

    OrderBuyRequestDTO request = mapper.readValue(json, OrderBuyRequestDTO.class);

    assertThat(request).isNotNull();
    assertThat(request.listingId()).isEqualTo(25L);
    assertThat(request.playerId()).isNull();
    assertThat(request.quantity()).isNull();
  }

  @ParameterizedTest
  @ValueSource(strings = {"25.0", "1e1", "\"25\"", "true", "[]", "{}", "9223372036854775808"})
  @DisplayName("Rechaza listingId con formatos no integrales o representaciones inválidas")
  void rejectNonIntegralOrInvalidListingIdFormats(String invalidValue) {
    String json = "{\"listingId\": " + invalidValue + "}";

    assertThatThrownBy(() -> mapper.readValue(json, OrderBuyRequestDTO.class))
        .isInstanceOf(InvalidFormatException.class);
  }
}
