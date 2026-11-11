package footballmarket.features.quote.presentation.dtos.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/** Página HTTP del ranking por cotización y sus opciones globales. */
public record QuoteRankingPageResponseDTO(
    @Schema(description = "Entradas de la página en orden global") List<Entry> content,
    @Schema(description = "Página base cero") int page,
    @Schema(description = "Tamaño solicitado, entre 1 y 100") int size,
    @Schema(description = "Total del conjunto filtrado") long totalElements,
    @Schema(description = "Cantidad de páginas del conjunto filtrado; cero si está vacío")
        long totalPages,
    @Schema(description = "Opciones globales sin filtros") FilterOptions filterOptions) {
  /** Posición oficial previa a filtros y cotización vigente con su identificación local. */
  public record Entry(
      @Schema(description = "Posición global única; no se renumera al filtrar") long position,
      @Schema(description = "Identificador local del jugador") Long playerId,
      @Schema(description = "Precio vigente en centésimas de crédito") long price,
      @Schema(description = "Día de vigencia de la cotización") LocalDate effectiveDate,
      @Schema(
              description = "Origen de la fila",
              allowableValues = {"INITIAL", "RECOMPUTED", "CARRIED_FORWARD"})
          String origin,
      @Schema(description = "Instante original del cálculo, no el de la lectura")
          Instant calculatedAt,
      @Schema(description = "Identificación del jugador") PlayerIdentity player) {}

  /** Datos identificatorios; las asociaciones y las imágenes ausentes se conservan nulas. */
  public record PlayerIdentity(
      String name, String position, String imageUrl, Team team, League league) {}

  /** Equipo local; el escudo no está disponible en el catálogo actual. */
  public record Team(Long id, String name, String crestUrl) {}

  /** Liga local identificable. */
  public record League(Long id, String name) {}

  /** Opciones del universo elegible, independientes del filtro y de la página. */
  public record FilterOptions(List<League> leagues, List<String> positions) {}
}
