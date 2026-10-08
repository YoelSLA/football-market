package footballmarket.models;

import footballmarket.models.enums.ExternalProvider;
import footballmarket.models.exceptions.InvalidPlayerException;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;

/**
 * Jugador con identidad local; los identificadores de proveedores se conservan como referencias.
 */
@Entity
@Table(name = "players")
@Getter
public class Player {
  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "player_ids")
  @SequenceGenerator(name = "player_ids", sequenceName = "players_id_seq", allocationSize = 1)
  private Long id;

  private LocalDate dateOfBirth;
  private String nationality;

  @Column(length = 2048)
  private String imageUrl;

  @Column(length = 2048)
  private String fallbackImageUrl;

  @OneToMany(mappedBy = "player", cascade = CascadeType.ALL)
  @Getter(AccessLevel.NONE)
  private List<PlayerExternalReference> externalReferences;

  @Column(nullable = false)
  private String name;

  @Column(name = "team", nullable = false)
  private String legacyTeam;

  @Column(name = "league", nullable = false)
  private String legacyLeague;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "team_id")
  private Team team;

  @Column(nullable = false)
  private String position;

  @Column(nullable = false)
  private boolean active;

  protected Player() {
    this.externalReferences = new ArrayList<>();
  }

  /**
   * Crea un jugador con identidad interna pendiente de generación y ya activo.
   *
   * <p>La identidad en Football-Data.org se agrega por separado como referencia externa.
   *
   * @param name nombre del jugador
   * @param team equipo según la fuente
   * @param league liga según la fuente
   * @param position posición según la fuente
   */
  public Player(String name, String team, String league, String position) {
    this.update(name, team, league, position);
    this.activate();
  }

  /** Crea un jugador asociado; los textos legacy son solo un espejo de compatibilidad. */
  public Player(String name, Team team, String position) {
    this.update(name, team, position);
    this.activate();
  }

  /** La liga se deriva exclusivamente del equipo, nunca del espejo legacy. */
  public League getLeague() {
    return this.team == null ? null : this.team.getLeague();
  }

  /** Actualiza datos válidos y asociación actual sin una liga independiente. */
  public void update(String name, Team team, String position) {
    requireText(name);
    requireText(position);
    if (team == null) {
      throw new InvalidPlayerException("El jugador requiere un equipo resuelto");
    }
    if (this.name == null || !PlayerPresentation.equivalent(this.name, name)) {
      this.name = name;
    }
    if (this.position == null || !PlayerPresentation.equivalent(this.position, position)) {
      this.position = position;
    }
    this.associateTeam(team);
  }

  /** Asocia por identidad resuelta y actualiza el espejo temporal, sin modificar active. */
  public void associateTeam(Team team) {
    if (team == null) {
      throw new InvalidPlayerException("La asociación requiere un equipo resuelto");
    }
    this.team = team;
    this.refreshLegacyMirror();
  }

  /** Refleja renombrados de las entidades asociadas para convivencia con el binario previo. */
  public void refreshLegacyMirror() {
    if (this.team != null) {
      this.legacyTeam = this.team.getName();
      this.legacyLeague = this.team.getLeague().getName();
    }
  }

  /**
   * Devuelve las identidades externas del jugador.
   *
   * @return referencias del jugador, no modificables desde fuera
   */
  public List<PlayerExternalReference> getExternalReferences() {
    return Collections.unmodifiableList(this.externalReferences);
  }

  /**
   * Añade una identidad sin reemplazar ni duplicar una referencia existente.
   *
   * @param provider proveedor que asignó el identificador
   * @param externalId identificador del jugador en ese proveedor
   */
  public void addExternalReference(ExternalProvider provider, String externalId) {
    if (this.externalReferences.stream()
        .anyMatch(
            reference ->
                reference.getProvider() == provider
                    && reference.getExternalId().equals(externalId))) {
      return;
    }
    this.externalReferences.add(new PlayerExternalReference(this, provider, externalId));
  }

  /**
   * Aplica únicamente los atributos opcionales informados con valor válido.
   *
   * <p>La ausencia, el vacío o un formato no reconocido no borran ni sobrescriben lo conocido. Esta
   * operación nunca modifica la imagen, que no se resuelve en esta feature.
   *
   * @param dateOfBirth fecha de nacimiento válida o {@code null}
   * @param nationality nacionalidad válida o ausente
   */
  public void updateOptionalDetails(LocalDate dateOfBirth, String nationality) {
    if (dateOfBirth != null) {
      this.dateOfBirth = dateOfBirth;
    }
    if (nationality != null && !nationality.isBlank() && nationality.length() <= 255) {
      this.nationality = nationality.strip();
    }
  }

  /** Prioriza la imagen recortada, promueve la secundaria válida y preserva URLs sin reemplazo. */
  public void applyImages(String cutout, String thumbnail) {
    if (cutout != null) {
      if (thumbnail != null) {
        this.fallbackImageUrl = thumbnail.equals(cutout) ? null : thumbnail;
      } else if (this.fallbackImageUrl == null
          && !cutout.equals(this.imageUrl)
          && this.imageUrl != null) {
        this.fallbackImageUrl = this.imageUrl;
      }
      this.imageUrl = cutout;
    } else if (thumbnail != null) {
      if (this.imageUrl == null) {
        this.imageUrl = thumbnail;
      } else if (!thumbnail.equals(this.imageUrl)) {
        this.fallbackImageUrl = thumbnail;
      }
    }
  }

  /** Válida todos los datos antes de modificar el estado del jugador. */
  public void update(String name, String team, String league, String position) {
    requireText(name);
    requireText(team);
    requireText(league);
    requireText(position);
    this.name = name;
    if (this.team == null) {
      this.legacyTeam = team;
      this.legacyLeague = league;
    } else {
      this.refreshLegacyMirror();
    }
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
