package footballmarket.features.identity.domain.models;

import footballmarket.features.catalog.domain.enums.ExternalProvider;
import footballmarket.features.catalog.domain.exceptions.InvalidPlayerException;
import footballmarket.features.catalog.domain.models.player.PlayerExternalReference;
import footballmarket.features.identity.domain.records.WhoScoredIdentityContext;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import lombok.Getter;

/** Identidad candidata transitoria con pares de nombre y equipo observados juntos. */
public final class WhoScoredIdentityCandidate {
  @Getter private final String externalId;
  private final Set<WhoScoredIdentityContext> contexts = new LinkedHashSet<>();

  public WhoScoredIdentityCandidate(String externalId) {
    this.externalId =
        PlayerExternalReference.canonicalExternalId(ExternalProvider.WHOSCORED, externalId);
  }

  public List<WhoScoredIdentityContext> getContexts() {
    return List.copyOf(this.contexts);
  }

  public void observe(WhoScoredIdentityContext context) {
    if (context == null
        || !WhoScoredIdentityNormalizer.validName(context.name())
        || !WhoScoredIdentityNormalizer.validName(context.teamName())) {
      throw new InvalidPlayerException("El candidato requiere nombre y equipo válidos");
    }
    this.contexts.add(context);
  }
}
