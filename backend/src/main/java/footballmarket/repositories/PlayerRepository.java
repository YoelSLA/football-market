package footballmarket.repositories;

import footballmarket.models.Player;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/** Consulta local del catálogo vigente con asociaciones disponibles al terminar la lectura. */
@Repository
public interface PlayerRepository extends JpaRepository<Player, Long> {
  @EntityGraph(attributePaths = {"team", "team.league"})
  @Query(
      "select p from Player p join p.team t join t.league l where p.active = true and t.current = true")
  Page<Player> findByActiveTrue(Pageable pageable);

  /**
   * Consulta individual con el mismo predicado de visibilidad del catálogo.
   *
   * <p>Replica exactamente el filtro de {@link #findByActiveTrue(Pageable)} (jugador activo con
   * equipo vigente) y carga las asociaciones necesarias para la respuesta. Un jugador existente
   * pero no visible no produce coincidencias.
   *
   * @param id identificador interno del jugador
   * @return jugador visible con su equipo y liga cargados, o vacío si no cumple la visibilidad
   */
  @EntityGraph(attributePaths = {"team", "team.league"})
  @Query(
      "select p from Player p join p.team t join t.league l"
          + " where p.id = :id and p.active = true and t.current = true")
  Optional<Player> findVisibleById(Long id);

  /**
   * Lectura por lote de los jugadores indicados, con su equipo disponible para la respuesta.
   *
   * <p>Una sola consulta para toda la página: el equipo se carga en la misma lectura, de modo que
   * la presentación de las posiciones no abre una consulta por jugador. No filtra por visibilidad:
   * un jugador inactivo permanece en el portfolio con sus datos descriptivos.
   *
   * @param playerIds identificadores de los jugadores de la página
   * @return jugadores indicados con su equipo cargado
   */
  @Query("select p from Player p join fetch p.team t where p.id in :playerIds")
  List<Player> findAllByIdInWithTeam(@Param("playerIds") List<Long> playerIds);

  Optional<Player> findFirstByIdGreaterThanOrderByIdAsc(Long id);

  /** Materializa el snapshot de las ligas seleccionadas, sin filtros de vigencia adicionales. */
  @EntityGraph(attributePaths = {"team", "team.league", "externalReferences"})
  @Query("select distinct p from Player p where p.team.league.id in :leagueIds order by p.id")
  List<Player> findIdentitySnapshot(List<Long> leagueIds);

  /** Materializa únicamente pendientes todavía existentes y sin ninguna identidad WhoScored. */
  @EntityGraph(attributePaths = {"team", "team.league", "externalReferences"})
  @Query(
      "select distinct p from Player p where p.id in :playerIds and not exists (select r.id from PlayerExternalReference r where r.player = p and r.provider = footballmarket.models.enums.ExternalProvider.WHOSCORED) order by p.id")
  List<Player> findHistoricalIdentitySnapshot(List<Long> playerIds);

  /** Bloqueo breve de la identidad local, exclusivamente durante confirmación. */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select p from Player p where p.id = :id")
  Optional<Player> lockIdentityById(Long id);
}
