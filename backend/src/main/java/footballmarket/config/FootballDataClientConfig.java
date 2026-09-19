package footballmarket.config;

import footballmarket.integrations.exceptions.InvalidFootballDataConfigurationException;
import java.net.URI;
import java.net.http.HttpClient;
import java.time.Duration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
/** Configura el cliente HTTP seguro utilizado para comunicarse con Football-Data. */
public class FootballDataClientConfig {

  /**
   * Construye el cliente de Football-Data con timeouts y credenciales configuradas.
   *
   * @param properties configuración validada del proveedor
   * @return cliente HTTP de Football-Data
   */
  @Bean
  public RestClient footballDataRestClient(FootballDataProperties properties) {
    URI baseUrl;
    try {
      baseUrl = URI.create(properties.baseUrl());
    } catch (IllegalArgumentException ex) {
      throw new InvalidFootballDataConfigurationException(
          "La URL del proveedor no tiene un formato válido");
    }

    if (!"https".equalsIgnoreCase(baseUrl.getScheme())
        || baseUrl.getHost() == null
        || baseUrl.getUserInfo() != null
        || baseUrl.getQuery() != null
        || baseUrl.getFragment() != null) {
      throw new InvalidFootballDataConfigurationException(
          "La URL del proveedor debe ser HTTPS y no contener credenciales ni parámetros");
    }

    if (properties.apiKey() == null || properties.apiKey().isBlank()) {
      throw new InvalidFootballDataConfigurationException("Debe configurar la clave del proveedor");
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
