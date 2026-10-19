package footballmarket.integrations.exceptions;

public class WhoScoredStatisticsUnavailableException extends WhoScoredStatisticsException {
  public WhoScoredStatisticsUnavailableException(String code, Integer providerStatus) {
    super(code, providerStatus);
  }
}
