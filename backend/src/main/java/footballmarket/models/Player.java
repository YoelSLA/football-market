package footballmarket.models;

import footballmarket.models.exceptions.InvalidPlayerException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;

@Entity
@Table(name = "players")
@Getter
public class Player {
  @Id private Long id;

  @Column(nullable = false)
  private String name;

  @Column(nullable = false)
  private String team;

  @Column(nullable = false)
  private String league;

  @Column(nullable = false)
  private String position;

  @Column(nullable = false)
  private boolean active;

  protected Player() {}

  public Player(Long id, String name, String team, String league, String position) {
    if (id == null) {
      throw new InvalidPlayerException("El identificador del jugador es obligatorio");
    }
    this.id = id;
    this.update(name, team, league, position);
    this.activate();
  }

  /** Válida todos los datos antes de modificar el estado del jugador. */
  public void update(String name, String team, String league, String position) {
    requireText(name);
    requireText(team);
    requireText(league);
    requireText(position);
    this.name = name;
    this.team = team;
    this.league = league;
    this.position = position;
  }

  public void activate() {
    this.active = true;
  }

  public void deactivate() {
    this.active = false;
  }

  private static void requireText(String value) {
    if (value == null || value.isBlank()) {
      throw new InvalidPlayerException("Los datos del jugador son obligatorios");
    }
  }
}
