package footballmarket.controllers.mappers;

import footballmarket.controllers.dtos.requests.RegisterRequestDTO;
import footballmarket.models.User;

public class UserMapper {

  private UserMapper() {}

  public static User toModel(RegisterRequestDTO request) {
    return new User(request.email(), request.password());
  }
}
