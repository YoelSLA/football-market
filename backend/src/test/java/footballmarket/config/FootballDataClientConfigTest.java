package footballmarket.config;

import static org.assertj.core.api.Assertions.*;

import footballmarket.integrations.exceptions.InvalidFootballDataConfigurationException;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
class FootballDataClientConfigTest {
  @ParameterizedTest
  @ValueSource(
      strings = {
        "http://provider.example",
        "https://user:password@provider.example",
        "https://provider.example?token=secret",
        "https://provider.example#fragment"
      })
  void rejectsUnsafeProviderUrls(String url) {
    var properties = new FootballDataProperties("test-only", url, List.of("PL"));
    assertThatThrownBy(() -> new FootballDataClientConfig().footballDataRestClient(properties))
        .isInstanceOf(InvalidFootballDataConfigurationException.class);
  }

  @Test
  void requiresOrderedNonemptyCompetitionsAndDoesNotPrintKey() {
    assertThatThrownBy(() -> new FootballDataProperties("key", "https://provider.example", null))
        .isInstanceOf(InvalidFootballDataConfigurationException.class);
    assertThatThrownBy(
            () -> new FootballDataProperties("key", "https://provider.example", List.of()))
        .isInstanceOf(InvalidFootballDataConfigurationException.class);
    assertThatThrownBy(
            () -> new FootballDataProperties("key", "https://provider.example", List.of(" ")))
        .isInstanceOf(InvalidFootballDataConfigurationException.class);
    var properties =
        new FootballDataProperties(
            "private-test-key", "https://provider.example", List.of("PD", "PL"));
    assertThat(properties.competitions()).containsExactly("PD", "PL");
    assertThat(properties.toString()).doesNotContain("private-test-key");
    assertThat(new FootballDataClientConfig().footballDataRestClient(properties)).isNotNull();
    assertThatThrownBy(
            () ->
                new FootballDataClientConfig()
                    .footballDataRestClient(
                        new FootballDataProperties(" ", "https://provider.example", List.of("PL"))))
        .isInstanceOf(InvalidFootballDataConfigurationException.class);
  }
}
