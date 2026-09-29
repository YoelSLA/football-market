package footballmarket.services;

import footballmarket.models.records.PlayerSnapshot;

/** Contrato de obtención y consolidación de jugadores desde el proveedor externo. */
public interface FootballDataPlayerService {

  /**
   * @return foto consolidada de las competiciones configuradas
   */
  PlayerSnapshot fetchSnapshot();
}
