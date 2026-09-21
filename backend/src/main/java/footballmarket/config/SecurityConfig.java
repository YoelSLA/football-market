package footballmarket.config;

import footballmarket.security.PersistedUserJwtAuthenticationConverter;
import footballmarket.services.AuthenticationService;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.List;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
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
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
/** Define los componentes y reglas de seguridad de la API. */
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
   * @param authenticationService servicio de identidad persistida
   * @return cadena de filtros configurada
   * @throws Exception si Spring Security no puede construir la cadena
   */
  @Bean
  public SecurityFilterChain securityFilterChain(
      HttpSecurity http, AuthenticationService authenticationService) throws Exception {
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
                    .anyRequest()
                    .authenticated())
        .oauth2ResourceServer(
            oauth2 ->
                oauth2.jwt(
                    jwt ->
                        jwt.jwtAuthenticationConverter(
                            new PersistedUserJwtAuthenticationConverter(authenticationService))));

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
