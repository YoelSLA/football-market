package footballmarket.repositories;

import footballmarket.models.CatalogTransition;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

/** Lectura bloqueante del marcador dentro de la aplicación transaccional de la foto. */
@Repository
public interface CatalogTransitionRepository extends JpaRepository<CatalogTransition, Integer> {
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select t from CatalogTransition t where t.id = 1")
  Optional<CatalogTransition> findForApplication();
}
