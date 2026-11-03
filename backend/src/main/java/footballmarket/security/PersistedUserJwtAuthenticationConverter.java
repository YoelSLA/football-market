package footballmarket.security;

import lombok.RequiredArgsConstructor;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;

/** Valida la identidad persistida antes de aceptar una autenticación JWT. */
@RequiredArgsConstructor
public final class PersistedUserJwtAuthenticationConverter
    implements Converter<Jwt, AbstractAuthenticationToken> {

  private final PersistedIdentityValidator identityValidator;
  private final JwtAuthenticationConverter delegate = new JwtAuthenticationConverter();

  /**
   * Comprueba que el sujeto corresponde a un usuario persistido actual y conserva las autoridades
   * del JWT.
   *
   * @param jwt token previamente validado por el Resource Server
   * @return autenticación del usuario existente
   * @throws InvalidBearerTokenException si la identidad de la sesión no es válida o no existe
   */
  @Override
  public AbstractAuthenticationToken convert(Jwt jwt) {
    String subject = jwt.getSubject();
    if (subject == null || subject.isBlank() || !this.identityValidator.exists(subject)) {
      throw new InvalidBearerTokenException("La identidad de la sesión no es válida.");
    }
    return this.delegate.convert(jwt);
  }
}
