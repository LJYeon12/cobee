package org.example.memberservice.dto.token;

import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class TokenRefreshResponse {
    private String grantType;
    private String accessToken;
    private String refreshToken;
    private Long refreshTokenExpirationTime;

    public static TokenRefreshResponse of(String accessToken, String refreshToken, Long expirationTime) {
        return TokenRefreshResponse.builder()
                .grantType("Bearer")
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .refreshTokenExpirationTime(expirationTime)
                .build();
    }
}
