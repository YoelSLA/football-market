package footballmarket.repositories;

import footballmarket.models.Player;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PlayerRepository extends JpaRepository<Player, Long> {
  Page<Player> findByActiveTrue(Pageable pageable);

  Optional<Player> findFirstByIdGreaterThanOrderByIdAsc(Long id);
}
