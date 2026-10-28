package footballmarket.support;

import footballmarket.features.catalog.domain.models.league.League;
import footballmarket.features.catalog.domain.models.player.Player;
import footballmarket.features.catalog.domain.models.team.Team;
import footballmarket.features.catalog.infraestructure.LeagueRepository;
import footballmarket.features.catalog.infraestructure.PlayerRepository;
import footballmarket.features.catalog.infraestructure.TeamRepository;
import footballmarket.features.statistics.domain.enums.ExtractionRunStatus;
import footballmarket.features.statistics.domain.enums.MetricSource;
import footballmarket.features.statistics.domain.enums.PlayerExtractionResult;
import footballmarket.features.statistics.domain.models.PlayerStatistics;
import footballmarket.features.statistics.domain.models.PlayerStatisticsExtractionRun;
import footballmarket.features.statistics.domain.models.PlayerStatisticsRunItem;
import footballmarket.models.*;
import footballmarket.models.enums.*;
import footballmarket.repositories.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Fixtures compartidas: prepara filas, nunca ejecuta la consulta bajo prueba. */
@Component
public class PlayerStatisticsQueryData {
  private final LeagueRepository leagues;
  private final TeamRepository teams;
  private final PlayerRepository players;
  private final PlayerStatisticsExtractionRunRepository runs;
  private final PlayerStatisticsRunItemRepository items;
  private final PlayerStatisticsRepository statistics;

  @Autowired
  public PlayerStatisticsQueryData(
      LeagueRepository leagues,
      TeamRepository teams,
      PlayerRepository players,
      PlayerStatisticsExtractionRunRepository runs,
      PlayerStatisticsRunItemRepository items,
      PlayerStatisticsRepository statistics) {
    this.leagues = leagues;
    this.teams = teams;
    this.players = players;
    this.runs = runs;
    this.items = items;
    this.statistics = statistics;
  }

  @Transactional
  public Long player(String name) {
    League league = this.leagues.save(new League(name + " League"));
    Team team = this.teams.save(new Team(name + " Team", league, true));
    return this.players.saveAndFlush(new Player(name, team, "Forward")).getId();
  }

  @Transactional
  public void deactivate(Long playerId) {
    this.players.findById(playerId).orElseThrow().deactivate();
  }

  @Transactional
  public Long row(
      Long playerId,
      LocalDate date,
      MetricSource source,
      String season,
      String competition,
      PlayerExtractionResult result,
      Map<String, BigDecimal> values) {
    Player player = this.players.findById(playerId).orElseThrow();
    Instant time = date.atStartOfDay(ZoneOffset.UTC).toInstant();
    PlayerStatisticsExtractionRun run = new PlayerStatisticsExtractionRun(date, source, time, null);
    run.finish(
        ExtractionRunStatus.PARTIAL,
        time.plusSeconds(2),
        1,
        0,
        1,
        result == PlayerExtractionResult.COMPLETED ? 1 : 0,
        0,
        result == PlayerExtractionResult.COMPLETED ? 0 : 1,
        0,
        values.size(),
        60 - values.size(),
        0,
        0,
        null);
    this.runs.save(run);
    PlayerStatisticsRunItem item = new PlayerStatisticsRunItem(run, player, "123", time);
    item.finish(result, time.plusSeconds(1), 1, values.size(), 60 - values.size(), null);
    this.items.save(item);
    PlayerStatistics row =
        new PlayerStatistics(
            player, date, source, PlayerStatistics.RESOLVED, item, time.plusSeconds(1));
    row.resolveContext(competition, "3", season, season);
    row.applyMetrics(values, values.size(), 60 - values.size());
    return this.statistics.saveAndFlush(row).getId();
  }
}
