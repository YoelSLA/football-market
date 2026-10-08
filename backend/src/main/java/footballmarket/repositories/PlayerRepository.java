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

  Optional<Player> findFirstByIdGreaterThanOrderByIdAsc(Long id);
}
