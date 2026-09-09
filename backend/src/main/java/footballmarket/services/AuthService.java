package footballmarket.services;

import footballmarket.controllers.dtos.requests.auth.LoginRequest;
import footballmarket.controllers.dtos.requests.auth.RegisterRequest;
import footballmarket.controllers.dtos.responses.auth.LoginResponse;
import footballmarket.controllers.dtos.responses.auth.RegisterResponse;

public interface AuthService {
  RegisterResponse register(RegisterRequest request);

  LoginResponse login(LoginRequest request);
}
