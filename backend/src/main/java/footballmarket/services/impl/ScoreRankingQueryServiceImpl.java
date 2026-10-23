package footballmarket.services.impl;

import footballmarket.models.AttributeValuation;
import footballmarket.models.PositionalValuation;
import footballmarket.models.QuoteExecution;
import footballmarket.models.enums.ValuationStrategy;
import footballmarket.models.records.ScoreRanking;
import footballmarket.models.records.ScoreRankingEntry;
import footballmarket.repositories.AttributeValuationRepository;
import footballmarket.repositories.PositionalValuationRepository;
import footballmarket.repositories.QuoteExecutionRepository;
import footballmarket.services.ScoreRankingQueryService;
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
    QuoteExecution execution = this.executions.findTopByOrderByQuoteDateDesc().orElse(null);
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
}
