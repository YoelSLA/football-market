package footballmarket.features.ranking.presentation.dtos.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Respuesta paginada del ranking por score. */
public record ScoreRankingPageResponseDTO(
    @Schema(description = "Estrategia de la última ejecución finalizada", nullable = true)
        String strategy,
    @Schema(description = "Fecha de la última ejecución finalizada", nullable = true)
        LocalDate quoteDate,
    @Schema(description = "Entradas de la página filtrada") List<Entry> content,
    @Schema(description = "Página base cero") int page,
    @Schema(description = "Tamaño entre 1 y 100") int size,
    @Schema(description = "Total del conjunto filtrado") long totalElements,
    @Schema(description = "Cantidad de páginas del conjunto filtrado") long totalPages,
    @Schema(description = "Opciones globales sin filtros") FilterOptions filterOptions) {
  /** Posición global y score persistido. */
  public record Entry(
      @Schema(description = "Posición global previa a filtros") long position,
      @Schema(description = "Identificador del jugador") Long playerId,
      @Schema(description = "Score final persistido") BigDecimal score,
      @Schema(description = "Fecha de la valuación seleccionada") LocalDate valuationDate,
      @Schema(description = "Identidad del jugador") Player player) {}

  /** Identidad persistida del jugador. */
  public record Player(String name, String position, String imageUrl, Team team, League league) {}

  /** Equipo del jugador. */
  public record Team(Long id, String name, String crestUrl) {}

  /** Liga del jugador u opción de filtro. */
  public record League(Long id, String name) {}

  /** Opciones del universo elegible sin filtros. */
  public record FilterOptions(List<League> leagues, List<String> positions) {}
}
