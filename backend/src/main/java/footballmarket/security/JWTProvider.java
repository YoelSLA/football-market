package footballmarket.security;

import io.jsonwebtoken.Jwts;
import java.time.Instant;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Genera tokens JWT firmados para usuarios autenticados. */
@Component
public class JWTProvider {

  private final SecretKey secretKey;
  private final long expiration;

  public JWTProvider(SecretKey secretKey, @Value("${jwt.expiration}") long expiration) {

    this.secretKey = secretKey;
    this.expiration = expiration;
  }

  /**
   * Genera un token para el email autenticado.
   *
   * @param email identidad incluida como sujeto
   * @return token JWT firmado
   */
  public String generateToken(String email) {
    Instant now = Instant.now();
    Instant expirationDate = now.plusMillis(this.expiration);

    return Jwts.builder()
        .subject(email)
        .issuedAt(Date.from(now))
        .expiration(Date.from(expirationDate))
        .signWith(this.secretKey, Jwts.SIG.HS256)
        .compact();
  }
}
