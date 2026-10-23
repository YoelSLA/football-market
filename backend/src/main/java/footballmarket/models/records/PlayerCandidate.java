package footballmarket.models.records;

import footballmarket.models.PlayerPresentation;
import footballmarket.models.exceptions.InvalidPlayerException;
import java.time.LocalDate;
import java.util.List;

/** Datos inmutables de una foto; externalId nunca representa la identidad interna del jugador. */
public record PlayerCandidate(
    String externalId,
    String name,
    String team,
    String league,
    String position,
    LocalDate dateOfBirth,
    String nationality,
    String teamExternalId) {
  /** Conserva el contrato interno anterior durante la adaptación de productores de fotos. */
  public PlayerCandidate(
      String externalId,
      String name,
      String team,
      String league,
      String position,
      LocalDate dateOfBirth,
      String nationality) {
    this(externalId, name, team, league, position, dateOfBirth, nationality, null);
  }

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

  /** Conserva prioridad semántica de la referencia y reúne presentaciones equivalentes. */
  public PlayerCandidate consolidatePresentation(PlayerCandidate observation) {
    if (!this.externalId.equals(observation.externalId())
        || !java.util.Objects.equals(this.teamExternalId, observation.teamExternalId())
        || !PlayerPresentation.equivalent(this.name, observation.name())
        || !PlayerPresentation.equivalent(this.position, observation.position())) {
      return this;
    }
    return new PlayerCandidate(
        this.externalId,
        PlayerPresentation.canonical(List.of(this.name, observation.name())),
        this.team,
        this.league,
        PlayerPresentation.canonical(List.of(this.position, observation.position())),
        this.dateOfBirth,
        this.nationality,
        this.teamExternalId);
  }
}
