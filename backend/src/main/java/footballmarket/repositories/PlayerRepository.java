package footballmarket.repositories;

import footballmarket.models.Player;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlayerRepository extends JpaRepository<Player, Long> {
  Page<Player> findByActiveTrue(Pageable pageable);
}
