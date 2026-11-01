package footballmarket.features.identity.domain.records;

/** Nombre y equipo de una misma observación externa; nunca se cruzan filas distintas. */
public record WhoScoredIdentityContext(String name, String teamName) {}
