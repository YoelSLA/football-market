package footballmarket.services;

import footballmarket.models.Player;
import footballmarket.models.records.PlayerSnapshot;
import footballmarket.models.records.PlayerSynchronizationResult;
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
   * @return resumen del intento confirmado
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
}
