package footballmarket.config;

import java.net.URI;
import java.net.http.HttpClient;
import java.time.Duration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class FootballDataClientConfig {
  @Bean
  public RestClient footballDataRestClient(FootballDataProperties properties) {
    URI baseUrl = URI.create(properties.baseUrl());
    if (!"https".equalsIgnoreCase(baseUrl.getScheme())
        || baseUrl.getHost() == null
        || baseUrl.getUserInfo() != null
        || baseUrl.getQuery() != null
        || baseUrl.getFragment() != null) {
      throw new IllegalArgumentException(
          "La URL del proveedor debe ser HTTPS y no contener credenciales ni parámetros");
    }
    if (properties.apiKey() == null || properties.apiKey().isBlank()) {
      throw new IllegalArgumentException("Debe configurar la clave del proveedor");
    }
    var client =
        HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .followRedirects(HttpClient.Redirect.NEVER)
            .build();
    var factory = new JdkClientHttpRequestFactory(client);
    factory.setReadTimeout(Duration.ofSeconds(20));
    return RestClient.builder()
        .baseUrl(baseUrl.toString())
        .requestFactory(factory)
        .defaultHeader("X-Auth-Token", properties.apiKey())
        .build();
  }
}
