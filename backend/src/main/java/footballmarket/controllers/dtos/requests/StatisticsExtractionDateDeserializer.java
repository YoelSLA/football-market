package footballmarket.controllers.dtos.requests;

import footballmarket.controllers.exceptions.InvalidStatisticsRequestException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.ValueDeserializer;

/** Exige una cadena ISO de fecha; impide coerciones desde números, arrays u otros tipos JSON. */
public class StatisticsExtractionDateDeserializer extends ValueDeserializer<LocalDate> {
  @Override
  public LocalDate deserialize(JsonParser parser, DeserializationContext context) {
    if (!parser.hasToken(JsonToken.VALUE_STRING)) {
      throw invalidDate();
    }
    String published = parser.getString();
    if (!published.matches("[0-9]{4}-[0-9]{2}-[0-9]{2}")) {
      throw invalidDate();
    }
    try {
      return LocalDate.parse(published, DateTimeFormatter.ISO_LOCAL_DATE);
    } catch (DateTimeParseException invalid) {
      throw invalidDate();
    }
  }

  private static InvalidStatisticsRequestException invalidDate() {
    return new InvalidStatisticsRequestException(
        "INVALID_STATISTICS_EXTRACTION_DATE",
        "La fecha de extracción debe ser una cadena ISO válida (yyyy-MM-dd)");
  }
}
