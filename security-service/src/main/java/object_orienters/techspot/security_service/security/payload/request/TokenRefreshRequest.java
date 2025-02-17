package object_orienters.techspot.security_service.security.payload.request;

import jakarta.validation.constraints.NotBlank;

public class TokenRefreshRequest {

    @NotBlank
    private String refreshToken;

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }
}
