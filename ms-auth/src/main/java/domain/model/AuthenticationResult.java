package domain.model;

import lombok.Builder;

@Builder
public record AuthenticationResult(
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresIn,
        long refreshExpiresIn
) {
}
