package footballmarket.services.impl;

import footballmarket.controllers.dtos.requests.auth.LoginRequest;
import footballmarket.controllers.dtos.requests.auth.RegisterRequest;
import footballmarket.controllers.dtos.responses.auth.LoginResponse;
import footballmarket.controllers.dtos.responses.auth.RegisterResponse;
import footballmarket.models.User;
import footballmarket.repositories.UserRepository;
import footballmarket.security.jwt.JwtProvider;
import footballmarket.services.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;

    @Override
    public RegisterResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already exists");
        }

        User user = User.builder()
                .email(request.getEmail().toLowerCase())
                .password(passwordEncoder.encode(request.getPassword()))
                .build();

        user = userRepository.save(user);

        return RegisterResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .build();
    }

    @Override
    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail().toLowerCase())
                .orElseThrow(() -> new RuntimeException("Invalid credentials"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("Invalid credentials");
        }

        String token = jwtProvider.generateToken(user.getEmail());

        return LoginResponse.builder()
                .token(token)
                .build();
    }
}
