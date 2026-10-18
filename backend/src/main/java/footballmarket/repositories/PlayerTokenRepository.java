package footballmarket.repositories;

import footballmarket.models.PlayerToken;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/** Acceso a las tenencias de tokens por jugador y propietario. */
@Repository
public interface PlayerTokenRepository extends JpaRepository<PlayerToken, Long> {

  /**
   * Localiza la tenencia de un propietario sobre un jugador.
   *
   * @param playerId jugador de la tenencia
   * @param ownerUserId propietario de la tenencia
   * @return la tenencia, o vacío si ese propietario nunca compró ese jugador
   */
  Optional<PlayerToken> findByPlayerIdAndOwnerId(Long playerId, Long ownerUserId);

  /**
   * Crea la tenencia en cero del comprador cuando todavía no existe.
   *
   * <p>Ignorar el conflicto serializa la creación frente a otra inserción concurrente de la misma
   * tenencia. Cuando la fila ya existe la operación se descarta y no bloquea esa fila: el bloqueo
   * efectivo de la tenencia lo aporta la consulta de bloqueo posterior.
   *
   * @param playerId jugador comprado
   * @param ownerUserId comprador
   */
  @Modifying
  @Query(
      value =
          "insert into player_tokens (player_id, owner_user_id, quantity, updated_at)"
              + " values (:playerId, :ownerUserId, 0, now())"
              + " on conflict (player_id, owner_user_id) do nothing",
      nativeQuery = true)
  void insertHoldingIfAbsent(
      @Param("playerId") Long playerId, @Param("ownerUserId") Long ownerUserId);

  /**
   * Bloquea las tenencias de los propietarios indicados sobre un jugador.
   *
   * <p>El orden ascendente por propietario es explícito y obligatorio para que las operaciones
   * concurrentes adquieran los bloqueos siempre en la misma secuencia.
   *
   * @param playerId jugador cuyas tenencias se bloquean
   * @param ownerUserIds propietarios cuyas tenencias se bloquean, en cualquier orden de entrada
   * @return tenencias bloqueadas ordenadas por propietario
   */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query(
      "select t from PlayerToken t where t.player.id = :playerId and t.owner.id in :ownerUserIds"
          + " order by t.owner.id asc")
  List<PlayerToken> lockHoldingsInAscendingOwnerOrder(
      @Param("playerId") Long playerId, @Param("ownerUserIds") List<Long> ownerUserIds);
}
