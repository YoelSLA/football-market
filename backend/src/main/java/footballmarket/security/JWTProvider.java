package footballmarket.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class JWTProvider {

  private final SecretKey secretKey;
  private final long expiration;

  public JWTProvider(
      @Value("${jwt.secret}") String secret, @Value("${jwt.expiration}") long expiration) {

    this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    this.expiration = expiration;
  }

  public String generateToken(String email) {
    Instant now = Instant.now();
    Instant expirationDate = now.plusMillis(expiration);

    return Jwts.builder()
        .subject(email)
        .issuedAt(Date.from(now))
        .expiration(Date.from(expirationDate))
        .signWith(secretKey)
        .compact();
  }
}
