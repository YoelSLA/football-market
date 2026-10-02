package footballmarket.models.records;

import footballmarket.models.exceptions.InvalidPlayerException;
import java.time.LocalDate;

/** Datos inmutables de una foto; externalId nunca representa la identidad interna del jugador. */
public record PlayerCandidate(
    String externalId,
    String name,
    String team,
    String league,
    String position,
    LocalDate dateOfBirth,
    String nationality) {
  public PlayerCandidate {
    if (externalId == null
        || externalId.isBlank()
        || externalId.length() > 255
        || name == null
        || name.isBlank()
        || team == null
        || team.isBlank()
        || league == null
        || league.isBlank()
        || position == null
        || position.isBlank()) {
      throw new InvalidPlayerException(
          "Los datos obligatorios del candidato deben estar presentes");
    }
    nationality =
        nationality == null || nationality.isBlank() || nationality.length() > 255
            ? null
            : nationality.strip();
  }
}
