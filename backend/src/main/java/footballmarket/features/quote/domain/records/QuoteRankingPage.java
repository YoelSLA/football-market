package footballmarket.features.quote.domain.records;

import java.util.List;

/** Página de posiciones globales y opciones del universo elegible sin filtros. */
public record QuoteRankingPage(
    List<PlayerRankingEntry> content,
    int page,
    int size,
    long totalElements,
    List<LeagueOption> leagues,
    List<String> positions) {
  public long totalPages() {
    return this.totalElements / this.size + (this.totalElements % this.size == 0 ? 0 : 1);
  }

  /** Liga representada en el universo cotizable. */
  public record LeagueOption(Long id, String name) {}

  /** Identificación local del jugador, sin sustitutos inventados para datos ausentes. */
  public record PlayerIdentity(
      String name, String position, String imageUrl, TeamIdentity team, LeagueOption league) {}

  /** Equipo local; el catálogo actual no persiste escudo. */
  public record TeamIdentity(Long id, String name, String crestUrl) {}
}
