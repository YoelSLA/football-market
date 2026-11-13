package footballmarket.features.valuation.infraestructure;

/** Fragmentos de lectura compartidos por las consultas de ranking de ambas estrategias. */
final class ScoreRankingSql {
  private ScoreRankingSql() {}

  static final String POSITIONAL =
      "with latest as (select distinct on (player_id) player_id, final_score as score, valuation_date from player_positional_valuations order by player_id, valuation_date desc), ";
  static final String ATTRIBUTE =
      "with latest as (select distinct on (player_id) player_id, final_grl as score, valuation_date from player_attribute_valuations order by player_id, valuation_date desc), ";
  static final String RANKED =
      """
      ranked as (
        select row_number() over (order by v.score desc, v.player_id asc) as position,
          v.player_id as "playerId", v.score, v.valuation_date as "valuationDate",
          p.name, p.position as "playerPosition", p.image_url as "imageUrl",
          t.id as "teamId", t.name as "teamName", l.id as "leagueId", l.name as "leagueName"
        from latest v join players p on p.id = v.player_id
        left join teams t on t.id = p.team_id left join leagues l on l.id = t.league_id
        where exists (select 1 from player_quotes q where q.player_id = p.id)
          and exists (select 1 from player_statistics s where s.player_id = p.id
            and s.present_metrics > 0 and s.extraction_date =
              (select max(s2.extraction_date) from player_statistics s2 where s2.player_id = p.id))
      )
      """;
  static final String FILTER =
      """
      where (cast(:name as text) is null or lower(name) like
        '%' || replace(replace(replace(lower(cast(:name as text)), '!', '!!'), '%', '!%'), '_', '!_') || '%' escape '!')
      and (cast(:leagueId as bigint) is null or "leagueId" = :leagueId)
      and (cast(:position as text) is null or lower("playerPosition") = lower(cast(:position as text)))
      """;
  static final String PAGE =
      "select * from ranked " + FILTER + " order by position limit :size offset :offset";
  static final String COUNT = "select count(*) from ranked " + FILTER;
  static final String LEAGUES =
      "select distinct \"leagueId\" as id, \"leagueName\" as name from ranked where \"leagueId\" is not null order by name, id";
  static final String POSITIONS =
      "select distinct \"playerPosition\" from ranked where \"playerPosition\" is not null order by \"playerPosition\"";
}
