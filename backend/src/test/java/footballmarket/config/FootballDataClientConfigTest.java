package footballmarket.config;

import static org.assertj.core.api.Assertions.*;

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
    assertThatIllegalArgumentException()
        .isThrownBy(() -> new FootballDataClientConfig().footballDataRestClient(properties));
  }

  @Test
  void requiresOrderedNonemptyCompetitionsAndDoesNotPrintKey() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> new FootballDataProperties("key", "https://provider.example", null));
    assertThatIllegalArgumentException()
        .isThrownBy(() -> new FootballDataProperties("key", "https://provider.example", List.of()));
    assertThatIllegalArgumentException()
        .isThrownBy(
            () -> new FootballDataProperties("key", "https://provider.example", List.of(" ")));
    var properties =
        new FootballDataProperties(
            "private-test-key", "https://provider.example", List.of("PD", "PL"));
    assertThat(properties.competitions()).containsExactly("PD", "PL");
    assertThat(properties.toString()).doesNotContain("private-test-key");
    assertThat(new FootballDataClientConfig().footballDataRestClient(properties)).isNotNull();
    assertThatIllegalArgumentException()
        .isThrownBy(
            () ->
                new FootballDataClientConfig()
                    .footballDataRestClient(
                        new FootballDataProperties(
                            " ", "https://provider.example", List.of("PL"))));
  }
}
