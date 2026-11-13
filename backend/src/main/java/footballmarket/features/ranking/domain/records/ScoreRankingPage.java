package footballmarket.features.ranking.domain.records;

import footballmarket.features.valuation.domain.enums.ValuationStrategy;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Página de posiciones globales con encabezado y opciones del universo elegible. */
public record ScoreRankingPage(
    ValuationStrategy strategy,
    LocalDate quoteDate,
    List<Entry> content,
    int page,
    int size,
    long totalElements,
    ScoreRankingFilterOptions filterOptions) {
  public long totalPages() {
    return this.totalElements / this.size + (this.totalElements % this.size == 0 ? 0 : 1);
  }

  /** Entrada con score persistido e identidad local. */
  public record Entry(
      long position, Long playerId, BigDecimal score, LocalDate valuationDate, Player player) {}

  /** Identidad del jugador y asociaciones disponibles. */
  public record Player(
      String name,
      String position,
      String imageUrl,
      Team team,
      ScoreRankingFilterOptions.League league) {}

  /** Equipo del jugador. */
  public record Team(Long id, String name, String crestUrl) {}
}
