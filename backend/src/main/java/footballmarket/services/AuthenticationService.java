package footballmarket.services;

import footballmarket.models.User;

public interface AuthenticationService {

  void register(User user);

  String login(String email, String password);
}
