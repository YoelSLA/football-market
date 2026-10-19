package footballmarket.integrations.exceptions;

public class WhoScoredStatisticsRateLimitException extends WhoScoredStatisticsException {
  public WhoScoredStatisticsRateLimitException(Integer providerStatus) {
    super("PROVIDER_TRANSPORT_ERROR", providerStatus);
  }
}
