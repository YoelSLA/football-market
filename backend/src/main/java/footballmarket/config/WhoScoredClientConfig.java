package footballmarket.config;

import java.net.http.HttpClient;
import java.time.Duration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/** Cliente dedicado: HTTPS validado, sin redirecciones ni propagación del JWT del operador. */
@Configuration
@EnableConfigurationProperties(WhoScoredProperties.class)
public class WhoScoredClientConfig {
  @Bean
  public RestClient whoScoredRestClient(WhoScoredProperties properties) {
    HttpClient client =
        HttpClient.newBuilder()
            .connectTimeout(Duration.ofMillis(properties.connectTimeoutMs()))
            .followRedirects(HttpClient.Redirect.NEVER)
            .build();
    JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(client);
    factory.setReadTimeout(Duration.ofMillis(properties.readTimeoutMs()));
    return RestClient.builder()
        .baseUrl(properties.baseUrl())
        .requestFactory(factory)
        .defaultHeader(HttpHeaders.USER_AGENT, properties.userAgent())
        .build();
  }
}
