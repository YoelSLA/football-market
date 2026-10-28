package footballmarket.features.statistics.presentation.dtos.request;

import footballmarket.features.statistics.presentation.exceptions.InvalidStatisticsRequestException;
import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.ValueDeserializer;

/** Rechaza coerciones de fracciones y texto a enteros del contrato de scraping. */
public class StatisticsMaxPlayersDeserializer extends ValueDeserializer<Integer> {
  @Override
  public Integer deserialize(JsonParser parser, DeserializationContext context) {
    if (!parser.hasToken(JsonToken.VALUE_NUMBER_INT)) {
      throw new InvalidStatisticsRequestException(
          "INVALID_STATISTICS_MAX_PLAYERS",
          "El máximo de jugadores debe ser un entero positivo de 32 bits");
    }
    java.math.BigInteger value = parser.getBigIntegerValue();
    if (value.signum() <= 0
        || value.compareTo(java.math.BigInteger.valueOf(Integer.MAX_VALUE)) > 0) {
      throw new InvalidStatisticsRequestException(
          "INVALID_STATISTICS_MAX_PLAYERS",
          "El máximo de jugadores debe ser un entero positivo de 32 bits");
    }
    return value.intValueExact();
  }
}
