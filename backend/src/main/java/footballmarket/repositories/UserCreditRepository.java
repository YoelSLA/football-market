package footballmarket.repositories;

import footballmarket.models.UserCredit;
import jakarta.persistence.LockModeType;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/** Acceso a los saldos de crédito del mercado. */
@Repository
public interface UserCreditRepository extends JpaRepository<UserCredit, Long> {

  /**
   * Bloquea las cuentas de crédito de los usuarios indicados.
   *
   * <p>El orden ascendente por {@code user_id} es explícito y obligatorio: hace que dos operaciones
   * concurrentes adquieran siempre los bloqueos en el mismo orden y no se produzcan interbloqueos
   * entre ellas.
   *
   * @param userIds identificadores de las cuentas a bloquear, en cualquier orden de entrada
   * @return cuentas bloqueadas ordenadas por identificador de usuario
   */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select c from UserCredit c where c.userId in :userIds order by c.userId asc")
  List<UserCredit> lockAccountsInAscendingOrder(@Param("userIds") List<Long> userIds);
}
