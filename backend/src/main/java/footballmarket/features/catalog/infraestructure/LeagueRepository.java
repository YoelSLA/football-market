package footballmarket.features.catalog.infraestructure;

import footballmarket.features.catalog.domain.models.league.League;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/** Persistencia de ligas con identidad interna. */
@Repository
public interface LeagueRepository extends JpaRepository<League, Long> {}
