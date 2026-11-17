package footballmarket.features.ranking.application.impl;

import footballmarket.features.quote.domain.models.QuoteExecution;
import footballmarket.features.quote.domain.records.QuoteRankingPage.LeagueOption;
import footballmarket.features.quote.infraestructure.QuoteExecutionRepository;
import footballmarket.features.ranking.application.ScoreRankingQueryService;
import footballmarket.features.ranking.domain.records.ScoreRanking;
import footballmarket.features.ranking.domain.records.ScoreRankingEntry;
import footballmarket.features.ranking.domain.records.ScoreRankingFilterOptions;
import footballmarket.features.ranking.domain.records.ScoreRankingPage;
import footballmarket.features.ranking.domain.records.ScoreRankingQuery;
import footballmarket.features.ranking.domain.records.ScoreRankingRow;
import footballmarket.features.valuation.domain.enums.ValuationStrategy;
import footballmarket.features.valuation.domain.models.AttributeValuation;
import footballmarket.features.valuation.domain.models.PositionalValuation;
import footballmarket.features.valuation.infraestructure.AttributeValuationRepository;
import footballmarket.features.valuation.infraestructure.PositionalValuationRepository;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

/** Consulta consistente del ranking por score sobre la información ya persistida. */
@Service
@Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
public class ScoreRankingQueryServiceImpl implements ScoreRankingQueryService {
  private static final Logger LOG = LoggerFactory.getLogger(ScoreRankingQueryServiceImpl.class);

  private final QuoteExecutionRepository executions;
  private final PositionalValuationRepository positionalValuations;
  private final AttributeValuationRepository attributeValuations;

  public ScoreRankingQueryServiceImpl(
      QuoteExecutionRepository executions,
      PositionalValuationRepository positionalValuations,
      AttributeValuationRepository attributeValuations) {
    this.executions = executions;
    this.positionalValuations = positionalValuations;
    this.attributeValuations = attributeValuations;
  }

  @Override
  public ScoreRanking findScoreRanking() {
    QuoteExecution execution =
        this.executions.findTopByFinishedAtIsNotNullOrderByStartedAtDescIdDesc().orElse(null);
    if (execution == null) {
      LOG.debug("score_ranking_read execution=none entries=0");
      return new ScoreRanking(null, null, List.of());
    }

    List<ScoreRankingEntry> entries = this.findEntries(execution.getStrategyName());
    LOG.debug(
        "score_ranking_read strategy={} quoteDate={} entries={}",
        execution.getStrategyName(),
        execution.getQuoteDate(),
        entries.size());
    return new ScoreRanking(execution.getStrategyName(), execution.getQuoteDate(), entries);
  }

  private List<ScoreRankingEntry> findEntries(ValuationStrategy strategy) {
    List<ScoreRankingEntry> entries = new ArrayList<>();
    if (strategy == ValuationStrategy.POSITIONAL_PERFORMANCE) {
      List<PositionalValuation> valuations = this.positionalValuations.findScoreRanking();
      for (int index = 0; index < valuations.size(); index++) {
        PositionalValuation valuation = valuations.get(index);
        entries.add(
            new ScoreRankingEntry(
                index + 1,
                valuation.getPlayer().getId(),
                valuation.getFinalScore(),
                valuation.getValuationDate()));
      }
    } else {
      List<AttributeValuation> valuations = this.attributeValuations.findScoreRanking();
      for (int index = 0; index < valuations.size(); index++) {
        AttributeValuation valuation = valuations.get(index);
        entries.add(
            new ScoreRankingEntry(
                index + 1,
                valuation.getPlayer().getId(),
                valuation.getFinalGrl(),
                valuation.getValuationDate()));
      }
    }
    return List.copyOf(entries);
  }

  @Override
  public ScoreRankingPage findScoreRanking(ScoreRankingQuery query) {
    QuoteExecution execution =
        this.executions.findTopByFinishedAtIsNotNullOrderByStartedAtDescIdDesc().orElse(null);
    if (execution == null) {
      return new ScoreRankingPage(
          null,
          null,
          List.of(),
          query.page(),
          query.size(),
          0,
          new ScoreRankingFilterOptions(List.of(), List.of()));
    }
    boolean positional = execution.getStrategyName() == ValuationStrategy.POSITIONAL_PERFORMANCE;
    List<ScoreRankingRow> rows =
        positional
            ? this.positionalValuations.findRankingPage(
                query.name(), query.leagueId(), query.position(), query.size(), query.offset())
            : this.attributeValuations.findRankingPage(
                query.name(), query.leagueId(), query.position(), query.size(), query.offset());
    long total =
        positional
            ? this.positionalValuations.countRanking(
                query.name(), query.leagueId(), query.position())
            : this.attributeValuations.countRanking(
                query.name(), query.leagueId(), query.position());
    List<LeagueOption> leagues =
        positional
            ? this.positionalValuations.findRankingLeagues()
            : this.attributeValuations.findRankingLeagues();
    List<String> positions =
        positional
            ? this.positionalValuations.findRankingPositions()
            : this.attributeValuations.findRankingPositions();
    List<ScoreRankingPage.Entry> entries =
        rows.stream()
            .map(
                row ->
                    new ScoreRankingPage.Entry(
                        row.getPosition(),
                        row.getPlayerId(),
                        row.getScore(),
                        row.getValuationDate(),
                        new ScoreRankingPage.Player(
                            row.getName(),
                            row.getPlayerPosition(),
                            row.getImageUrl(),
                            row.getTeamId() == null
                                ? null
                                : new ScoreRankingPage.Team(
                                    row.getTeamId(), row.getTeamName(), null),
                            row.getLeagueId() == null
                                ? null
                                : new ScoreRankingFilterOptions.League(
                                    row.getLeagueId(), row.getLeagueName()))))
            .toList();
    return new ScoreRankingPage(
        execution.getStrategyName(),
        execution.getQuoteDate(),
        entries,
        query.page(),
        query.size(),
        total,
        new ScoreRankingFilterOptions(
            leagues.stream()
                .map(league -> new ScoreRankingFilterOptions.League(league.id(), league.name()))
                .toList(),
            positions));
  }
}
