package footballmarket.services;

import footballmarket.models.Player;
import footballmarket.models.records.PlayerSnapshot;
import footballmarket.models.records.PlayerSynchronizationResult;
import org.springframework.data.domain.Page;

/** Contrato de persistencia y consulta del catálogo local de jugadores. */
public interface PlayerCatalogService {

  /**
   * Aplica una foto completa al catálogo de forma atómica.
   *
   * @param playerSnapshot foto validada del proveedor
   * @return resumen de cambios aplicados
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
