package footballmarket.repositories;

import footballmarket.models.Player;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PlayerRepository extends JpaRepository<Player, Long> {
  Page<Player> findByActiveTrue(Pageable pageable);

  List<Player> findByActiveTrue();
}
