package footballmarket.integrations.exceptions;

public class WhoScoredStatisticsStructureChangedException extends WhoScoredStatisticsException {
  public WhoScoredStatisticsStructureChangedException() {
    super("PROVIDER_STRUCTURE_CHANGED", null);
  }
}
