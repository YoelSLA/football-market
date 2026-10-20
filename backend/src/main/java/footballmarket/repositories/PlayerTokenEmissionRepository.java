package footballmarket.repositories;

import footballmarket.models.PlayerTokenEmission;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/** Acceso a las emisiones iniciales de tokens por jugador. */
@Repository
public interface PlayerTokenEmissionRepository extends JpaRepository<PlayerTokenEmission, Long> {

  /**
   * Carga las emisiones de un propietario.
   *
   * <p>La asociación con el propietario es {@code EAGER} por el mapeo de JPA y se recupera en la
   * misma sentencia, para no pagar una consulta por emisión; el plegado solo necesita su
   * identificador.
   *
   * @param userId propietario cuyas emisiones se recuperan
   * @return emisiones del propietario
   */
  @Query("select e from PlayerTokenEmission e join fetch e.owner where e.owner.id = :userId")
  List<PlayerTokenEmission> findFinancialEmissions(@Param("userId") Long userId);

  /** Detecta inventario sin emisión o una emisión sin tenencia de su propietario inicial. */
  @Query(
      value =
          """
          SELECT EXISTS (
              SELECT 1 FROM player_tokens t
              WHERE NOT EXISTS (
                  SELECT 1 FROM player_token_emissions e WHERE e.player_id = t.player_id
              )
          ) OR EXISTS (
              SELECT 1 FROM player_token_emissions e
              WHERE NOT EXISTS (
                  SELECT 1 FROM player_tokens t
                  WHERE t.player_id = e.player_id AND t.owner_user_id = e.owner_user_id
              )
          )
          """,
      nativeQuery = true)
  boolean hasInconsistentInventory();

  /**
   * Emite únicamente jugadores sin emisión y crea sus tenencias dentro de la misma sentencia.
   *
   * <p>Incluye inactivos. RETURNING impide crear tenencias para emisiones históricas. Un conflicto
   * de tenencia no se ignora: debe revertir el aprovisionamiento, no reparar inventario existente.
   * Requiere la transacción del llamador y ausencia de sincronizaciones concurrentes del catálogo.
   *
   * @param superuserId propietario inicial ya persistido
   * @param initialQuantity cantidad inicial definida por el modelo
   * @return número de tenencias iniciales insertadas
   */
  @Modifying(flushAutomatically = true)
  @Query(
      value =
          """
          WITH created_emissions AS (
              INSERT INTO player_token_emissions (player_id, owner_user_id, initial_quantity)
              SELECT p.id, :superuserId, :initialQuantity
              FROM players p
              WHERE NOT EXISTS (
                  SELECT 1 FROM player_token_emissions e WHERE e.player_id = p.id
              )
              ORDER BY p.id
              ON CONFLICT (player_id) DO NOTHING
              RETURNING player_id, owner_user_id, initial_quantity
          )
          INSERT INTO player_tokens (player_id, owner_user_id, quantity)
          SELECT player_id, owner_user_id, initial_quantity FROM created_emissions
          """,
      nativeQuery = true)
  int recoverMissingEmissions(
      @Param("superuserId") Long superuserId, @Param("initialQuantity") int initialQuantity);
}
