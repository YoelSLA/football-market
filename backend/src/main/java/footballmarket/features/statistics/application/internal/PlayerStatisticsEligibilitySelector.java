package footballmarket.features.statistics.application.internal;

import footballmarket.features.catalog.domain.enums.ExternalProvider;
import footballmarket.features.catalog.infraestructure.PlayerExternalReferenceRepository;
import footballmarket.features.catalog.infraestructure.PlayerRepository;
import footballmarket.features.statistics.domain.enums.MetricSource;
import footballmarket.features.statistics.infraestructure.PlayerStatisticsRunItemRepository;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

/**
 * Población elegible del recorrido y sus pendientes por orden ascendente de ID local.
 *
 * <p>Solo se recorren jugadores con referencia WhoScored confirmada; el resto se contabiliza como
 * exclusión explícita y nunca se crea ni modifica identidad alguna.
 */
@Component
public class PlayerStatisticsEligibilitySelector {
  private final PlayerRepository players;
  private final PlayerExternalReferenceRepository references;
  private final PlayerStatisticsRunItemRepository items;

  public PlayerStatisticsEligibilitySelector(
      PlayerRepository players,
      PlayerExternalReferenceRepository references,
      PlayerStatisticsRunItemRepository items) {
    this.players = players;
    this.references = references;
    this.items = items;
  }

  /** Tamaño del catálogo vigente y población con referencia confirmada. */
  public Population population() {
    long catalog = this.players.count();
    long eligible = this.references.countEligiblePlayers(ExternalProvider.WHOSCORED);
    return new Population(catalog, eligible, catalog - eligible);
  }

  /** Pendientes de la campaña por ID local ascendente, sin repetir confirmaciones. */
  public List<Long> pendingPlayerIds(LocalDate extractionDate, MetricSource source, int limit) {
    if (limit < 1) {
      return List.of();
    }
    return this.items.findPendingPlayerIds(extractionDate, source, PageRequest.of(0, limit));
  }

  /** Instantánea completa de pendientes del día, en ID local ascendente, para el canal JSON. */
  public List<Long> allPendingPlayerIds(LocalDate extractionDate, MetricSource source) {
    return this.items.findPendingPlayerIds(
        extractionDate, source, org.springframework.data.domain.Pageable.unpaged());
  }

  /** Conteo de población del catálogo, sin acceso externo. */
  public record Population(
      long catalogPlayers, long eligiblePlayers, long excludedWithoutReference) {
    public Population {
      if (catalogPlayers < 0 || eligiblePlayers < 0 || excludedWithoutReference < 0) {
        throw new footballmarket.features.statistics.application.exceptions
            .PlayerStatisticsSynchronizationException();
      }
    }
  }
}
