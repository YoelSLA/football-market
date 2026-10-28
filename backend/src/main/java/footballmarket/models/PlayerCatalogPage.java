package footballmarket.models;

import java.util.List;

/** Resultado de catálogo con enriquecimiento explícito y no persistido. */
public record PlayerCatalogPage(
    List<Entry> content, int page, int size, long totalElements, int totalPages) {
  public PlayerCatalogPage {
    content = List.copyOf(content);
  }

  /** El resumen null significa que no fue solicitado; los valores ausentes viven dentro de él. */
  public record Entry(Player player, PlayerCatalogStatistics statistics) {}
}
