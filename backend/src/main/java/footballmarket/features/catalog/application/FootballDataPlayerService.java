package footballmarket.features.catalog.application;

import footballmarket.features.catalog.domain.records.PlayerSnapshot;

/** Contrato de obtención y consolidación de jugadores desde el proveedor externo. */
public interface FootballDataPlayerService {

  /**
   * @return foto consolidada de las competiciones configuradas
   */
  PlayerSnapshot fetchSnapshot();
}
