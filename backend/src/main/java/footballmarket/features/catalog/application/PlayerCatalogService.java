package footballmarket.features.catalog.application;

import footballmarket.features.catalog.application.exceptions.PlayerNotFoundException;
import footballmarket.features.catalog.application.exceptions.PlayerSynchronizationPersistenceException;
import footballmarket.features.catalog.domain.models.player.Player;
import footballmarket.features.catalog.domain.records.PlayerCatalogCriteria;
import footballmarket.features.catalog.domain.records.PlayerCatalogFilters;
import footballmarket.features.catalog.domain.records.PlayerCatalogPage;
import footballmarket.features.catalog.domain.records.PlayerSnapshot;
import footballmarket.features.catalog.domain.records.PlayerSynchronizationResult;
import org.springframework.data.domain.Page;

/** Contrato de persistencia y consulta del catálogo local de jugadores. */
public interface PlayerCatalogService {

  /**
   * Aplica una foto completa al catálogo con un único commit.
   *
   * <p>Un conflicto de referencia identificado por la restricción única se descarta sin reasignar
   * jugadores: se revierte el intento y se reaplica la misma foto sin ese candidato, sin repetir
   * las lecturas al proveedor. Cualquier otro error revierte la ejecución completa.
   *
   * @param playerSnapshot foto validada del proveedor
   * @return resumen del intento confirmado; con todos los contadores a cero si la foto no permite
   *     resolver una identidad requerida y por tanto no se aplica
   * @throws PlayerSynchronizationPersistenceException si la aplicación local falla por una causa
   *     técnica
   */
  PlayerSynchronizationResult applySynchronization(PlayerSnapshot playerSnapshot);

  /**
   * Obtiene una página estable de jugadores activos.
   *
   * @param page número de página desde cero
   * @param size tamaño solicitado
   * @return página de jugadores activos
   */
  Page<Player> getActivePlayers(int page, int size);

  /** Página del conjunto visible con filtros globales y enriquecimiento solicitado. */
  PlayerCatalogPage queryCatalog(PlayerCatalogCriteria criteria, int page, int size);

  /** Opciones globales independientes de los filtros y la página actual. */
  PlayerCatalogFilters catalogFilters();

  /**
   * Obtiene un jugador visible para el catálogo mediante su identificador interno.
   *
   * <p>Aplica el mismo criterio de visibilidad que el listado: jugador activo con equipo vigente y
   * liga válida. Un jugador inexistente, inactivo o no visible produce la misma ausencia, sin
   * revelar su existencia.
   *
   * @param id identificador interno del jugador
   * @return jugador visible con equipo y liga cargados
   * @throws PlayerNotFoundException si no hay un jugador visible con ese identificador
   */
  Player getVisiblePlayerById(long id);
}
