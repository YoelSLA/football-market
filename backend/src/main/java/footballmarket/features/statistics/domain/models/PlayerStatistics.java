package footballmarket.features.statistics.domain.models;

import footballmarket.features.catalog.domain.exceptions.InvalidPlayerException;
import footballmarket.features.catalog.domain.models.player.Player;
import footballmarket.features.statistics.domain.enums.MetricSource;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
import lombok.Getter;

/**
 * Estadísticas de un jugador en una campaña y un canal, en una sola fila.
 *
 * <p>El contexto se limita a la liga local del jugador y a su temporada vigente: nunca se suman
 * contextos dedistintos. {@code JSON} y {@code SCRAPING} no se pisan porque la fuente es parte de
 * la clave unica junto con la fecha de campana y el jugador.
 *
 * <p>Una métrica no publicada queda en {@code null}; no se persiste como cero ni altera el
 * histórico.
 */
@Entity
@Table(name = "player_statistics")
@Getter
public class PlayerStatistics {
  /** {@code RESOLVED} cuando el contexto corresponde a la liga del jugador. */
  public static final String RESOLVED = "RESOLVED";

  /** El proveedor respondió, pero ningún contexto publicado corresponde a la liga del jugador. */
  public static final String NO_LEAGUE_CONTEXT = "NO_LEAGUE_CONTEXT";

  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "statistics_row_ids")
  @SequenceGenerator(
      name = "statistics_row_ids",
      sequenceName = "player_statistics_id_seq",
      allocationSize = 1)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "player_id", nullable = false, updatable = false)
  private Player player;

  @Column(name = "extraction_date", nullable = false, updatable = false)
  private LocalDate extractionDate;

  @Enumerated(EnumType.STRING)
  @Column(name = "source", nullable = false, updatable = false)
  private MetricSource source;

  @Column(name = "provider_competition_id")
  private String providerCompetitionId;

  @Column(name = "provider_team_id")
  private String providerTeamId;

  @Column(name = "season_key")
  private String seasonKey;

  @Column(name = "season_label")
  private String seasonLabel;

  @Column(name = "context_status", nullable = false)
  private String contextStatus;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "run_item_id")
  private PlayerStatisticsRunItem runItem;

  @Column(name = "observed_at", nullable = false)
  private Instant observedAt;

  @Column(name = "present_metrics", nullable = false)
  private int presentMetrics;

  @Column(name = "absent_metrics", nullable = false)
  private int absentMetrics;

  @Column(name = "minutes_played_total")
  private BigDecimal minutes_played_total;

  @Column(name = "appearances_total")
  private BigDecimal appearances_total;

  @Column(name = "goals_total")
  private BigDecimal goals_total;

  @Column(name = "assists_total")
  private BigDecimal assists_total;

  @Column(name = "shots_per_match")
  private BigDecimal shots_per_match;

  @Column(name = "pass_completion_percentage")
  private BigDecimal pass_completion_percentage;

  @Column(name = "aerial_duels_won_per_match")
  private BigDecimal aerial_duels_won_per_match;

  @Column(name = "yellow_cards_total")
  private BigDecimal yellow_cards_total;

  @Column(name = "red_cards_total")
  private BigDecimal red_cards_total;

  @Column(name = "tackles_per_match")
  private BigDecimal tackles_per_match;

  @Column(name = "interceptions_per_match")
  private BigDecimal interceptions_per_match;

  @Column(name = "clearances_per_match")
  private BigDecimal clearances_per_match;

  @Column(name = "blocked_shots_per_match")
  private BigDecimal blocked_shots_per_match;

  @Column(name = "dribbled_past_per_match")
  private BigDecimal dribbled_past_per_match;

  @Column(name = "offsides_won_per_match")
  private BigDecimal offsides_won_per_match;

  @Column(name = "own_goals_total")
  private BigDecimal own_goals_total;

  @Column(name = "saves_per_match")
  private BigDecimal saves_per_match;

  @Column(name = "six_yard_box_saves_per_match")
  private BigDecimal six_yard_box_saves_per_match;

  @Column(name = "penalty_area_saves_per_match")
  private BigDecimal penalty_area_saves_per_match;

  @Column(name = "outside_box_saves_per_match")
  private BigDecimal outside_box_saves_per_match;

  @Column(name = "attempted_tackles_per_match")
  private BigDecimal attempted_tackles_per_match;

  @Column(name = "blocked_crosses_per_match")
  private BigDecimal blocked_crosses_per_match;

  @Column(name = "blocked_passes_per_match")
  private BigDecimal blocked_passes_per_match;

  @Column(name = "failed_dribbles_per_match")
  private BigDecimal failed_dribbles_per_match;

  @Column(name = "total_dribbles_per_match")
  private BigDecimal total_dribbles_per_match;

  @Column(name = "aerial_duels_lost_per_match")
  private BigDecimal aerial_duels_lost_per_match;

  @Column(name = "total_aerial_duels_per_match")
  private BigDecimal total_aerial_duels_per_match;

  @Column(name = "inaccurate_long_balls_per_match")
  private BigDecimal inaccurate_long_balls_per_match;

  @Column(name = "accurate_short_passes_per_match")
  private BigDecimal accurate_short_passes_per_match;

  @Column(name = "inaccurate_short_passes_per_match")
  private BigDecimal inaccurate_short_passes_per_match;

  @Column(name = "long_key_passes_per_match")
  private BigDecimal long_key_passes_per_match;

  @Column(name = "short_key_passes_per_match")
  private BigDecimal short_key_passes_per_match;

  @Column(name = "six_yard_box_shots_per_match")
  private BigDecimal six_yard_box_shots_per_match;

  @Column(name = "penalty_area_shots_per_match")
  private BigDecimal penalty_area_shots_per_match;

  @Column(name = "outside_box_shots_per_match")
  private BigDecimal outside_box_shots_per_match;

  @Column(name = "six_yard_box_goals_per_match")
  private BigDecimal six_yard_box_goals_per_match;

  @Column(name = "penalty_area_goals_per_match")
  private BigDecimal penalty_area_goals_per_match;

  @Column(name = "outside_box_goals_per_match")
  private BigDecimal outside_box_goals_per_match;

  @Column(name = "cross_assists_per_match")
  private BigDecimal cross_assists_per_match;

  @Column(name = "corner_assists_per_match")
  private BigDecimal corner_assists_per_match;

  @Column(name = "through_ball_assists_per_match")
  private BigDecimal through_ball_assists_per_match;

  @Column(name = "free_kick_assists_per_match")
  private BigDecimal free_kick_assists_per_match;

  @Column(name = "throw_in_assists_per_match")
  private BigDecimal throw_in_assists_per_match;

  @Column(name = "other_assists_per_match")
  private BigDecimal other_assists_per_match;

  @Column(name = "fouls_committed_per_match")
  private BigDecimal fouls_committed_per_match;

  @Column(name = "goals_per_match")
  private BigDecimal goals_per_match;

  @Column(name = "assists_per_match")
  private BigDecimal assists_per_match;

  @Column(name = "whoscored_rating_average")
  private BigDecimal whoscored_rating_average;

  @Column(name = "shots_on_target_total")
  private BigDecimal shots_on_target_total;

  @Column(name = "second_yellow_total")
  private BigDecimal second_yellow_total;

  @Column(name = "starts_total")
  private BigDecimal starts_total;

  @Column(name = "substitute_appearances_total")
  private BigDecimal substitute_appearances_total;

  @Column(name = "key_passes_per_match")
  private BigDecimal key_passes_per_match;

  @Column(name = "successful_dribbles_per_match")
  private BigDecimal successful_dribbles_per_match;

  @Column(name = "offsides_per_match")
  private BigDecimal offsides_per_match;

  @Column(name = "dispossessed_per_match")
  private BigDecimal dispossessed_per_match;

  @Column(name = "bad_controls_per_match")
  private BigDecimal bad_controls_per_match;

  @Column(name = "passes_per_match")
  private BigDecimal passes_per_match;

  @Column(name = "successful_long_balls_per_match")
  private BigDecimal successful_long_balls_per_match;

  @Column(name = "successful_crosses_per_match")
  private BigDecimal successful_crosses_per_match;

  protected PlayerStatistics() {}

  public PlayerStatistics(
      Player player,
      LocalDate extractionDate,
      MetricSource source,
      String contextStatus,
      PlayerStatisticsRunItem runItem,
      Instant observedAt) {
    if (player == null
        || extractionDate == null
        || source == null
        || contextStatus == null
        || runItem == null
        || observedAt == null) {
      throw new InvalidPlayerException("La fila de estadísticas requiere contexto completo");
    }
    this.player = player;
    this.extractionDate = extractionDate;
    this.source = source;
    this.contextStatus = contextStatus;
    this.runItem = runItem;
    this.observedAt = observedAt;
  }

  /** Fija el contexto del jugador o marca que su liga no publicó datos. */
  public void resolveContext(
      String competitionId, String teamId, String seasonKey, String seasonLabel) {
    this.providerCompetitionId = competitionId;
    this.providerTeamId = teamId;
    this.seasonKey = seasonKey;
    this.seasonLabel = seasonLabel;
  }

  /**
   * Reconfirma una fila de la campaña cuya liga local todavía no había publicado contexto.
   *
   * <p>Solo una fila {@code NO_LEAGUE_CONTEXT} admite reconfirmación: es la única que el recorrido
   * vuelve a seleccionar, porque un jugador confirmado queda excluido de su campaña. Una fila ya
   * resuelta nunca se sobrescribe ni se degrada, de modo que sus métricas no se mezclan con las de
   * otro contexto.
   *
   * <p>Una fila sin contexto resuelto no publica ninguna métrica, así que todas sus columnas siguen
   * nulas y la reconfirmación no necesita borrar valores previos.
   */
  public void reconfirm(String contextStatus, PlayerStatisticsRunItem runItem, Instant observedAt) {
    if (contextStatus == null || runItem == null || observedAt == null) {
      throw new InvalidPlayerException("La reconfirmación requiere contexto completo");
    }
    if (!NO_LEAGUE_CONTEXT.equals(this.contextStatus)) {
      throw new InvalidPlayerException("Solo una fila sin contexto resuelto admite reconfirmación");
    }
    if (!NO_LEAGUE_CONTEXT.equals(contextStatus) && !RESOLVED.equals(contextStatus)) {
      throw new InvalidPlayerException("El estado de contexto reconfirmado no es válido");
    }
    this.contextStatus = contextStatus;
    this.runItem = runItem;
    this.observedAt = observedAt;
  }

  /** Aplica las métricas del catálogo; las claves ausentes quedan en null. */
  public void applyMetrics(Map<String, BigDecimal> values, int present, int absent) {
    if (present < 0 || absent < 0) {
      throw new InvalidPlayerException("El conteo de métricas de la fila es inconsistente");
    }
    values.forEach(
        (key, value) -> {
          switch (key) {
            case "minutes_played_total" -> this.minutes_played_total = value;
            case "appearances_total" -> this.appearances_total = value;
            case "goals_total" -> this.goals_total = value;
            case "assists_total" -> this.assists_total = value;
            case "shots_per_match" -> this.shots_per_match = value;
            case "pass_completion_percentage" -> this.pass_completion_percentage = value;
            case "aerial_duels_won_per_match" -> this.aerial_duels_won_per_match = value;
            case "yellow_cards_total" -> this.yellow_cards_total = value;
            case "red_cards_total" -> this.red_cards_total = value;
            case "tackles_per_match" -> this.tackles_per_match = value;
            case "interceptions_per_match" -> this.interceptions_per_match = value;
            case "clearances_per_match" -> this.clearances_per_match = value;
            case "blocked_shots_per_match" -> this.blocked_shots_per_match = value;
            case "dribbled_past_per_match" -> this.dribbled_past_per_match = value;
            case "offsides_won_per_match" -> this.offsides_won_per_match = value;
            case "own_goals_total" -> this.own_goals_total = value;
            case "saves_per_match" -> this.saves_per_match = value;
            case "six_yard_box_saves_per_match" -> this.six_yard_box_saves_per_match = value;
            case "penalty_area_saves_per_match" -> this.penalty_area_saves_per_match = value;
            case "outside_box_saves_per_match" -> this.outside_box_saves_per_match = value;
            case "attempted_tackles_per_match" -> this.attempted_tackles_per_match = value;
            case "blocked_crosses_per_match" -> this.blocked_crosses_per_match = value;
            case "blocked_passes_per_match" -> this.blocked_passes_per_match = value;
            case "failed_dribbles_per_match" -> this.failed_dribbles_per_match = value;
            case "total_dribbles_per_match" -> this.total_dribbles_per_match = value;
            case "aerial_duels_lost_per_match" -> this.aerial_duels_lost_per_match = value;
            case "total_aerial_duels_per_match" -> this.total_aerial_duels_per_match = value;
            case "inaccurate_long_balls_per_match" -> this.inaccurate_long_balls_per_match = value;
            case "accurate_short_passes_per_match" -> this.accurate_short_passes_per_match = value;
            case "inaccurate_short_passes_per_match" ->
                this.inaccurate_short_passes_per_match = value;
            case "long_key_passes_per_match" -> this.long_key_passes_per_match = value;
            case "short_key_passes_per_match" -> this.short_key_passes_per_match = value;
            case "six_yard_box_shots_per_match" -> this.six_yard_box_shots_per_match = value;
            case "penalty_area_shots_per_match" -> this.penalty_area_shots_per_match = value;
            case "outside_box_shots_per_match" -> this.outside_box_shots_per_match = value;
            case "six_yard_box_goals_per_match" -> this.six_yard_box_goals_per_match = value;
            case "penalty_area_goals_per_match" -> this.penalty_area_goals_per_match = value;
            case "outside_box_goals_per_match" -> this.outside_box_goals_per_match = value;
            case "cross_assists_per_match" -> this.cross_assists_per_match = value;
            case "corner_assists_per_match" -> this.corner_assists_per_match = value;
            case "through_ball_assists_per_match" -> this.through_ball_assists_per_match = value;
            case "free_kick_assists_per_match" -> this.free_kick_assists_per_match = value;
            case "throw_in_assists_per_match" -> this.throw_in_assists_per_match = value;
            case "other_assists_per_match" -> this.other_assists_per_match = value;
            case "fouls_committed_per_match" -> this.fouls_committed_per_match = value;
            case "goals_per_match" -> this.goals_per_match = value;
            case "assists_per_match" -> this.assists_per_match = value;
            case "whoscored_rating_average" -> this.whoscored_rating_average = value;
            case "shots_on_target_total" -> this.shots_on_target_total = value;
            case "second_yellow_total" -> this.second_yellow_total = value;
            case "starts_total" -> this.starts_total = value;
            case "substitute_appearances_total" -> this.substitute_appearances_total = value;
            case "key_passes_per_match" -> this.key_passes_per_match = value;
            case "successful_dribbles_per_match" -> this.successful_dribbles_per_match = value;
            case "offsides_per_match" -> this.offsides_per_match = value;
            case "dispossessed_per_match" -> this.dispossessed_per_match = value;
            case "bad_controls_per_match" -> this.bad_controls_per_match = value;
            case "passes_per_match" -> this.passes_per_match = value;
            case "successful_long_balls_per_match" -> this.successful_long_balls_per_match = value;
            case "successful_crosses_per_match" -> this.successful_crosses_per_match = value;
            default ->
                throw new InvalidPlayerException("La métrica no pertenece al catálogo cerrado");
          }
        });
    this.presentMetrics = present;
    this.absentMetrics = absent;
  }

  /** Valor de una métrica del catálogo, o null si el proveedor no la publicó. */
  public BigDecimal metricValue(String key) {
    if (key == null) {
      throw new InvalidPlayerException("La métrica consultada es obligatoria");
    }
    return switch (key) {
      case "minutes_played_total" -> this.minutes_played_total;
      case "appearances_total" -> this.appearances_total;
      case "goals_total" -> this.goals_total;
      case "assists_total" -> this.assists_total;
      case "shots_per_match" -> this.shots_per_match;
      case "pass_completion_percentage" -> this.pass_completion_percentage;
      case "aerial_duels_won_per_match" -> this.aerial_duels_won_per_match;
      case "yellow_cards_total" -> this.yellow_cards_total;
      case "red_cards_total" -> this.red_cards_total;
      case "tackles_per_match" -> this.tackles_per_match;
      case "interceptions_per_match" -> this.interceptions_per_match;
      case "clearances_per_match" -> this.clearances_per_match;
      case "blocked_shots_per_match" -> this.blocked_shots_per_match;
      case "dribbled_past_per_match" -> this.dribbled_past_per_match;
      case "offsides_won_per_match" -> this.offsides_won_per_match;
      case "own_goals_total" -> this.own_goals_total;
      case "saves_per_match" -> this.saves_per_match;
      case "six_yard_box_saves_per_match" -> this.six_yard_box_saves_per_match;
      case "penalty_area_saves_per_match" -> this.penalty_area_saves_per_match;
      case "outside_box_saves_per_match" -> this.outside_box_saves_per_match;
      case "attempted_tackles_per_match" -> this.attempted_tackles_per_match;
      case "blocked_crosses_per_match" -> this.blocked_crosses_per_match;
      case "blocked_passes_per_match" -> this.blocked_passes_per_match;
      case "failed_dribbles_per_match" -> this.failed_dribbles_per_match;
      case "total_dribbles_per_match" -> this.total_dribbles_per_match;
      case "aerial_duels_lost_per_match" -> this.aerial_duels_lost_per_match;
      case "total_aerial_duels_per_match" -> this.total_aerial_duels_per_match;
      case "inaccurate_long_balls_per_match" -> this.inaccurate_long_balls_per_match;
      case "accurate_short_passes_per_match" -> this.accurate_short_passes_per_match;
      case "inaccurate_short_passes_per_match" -> this.inaccurate_short_passes_per_match;
      case "long_key_passes_per_match" -> this.long_key_passes_per_match;
      case "short_key_passes_per_match" -> this.short_key_passes_per_match;
      case "six_yard_box_shots_per_match" -> this.six_yard_box_shots_per_match;
      case "penalty_area_shots_per_match" -> this.penalty_area_shots_per_match;
      case "outside_box_shots_per_match" -> this.outside_box_shots_per_match;
      case "six_yard_box_goals_per_match" -> this.six_yard_box_goals_per_match;
      case "penalty_area_goals_per_match" -> this.penalty_area_goals_per_match;
      case "outside_box_goals_per_match" -> this.outside_box_goals_per_match;
      case "cross_assists_per_match" -> this.cross_assists_per_match;
      case "corner_assists_per_match" -> this.corner_assists_per_match;
      case "through_ball_assists_per_match" -> this.through_ball_assists_per_match;
      case "free_kick_assists_per_match" -> this.free_kick_assists_per_match;
      case "throw_in_assists_per_match" -> this.throw_in_assists_per_match;
      case "other_assists_per_match" -> this.other_assists_per_match;
      case "fouls_committed_per_match" -> this.fouls_committed_per_match;
      case "goals_per_match" -> this.goals_per_match;
      case "assists_per_match" -> this.assists_per_match;
      case "whoscored_rating_average" -> this.whoscored_rating_average;
      case "shots_on_target_total" -> this.shots_on_target_total;
      case "second_yellow_total" -> this.second_yellow_total;
      case "starts_total" -> this.starts_total;
      case "substitute_appearances_total" -> this.substitute_appearances_total;
      case "key_passes_per_match" -> this.key_passes_per_match;
      case "successful_dribbles_per_match" -> this.successful_dribbles_per_match;
      case "offsides_per_match" -> this.offsides_per_match;
      case "dispossessed_per_match" -> this.dispossessed_per_match;
      case "bad_controls_per_match" -> this.bad_controls_per_match;
      case "passes_per_match" -> this.passes_per_match;
      case "successful_long_balls_per_match" -> this.successful_long_balls_per_match;
      case "successful_crosses_per_match" -> this.successful_crosses_per_match;
      default -> throw new InvalidPlayerException("La métrica no pertenece al catálogo cerrado");
    };
  }
}
