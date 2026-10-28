package footballmarket.features.auth.infraestructure;

import footballmarket.features.auth.domain.User;
import footballmarket.features.auth.domain.enums.UserRole;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

  Optional<User> findByEmail(String email);

  boolean existsByEmail(String email);

  boolean existsByUsername(String username);

  Optional<User> findByRole(UserRole role);
}
