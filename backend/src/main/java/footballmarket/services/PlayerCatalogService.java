package footballmarket.services;

import footballmarket.models.Player;
import footballmarket.models.PlayerCatalogFilters;
import footballmarket.models.PlayerCatalogPage;
import footballmarket.models.records.PlayerCatalogCriteria;
import footballmarket.models.records.PlayerSnapshot;
import footballmarket.models.records.PlayerSynchronizationResult;
import footballmarket.services.exceptions.PlayerNotFoundException;
import footballmarket.services.exceptions.PlayerSynchronizationPersistenceException;
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
