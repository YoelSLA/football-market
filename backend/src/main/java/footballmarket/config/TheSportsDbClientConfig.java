package footballmarket.config;

import java.net.http.HttpClient;
import java.time.Duration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/** Configuración del cliente v1; la clave solo se usa al construir la ruta HTTPS requerida. */
@Configuration
@EnableConfigurationProperties(TheSportsDbProperties.class)
public class TheSportsDbClientConfig {

  @Bean
  public RestClient theSportsDbRestClient(TheSportsDbProperties properties) {
    HttpClient client =
            HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(5))
                    .followRedirects(HttpClient.Redirect.NEVER)
                    .build();

    JdkClientHttpRequestFactory factory =
            new JdkClientHttpRequestFactory(client);

    factory.setReadTimeout(Duration.ofSeconds(15));

    return RestClient.builder()
            .requestFactory(factory)
            .baseUrl(
                    properties.baseUrl()
                            + "/api/v1/json/"
                            + properties.apiKey())
            .build();
  }
}