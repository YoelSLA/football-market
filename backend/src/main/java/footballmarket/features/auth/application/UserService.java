package footballmarket.features.auth.application;

import footballmarket.features.auth.domain.User;
import java.util.List;

/** Contrato de operaciones administrativas sobre usuarios. */
public interface UserService {

  /**
   * @return todos los usuarios persistidos
   */
  List<User> getAllUsers();

  /** Elimina todos los usuarios persistidos. */
  void deteleAllUsers();
}
