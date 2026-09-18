package footballmarket.services;

import footballmarket.models.User;
import java.util.List;

public interface UserService {

  List<User> getAllUsers();

  void deteleAllUsers();
}
