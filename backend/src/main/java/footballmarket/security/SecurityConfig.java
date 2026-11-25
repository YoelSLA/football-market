package footballmarket.security;

import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.List;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.security.oauth2.server.resource.web.DefaultBearerTokenResolver;
import org.springframework.security.oauth2.server.resource.web.access.BearerTokenAccessDeniedHandler;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/** Define los componentes y reglas de seguridad de la API. */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

  /**
   * @return codificador seguro de contraseñas
   */
  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  /**
   * Construye la clave utilizada para firmar y validar JWT.
   *
   * @param secret secreto configurado
   * @return clave HMAC
   */
  @Bean
  public SecretKey jwtSecretKey(@Value("${jwt.secret}") String secret) {
    return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
  }

  /**
   * Configura el decodificador de JWT.
   *
   * @param jwtSecretKey clave de firma
   * @return decodificador JWT
   */
  @Bean
  public JwtDecoder jwtDecoder(SecretKey jwtSecretKey) {
    return NimbusJwtDecoder.withSecretKey(jwtSecretKey).macAlgorithm(MacAlgorithm.HS256).build();
  }

  /**
   * Define autorización, CSRF y autenticación Bearer de la API.
   *
   * @param http configurador de seguridad HTTP
   * @param identityValidator validador de identidad persistida
   * @return cadena de filtros configurada
   */
  @Bean
  public SecurityFilterChain securityFilterChain(
      HttpSecurity http, PersistedIdentityValidator identityValidator) {
    BearerTokenResolver defaultBearerTokenResolver = new DefaultBearerTokenResolver();
    BearerTokenResolver bearerTokenResolver =
        request ->
            (request.getContextPath() + "/actuator/prometheus").equals(request.getRequestURI())
                ? null
                : defaultBearerTokenResolver.resolve(request);
    AccessDeniedHandler accessDeniedHandler =
        (request, response, exception) -> {
          if (HttpMethod.POST.name().equals(request.getMethod())
              && "/api/quotes/recalculate/market-demand".equals(request.getRequestURI())) {
            response.sendError(HttpStatus.UNAUTHORIZED.value());
          } else {
            new BearerTokenAccessDeniedHandler().handle(request, response, exception);
          }
        };
    http
        // API stateless: authentication is handled through JWT Bearer tokens,
        // not browser-managed cookies, so CSRF protection is not required here.
        .csrf(AbstractHttpConfigurer::disable) // NOSONAR
        .cors(Customizer.withDefaults())
        .sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(
            auth ->
                auth.requestMatchers(HttpMethod.POST, "/api/auth/register", "/api/auth/login")
                    .permitAll()
                    .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**")
                    .permitAll()
                    // Solo se permite anónimamente el acceso a los endpoints de monitoreo
                    // acordados.
                    .requestMatchers("/actuator/prometheus", "/actuator/health", "/actuator/info")
                    .permitAll()
                    // Bloquea la raíz, subendpoints y cualquier otro endpoint de Actuator.
                    .requestMatchers("/actuator", "/actuator/**")
                    .denyAll()
                    // Estadísticas WhoScored: mismas condiciones que las sincronizaciones
                    // del catálogo. El JWT protege la API propia y nunca se propaga al proveedor.
                    .requestMatchers(
                        "/api/players/statistics/whoscored/sync/scraping",
                        "/api/players/statistics/whoscored/sync/json",
                        "/api/valuations/evaluate")
                    .authenticated()
                    .requestMatchers(HttpMethod.POST, "/api/quotes/recalculate/market-demand")
                    .hasRole("SUPERUSER")
                    .anyRequest()
                    .authenticated())
        .exceptionHandling(exceptions -> exceptions.accessDeniedHandler(accessDeniedHandler))
        .oauth2ResourceServer(
            oauth2 ->
                oauth2
                    // Prometheus ignora cualquier Bearer: credenciales inválidas no deben cambiar
                    // el comportamiento de este endpoint público. El resto de la API mantiene JWT.
                    .bearerTokenResolver(bearerTokenResolver)
                    .accessDeniedHandler(accessDeniedHandler)
                    .jwt(
                        jwt ->
                            jwt.jwtAuthenticationConverter(
                                new PersistedUserJwtAuthenticationConverter(identityValidator))));

    return http.build();
  }

  /**
   * Permite que el cliente Vite local consuma la API mediante peticiones con cabecera Authorization
   * Bearer.
   *
   * @return configuración CORS de la API
   */
  @Bean
  public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration configuration = new CorsConfiguration();
    configuration.setAllowedOrigins(List.of("http://localhost:5173"));
    configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
    configuration.setAllowedHeaders(List.of("Authorization", "Content-Type"));

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/api/**", configuration);
    return source;
  }
}
