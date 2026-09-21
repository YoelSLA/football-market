package footballmarket.security;

import footballmarket.services.AuthenticationService;
import footballmarket.services.exceptions.CurrentUserNotFoundException;
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

  private final AuthenticationService authenticationService;
  private final JwtAuthenticationConverter delegate = new JwtAuthenticationConverter();

  /**
   * Comprueba que el sujeto corresponde a un usuario actual y conserva las autoridades del JWT.
   *
   * @param jwt token previamente validado por el Resource Server
   * @return autenticación del usuario existente
   * @throws InvalidBearerTokenException si la identidad de la sesión no es válida
   */
  @Override
  public AbstractAuthenticationToken convert(Jwt jwt) {
    try {
      this.authenticationService.getCurrentUser(jwt.getSubject());
    } catch (CurrentUserNotFoundException exception) {
      throw new InvalidBearerTokenException("La identidad de la sesión no es válida.", exception);
    }
    return this.delegate.convert(jwt);
  }
}
