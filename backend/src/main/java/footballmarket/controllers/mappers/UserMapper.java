package footballmarket.controllers.mappers;

import footballmarket.controllers.dtos.requests.auth.RegisterRequest;
import footballmarket.controllers.dtos.responses.auth.RegisterResponse;
import footballmarket.models.User;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    public User toEntity(RegisterRequest request) {
        return User.builder()
                .email(request.getEmail().toLowerCase())
                .password(request.getPassword())
                .build();
    }

    public RegisterResponse toResponse(User user) {
        return RegisterResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .build();
    }
}
