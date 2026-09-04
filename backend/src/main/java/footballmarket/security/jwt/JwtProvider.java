package footballmarket.security.jwt;

import org.springframework.stereotype.Component;

@Component
public class JwtProvider {
    public String generateToken(String email) {
        // Simple implementation for MVP without a real JWT library
        return "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9." + email + ".signature";
    }
}
