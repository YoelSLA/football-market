package footballmarket.repositories;

import footballmarket.models.Player;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
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

  Optional<Player> findFirstByIdGreaterThanOrderByIdAsc(Long id);
}
