package footballmarket.config;

import footballmarket.integrations.exceptions.InvalidFootballDataConfigurationException;
import java.net.URI;
import java.net.http.HttpClient;
import java.time.Duration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/** Configura el cliente HTTP seguro utilizado para comunicarse con Football-Data. */
@Configuration
public class FootballDataClientConfig {

  private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(5);
  private static final Duration READ_TIMEOUT = Duration.ofSeconds(20);

  /**
   * Construye el cliente de Football-Data con timeouts y credenciales configuradas.
   *
   * @param properties configuración validada del proveedor
   * @return cliente HTTP de Football-Data
   */
  @Bean
  public RestClient footballDataRestClient(FootballDataProperties properties) {
    URI baseUrl = this.parseAndValidateBaseUrl(properties.baseUrl());
    String apiKey = this.validateApiKey(properties.apiKey());
    JdkClientHttpRequestFactory requestFactory = this.createRequestFactory();

    return RestClient.builder()
        .baseUrl(baseUrl.toString())
        .requestFactory(requestFactory)
        .defaultHeader("X-Auth-Token", apiKey)
        .build();
  }

  /**
   * Convierte y valida la URL base configurada para Football-Data.
   *
   * @param rawBaseUrl URL base configurada como texto
   * @return URL base convertida y validada
   * @throws InvalidFootballDataConfigurationException si la URL no tiene un formato o una
   *     configuración válida
   */
  private URI parseAndValidateBaseUrl(String rawBaseUrl) {
    URI baseUrl = this.parseBaseUrl(rawBaseUrl);
    this.validateBaseUrl(baseUrl);
    return baseUrl;
  }

  /**
   * Convierte la URL base configurada a una instancia de {@link URI}.
   *
   * @param rawBaseUrl URL base configurada como texto
   * @return URI correspondiente a la URL configurada
   * @throws InvalidFootballDataConfigurationException si la URL no tiene un formato válido
   */
  private URI parseBaseUrl(String rawBaseUrl) {
    try {
      return URI.create(rawBaseUrl);
    } catch (IllegalArgumentException exception) {
      throw new InvalidFootballDataConfigurationException(
          "La URL del proveedor no tiene un formato válido");
    }
  }

  /**
   * Válida las restricciones de seguridad de la URL base de Football-Data.
   *
   * <p>La URL debe utilizar HTTPS, contener un host válido y no incluir credenciales, parámetros de
   * consulta ni fragmentos.
   *
   * @param baseUrl URL base que se desea validar
   * @throws InvalidFootballDataConfigurationException si la URL no cumple las restricciones
   *     requeridas
   */
  private void validateBaseUrl(URI baseUrl) {
    boolean isValid =
        "https".equalsIgnoreCase(baseUrl.getScheme())
            && baseUrl.getHost() != null
            && baseUrl.getUserInfo() == null
            && baseUrl.getQuery() == null
            && baseUrl.getFragment() == null;

    if (!isValid) {
      throw new InvalidFootballDataConfigurationException(
          "La URL del proveedor debe ser HTTPS y no contener credenciales ni parámetros");
    }
  }

  /**
   * Válida que la clave de acceso a Football-Data esté configurada.
   *
   * @param apiKey clave de acceso configurada para el proveedor
   * @return clave de acceso validada
   * @throws InvalidFootballDataConfigurationException si la clave es nula o está vacía
   */
  private String validateApiKey(String apiKey) {
    if (apiKey == null || apiKey.isBlank()) {
      throw new InvalidFootballDataConfigurationException("Debe configurar la clave del proveedor");
    }

    return apiKey;
  }

  /**
   * Construye la fábrica de solicitudes HTTP utilizada por el cliente de Football-Data.
   *
   * <p>Configura los tiempos máximos de conexión y lectura, y deshabilita el seguimiento automático
   * de redirecciones.
   *
   * @return fábrica de solicitudes HTTP configurada
   */
  private JdkClientHttpRequestFactory createRequestFactory() {
    HttpClient httpClient =
        HttpClient.newBuilder()
            .connectTimeout(CONNECT_TIMEOUT)
            .followRedirects(HttpClient.Redirect.NEVER)
            .build();

    JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
    requestFactory.setReadTimeout(READ_TIMEOUT);

    return requestFactory;
  }
}
